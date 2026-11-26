package com.example.demo.cart;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example.demo.entity.Category;
import com.example.demo.entity.Product;
import com.example.demo.entity.ProductVariant;
import com.example.demo.entity.User;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ProductVariantRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.JwtService;
import com.example.demo.support.AbstractIntegrationTest;

class CartReadTest extends AbstractIntegrationTest {

    @Autowired
    WebApplicationContext context;
    @Autowired
    UserRepository users;
    @Autowired
    CategoryRepository categories;
    @Autowired
    ProductRepository products;
    @Autowired
    ProductVariantRepository variants;
    @Autowired
    JwtService jwt;

    @Test
    void customerCanReadACartThatHasItems() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        String u = UUID.randomUUID().toString().substring(0, 8);
        User user = users.save(User.builder().email("cart-" + u + "@example.com").passwordHash("x").fullName("C").build());
        Category c = categories.save(Category.builder().name("C" + u).slug("c-" + u).build());
        Product p = products.save(Product.builder().category(c).name("P" + u).slug("p-" + u)
                .basePrice(new BigDecimal("100000")).build());
        ProductVariant v = variants.save(ProductVariant.builder().product(p).sku("S-" + u).size("M").color("Den")
                .stockQuantity(5).build());
        String bearer = "Bearer " + jwt.generateAccessToken(user.getId(), "CUSTOMER");

        mvc.perform(post("/api/cart").header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"variantId\":" + v.getId() + ",\"quantity\":2}"))
                .andExpect(status().is2xxSuccessful());

        mvc.perform(get("/api/cart").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));
    }
}
