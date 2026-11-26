package com.example.demo.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import com.example.demo.payment.PaymentAttempt;
import com.example.demo.payment.PaymentAttemptRepository;
import com.example.demo.payment.PaymentEvent;
import com.example.demo.payment.PaymentEvents;
import com.example.demo.payment.PaymentRepository;
import com.example.demo.payment.PaymentResult;
import com.example.demo.payment.PaymentResultProcessor;
import com.example.demo.payment.momo.MomoClient;
import com.example.demo.support.AbstractIntegrationTest;
import com.example.demo.support.TestData;
import com.jayway.jsonpath.JsonPath;

@TestPropertySource(properties = "vestra.kafka.enabled=true")
class KafkaOutboxTest extends AbstractIntegrationTest {

    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:3.9.1"));

    static {
        KAFKA.start();
    }

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Autowired
    WebApplicationContext context;
    @Autowired
    TestData data;
    @Autowired
    PaymentRepository paymentRepository;
    @Autowired
    PaymentAttemptRepository attemptRepository;
    @Autowired
    PaymentResultProcessor processor;
    @Autowired
    OutboxEventRepository outbox;
    @Autowired
    KafkaOutboxRelay relay;
    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @MockitoBean
    MomoClient momoClient;
    @MockitoSpyBean
    NotificationSender notificationSender;

    @Test
    void paymentSucceededIsPublishedOnceAndRedeliveryIsIgnoredByTheConsumer() throws Exception {
        Long orderId = paidMomoOrder(4000000001L);

        List<OutboxEvent> events = outbox.findByAggregateTypeAndAggregateIdOrderByIdAsc(PaymentEvents.AGGREGATE,
                String.valueOf(orderId));
        assertThat(events).extracting(OutboxEvent::getEventType).containsExactly(PaymentEvents.PAYMENT_SUCCEEDED);
        assertThat(events.get(0).getPublishedAt()).isNull();

        // Other test classes share this database and leave unpublished rows behind; drain them all (bounded).
        int published = 0;
        for (int i = 0; i < 100 && outbox.countByPublishedAtIsNull() > 0; i++) {
            published += relay.publishBatch();
        }
        assertThat(outbox.countByPublishedAtIsNull()).isZero();
        assertThat(published).isGreaterThanOrEqualTo(1);
        OutboxEvent sent = outbox.findByEventId(events.get(0).getEventId()).orElseThrow();
        assertThat(sent.getPublishedAt()).isNotNull();

        verify(notificationSender, timeout(30_000).times(1)).paymentReceived(String.valueOf(orderId), 120000L);

        // Simulate at-least-once redelivery: the very same event again.
        ProducerRecord<String, String> duplicate = new ProducerRecord<>("vestra.payments", sent.getAggregateId(),
                sent.getPayload());
        duplicate.headers().add(KafkaOutboxRelay.EVENT_ID_HEADER,
                sent.getEventId().toString().getBytes(StandardCharsets.UTF_8));
        duplicate.headers().add(KafkaOutboxRelay.EVENT_TYPE_HEADER,
                sent.getEventType().getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(duplicate).get();

        verify(notificationSender, after(5_000).times(1)).paymentReceived(String.valueOf(orderId), 120000L);
    }

    private Long paidMomoOrder(long transId) throws Exception {
        when(momoClient.createPayment(anyString(), anyString(), anyLong(), anyString(), anyString()))
                .thenAnswer(inv -> new MomoClient.CreateResponse("MOMO", inv.getArgument(1), inv.getArgument(0),
                        inv.getArgument(2), 1L, "ok", 0, "https://pay/" + inv.getArgument(0), null, null));
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        TestData.Customer buyer = data.customer();
        data.putInCart(buyer, data.variant(new BigDecimal("40000"), 10), 3);
        String body = mvc.perform(post("/api/orders")
                        .header("Authorization", buyer.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shippingRecipientName":"A","shippingPhone":"0900000000",
                                 "shippingAddressLine":"1 B","shippingCity":"Đà Nẵng","paymentMethod":"MOMO"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long orderId = ((Number) JsonPath.read(body, "$.id")).longValue();
        mvc.perform(post("/api/orders/" + orderId + "/payments/momo").header("Authorization", buyer.bearer()))
                .andExpect(status().isOk());
        PaymentAttempt attempt = attemptRepository.findFirstByPaymentIdOrderByCreatedAtDescIdDesc(
                paymentRepository.findByOrderId(orderId).orElseThrow().getId()).orElseThrow();

        processor.apply(new PaymentResult(PaymentEvent.Source.IPN, attempt.getProviderOrderId(), 120000L, transId, 0,
                "{}"));
        return orderId;
    }
}
