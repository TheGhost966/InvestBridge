package com.platform.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.auth.audit.AuditEvent;
import com.platform.auth.audit.AuditLogWriter;
import com.platform.auth.domain.Role;
import com.platform.auth.domain.User;
import com.platform.auth.dto.LoginRequest;
import com.platform.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
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
class AuthLoginTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired PasswordEncoder passwordEncoder;

    @MockBean UserRepository userRepository;
    @MockBean AuditLogWriter auditLogWriter;

    private User existingUser;

    @BeforeEach
    void setup() {
        existingUser = new User("bob@example.com",
                passwordEncoder.encode("correctpass"), Role.FOUNDER);
        existingUser.setId("user-001");

        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(existingUser));
        when(auditLogWriter.log(any(AuditEvent.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        LoginRequest req = new LoginRequest("bob@example.com", "correctpass");

        mockMvc.perform(post("/auth/login")
                .header("X-Internal-Request", "true")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        LoginRequest req = new LoginRequest("bob@example.com", "wrongpass");

        mockMvc.perform(post("/auth/login")
                .header("X-Internal-Request", "true")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_unknownEmail_returns401() throws Exception {
        LoginRequest req = new LoginRequest("nobody@example.com", "anypass");

        mockMvc.perform(post("/auth/login")
                .header("X-Internal-Request", "true")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_missingInternalHeader_returns403() throws Exception {
        LoginRequest req = new LoginRequest("bob@example.com", "correctpass");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }
}
