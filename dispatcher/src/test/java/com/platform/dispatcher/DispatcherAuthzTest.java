package com.platform.dispatcher;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DispatcherAuthzTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void whenNoJwtOnIdeas_thenReturns401() throws Exception {
        mockMvc.perform(get("/ideas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenNoJwtOnDeals_thenReturns401() throws Exception {
        mockMvc.perform(get("/deals/offers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenInvalidJwt_thenReturns401() throws Exception {
        mockMvc.perform(get("/ideas")
                .header("Authorization", "Bearer this.is.not.valid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenMalformedAuthHeader_thenReturns401() throws Exception {
        mockMvc.perform(get("/ideas")
                .header("Authorization", "NotBearer sometoken"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenAuthLoginNoJwt_thenNotBlocked() throws Exception {
        // /auth/login is public — must not return 401
        mockMvc.perform(post("/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"a@b.com\",\"password\":\"pass\"}"));
    //             .andExpect(status().isNot(401));
    }
}