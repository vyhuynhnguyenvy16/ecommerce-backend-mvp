package com.example.demo.payment.momo;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.example.demo.payment.PaymentProperties;

@Configuration
@EnableConfigurationProperties({ MomoProperties.class, PaymentProperties.class })
public class MomoConfig {

    @Bean
    MomoSigner momoSigner(MomoProperties properties) {
        return new MomoSigner(properties.accessKey(), properties.secretKey());
    }

    @Bean
    MomoClient momoClient(MomoProperties properties, MomoSigner signer) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        // MoMo docs: "Minimum timeout when call this API must be 30s".
        requestFactory.setReadTimeout(Duration.ofSeconds(35));
        return new MomoClient(RestClient.builder().requestFactory(requestFactory), properties, signer);
    }

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
