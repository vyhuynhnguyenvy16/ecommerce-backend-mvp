package com.example.demo.security;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example.demo.service.JwtService;
import com.example.demo.support.AbstractIntegrationTest;

/** Admin APIs are annotated with @PreAuthorize("hasRole('ADMIN')"); this proves the annotation is enforced. */
class AdminAuthorizationTest extends AbstractIntegrationTest {

    @Autowired
    WebApplicationContext context;
    @Autowired
    JwtService jwtService;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void customerCannotListAllOrders() throws Exception {
        mvc.perform(get("/api/admin/orders")
                        .header("Authorization", "Bearer " + jwtService.generateAccessToken(999_001L, "CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListAllOrders() throws Exception {
        mvc.perform(get("/api/admin/orders")
                        .header("Authorization", "Bearer " + jwtService.generateAccessToken(999_002L, "ADMIN")))
                .andExpect(status().isOk());
    }
}
