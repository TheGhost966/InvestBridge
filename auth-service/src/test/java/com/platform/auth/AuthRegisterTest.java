package com.platform.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.auth.audit.AuditLogWriter;
import com.platform.auth.domain.Role;
import com.platform.auth.dto.RegisterRequest;
import com.platform.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthRegisterTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean UserRepository userRepository;
    @MockBean AuditLogWriter auditLogWriter;

    @BeforeEach
    void setup() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void register_validBody_returns201WithToken() throws Exception {
        RegisterRequest req = new RegisterRequest("alice@example.com", "secret123", Role.INVESTOR);

        mockMvc.perform(post("/auth/register")
                .header("X-Internal-Request", "true")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        com.platform.auth.domain.User existing = new com.platform.auth.domain.User();
        existing.setEmail("alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(existing));

        RegisterRequest req = new RegisterRequest("alice@example.com", "secret123", Role.INVESTOR);

        mockMvc.perform(post("/auth/register")
                .header("X-Internal-Request", "true")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_blankEmail_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest("", "secret123", Role.INVESTOR);

        mockMvc.perform(post("/auth/register")
                .header("X-Internal-Request", "true")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_blankPassword_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest("alice@example.com", "", Role.INVESTOR);

        mockMvc.perform(post("/auth/register")
                .header("X-Internal-Request", "true")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_missingInternalHeader_returns403() throws Exception {
        RegisterRequest req = new RegisterRequest("alice@example.com", "secret123", Role.INVESTOR);

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }
}
