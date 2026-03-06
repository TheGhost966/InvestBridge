package com.platform.dispatcher;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AUTHORIZATION TESTS - RED PHASE
 * Written BEFORE implementation. Commit these failing tests first!
 * git commit -m "test: add authz tests RED"
 */
@SpringBootTest
@AutoConfigureMockMvc
class DispatcherAuthzTest {

    @Autowired
    private MockMvc mockMvc;

    // No JWT on protected route → must return exactly 401
    @Test
    void whenNoJwtOnIdeas_thenReturns401() throws Exception {
        mockMvc.perform(get("/ideas"))
                .andExpect(status().isUnauthorized()); // 401
    }

    // No JWT on deals → must return exactly 401
    @Test
    void whenNoJwtOnDeals_thenReturns401() throws Exception {
        mockMvc.perform(get("/deals/offers"))
                .andExpect(status().isUnauthorized()); // 401
    }

    // Completely invalid JWT string → must return 401
    @Test
    void whenInvalidJwt_thenReturns401() throws Exception {
        mockMvc.perform(get("/ideas")
                .header("Authorization", "Bearer this.is.not.valid"))
                .andExpect(status().isUnauthorized()); // 401
    }

    // Wrong prefix (not "Bearer ") → must return 401
    @Test
    void whenMalformedAuthHeader_thenReturns401() throws Exception {
        mockMvc.perform(get("/ideas")
                .header("Authorization", "NotBearer sometoken"))
                .andExpect(status().isUnauthorized()); // 401
    }

    // /auth/login is PUBLIC → dispatcher must not block it with 401
    // (it may return 503 if auth-service is down, but never 401)
    @Test
    void whenAuthLoginNoJwt_thenRouteIsPublic() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"a@b.com\",\"password\":\"pass\"}"));
    }
}