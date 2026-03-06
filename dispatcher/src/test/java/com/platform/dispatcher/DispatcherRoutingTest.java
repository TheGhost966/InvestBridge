package com.platform.dispatcher;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ROUTING TESTS - RED PHASE
 * Written BEFORE implementation. Commit these failing tests first!
 * git commit -m "test: add routing tests RED"
 */
@SpringBootTest
@AutoConfigureMockMvc
class DispatcherRoutingTest {

    @Autowired
    private MockMvc mockMvc;

    // /ideas/** is a protected route → dispatcher knows it → returns 401 (not 404)
    @Test
    void whenIdeasPath_thenRouteIsKnownReturns401NotFound() throws Exception {
        mockMvc.perform(get("/ideas"))
                .andExpect(status().isUnauthorized()); // 401 proves route is registered
    }

    // /deals/** is a protected route → returns 401 (not 404)
    @Test
    void whenDealsPath_thenRouteIsKnownReturns401() throws Exception {
        mockMvc.perform(get("/deals/offers"))
                .andExpect(status().isUnauthorized()); // 401 proves route is registered
    }

    // Completely unknown path → must return 404
    @Test
    void whenUnknownPath_thenReturns404() throws Exception {
        mockMvc.perform(get("/unknown/path/xyz"))
                .andExpect(status().isNotFound()); // 404
    }

    // /actuator/health is always public
    @Test
    void whenActuatorHealth_thenReturns200() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk()); // 200
    }
}