package com.platform.dispatcher;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DispatcherAuthzTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${jwt.secret}")
    private String jwtSecret;

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
                .content("{\"email\":\"a@b.com\",\"password\":\"pass\"}"))
                .andExpect(result ->
                        org.junit.jupiter.api.Assertions.assertNotEquals(
                                401, result.getResponse().getStatus()));
    }

    @Test
    void whenExpiredJwt_thenReturns401() throws Exception {
        // Create expired token (1 hour in the past)
        Instant now = Instant.now();
        Instant expiredTime = now.minusSeconds(3600); // 1 hour ago
        Instant issuedAt = now.minusSeconds(7200); // 2 hours ago
        
        String expiredToken = Jwts.builder()
                .subject("testuser")
                .claim("role", "INVESTOR")
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiredTime))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .compact();
        
        mockMvc.perform(get("/ideas")
                .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenValidJwtWrongRole_thenReturns403() throws Exception {
        // Create valid token with FOUNDER role (not authorized)
        Instant now = Instant.now();
        Instant expirationTime = now.plusSeconds(86400); // 1 day from now
        
        String wrongRoleToken = Jwts.builder()
                .subject("testuser")
                .claim("role", "FOUNDER")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expirationTime))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                .compact();
        
        mockMvc.perform(get("/ideas")
                .header("Authorization", "Bearer " + wrongRoleToken))
                .andExpect(status().isForbidden());
    }
}