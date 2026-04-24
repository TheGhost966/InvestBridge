package com.platform.auth;

import com.platform.auth.audit.AuditEvent;
import com.platform.auth.audit.AuditLogWriter;
import com.platform.auth.domain.Role;
import com.platform.auth.domain.User;
import com.platform.auth.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthMeTest {

    @Autowired MockMvc mockMvc;
    @Autowired PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @MockBean UserRepository userRepository;
    @MockBean AuditLogWriter auditLogWriter;

    private User testUser;
    private String validToken;

    @BeforeEach
    void setup() {
        testUser = new User("carol@example.com",
                passwordEncoder.encode("pass123"), Role.INVESTOR);
        testUser.setId("user-002");

        when(userRepository.findById("user-002")).thenReturn(Optional.of(testUser));
        when(auditLogWriter.log(any(AuditEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        validToken = Jwts.builder()
                .subject("user-002")
                .claim("role", "INVESTOR")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .compact();
    }

    @Test
    void getMe_validJwt_returns200WithEmail() throws Exception {
        mockMvc.perform(get("/auth/me")
                .header("X-Internal-Request", "true")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("carol@example.com"));
    }

    @Test
    void getMe_noJwt_returns401() throws Exception {
        mockMvc.perform(get("/auth/me")
                .header("X-Internal-Request", "true"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMe_missingInternalHeader_returns403() throws Exception {
        mockMvc.perform(get("/auth/me")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void logout_validJwt_returns200() throws Exception {
        mockMvc.perform(post("/auth/logout")
                .header("X-Internal-Request", "true")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());
    }

    @Test
    void logout_missingInternalHeader_returns403() throws Exception {
        mockMvc.perform(post("/auth/logout")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isForbidden());
    }
}
