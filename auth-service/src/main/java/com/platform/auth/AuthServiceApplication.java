package com.platform.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AUTH SERVICE — Handles all identity and access management.
 *
 * Responsibilities:
 *  - Register new users (hashes password with BCrypt, saves to MongoDB)
 *  - Login (verify password, issue JWT token)
 *  - Logout (blacklist token in Redis so it can't be reused)
 *  - Return current user profile (/auth/me)
 *  - Write audit log on every login/logout
 *
 * Port: 8081 (internal only — NOT exposed outside Docker network)
 * Security: Rejects any request missing X-Internal-Request: true header
 */
@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}