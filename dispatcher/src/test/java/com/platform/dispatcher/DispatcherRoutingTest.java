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
class DispatcherRoutingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void whenIdeasPath_thenReturns401NotFound() throws Exception {
        mockMvc.perform(get("/ideas"))
                .andExpect(status().isUnauthorized()); // 401 = route known + protected
    }

    @Test
    void whenDealsPath_thenReturns401NotFound() throws Exception {
        mockMvc.perform(get("/deals/offers"))
                .andExpect(status().isUnauthorized()); // 401 = route known + protected
    }

    @Test
    void whenUnknownPath_thenReturns404() throws Exception {
        mockMvc.perform(get("/unknown/path/xyz"))
                .andExpect(status().isNotFound()); // 404
    }

    @Test
    void whenActuatorHealth_thenReturns200() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk()); // 200
    }
}