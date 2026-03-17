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
class DispatcherErrorTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void whenUnknownRoute_thenReturns404() throws Exception {
        mockMvc.perform(get("/this/does/not/exist"))
                .andExpect(status().isNotFound());
    }

    @Test
    void whenRootPath_thenReturns404() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isNotFound());
    }

    @Test
    void whenActuatorHealth_thenReturns200() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void whenSpoofedInternalHeader_thenStillReturns401() throws Exception {
        mockMvc.perform(get("/ideas")
                .header("X-Internal-Request", "true"))
                .andExpect(status().isUnauthorized());
    }
}