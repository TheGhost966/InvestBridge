package com.platform.dispatcher;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ERROR HANDLING TESTS - RED PHASE
 * Written BEFORE implementation. Commit these failing tests first!
 * git commit -m "test: add error handling tests RED"
 */
@SpringBootTest
@AutoConfigureMockMvc
class DispatcherErrorTest {

    @Autowired
    private MockMvc mockMvc;

    // Unknown route → 404
    @Test
    void whenUnknownRoute_thenReturns404() throws Exception {
        mockMvc.perform(get("/this/does/not/exist"))
                .andExpect(status().isNotFound()); // 404
    }

    // Root path → 404
    @Test
    void whenRootPath_thenReturns404() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isNotFound()); // 404
    }

    // Protected route + no JWT → exactly 401 (not 403, not 404)
    @Test
    void whenProtectedRouteNoJwt_thenExactly401() throws Exception {
        mockMvc.perform(get("/ideas"))
                .andExpect(status().isUnauthorized()); // 401
    }

    // Actuator health → always 200, no JWT needed
    @Test
    void whenActuatorHealth_thenReturns200() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk()); // 200
    }

    // Spoofed internal header → still 401, JWT still required
    @Test
    void whenSpoofedInternalHeader_thenStillReturns401() throws Exception {
        mockMvc.perform(get("/ideas")
                .header("X-Internal-Request", "true"))
                .andExpect(status().isUnauthorized()); // 401
    }
}