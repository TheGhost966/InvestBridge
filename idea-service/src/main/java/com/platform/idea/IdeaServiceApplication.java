package com.platform.idea;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * IDEA SERVICE — Core business logic for startup ideas.
 *
 * Responsibilities:
 *  - Founders create and manage their startup ideas
 *  - Ideas go through a status lifecycle: DRAFT → SUBMITTED → VERIFIED/REJECTED
 *  - Admins verify or reject submitted ideas
 *  - Investors browse only VERIFIED ideas
 *  - Supports pagination and filtering
 *
 * Port: 8082 (internal only — NOT exposed outside Docker network)
 * Security: Rejects any request missing X-Internal-Request: true header
 */
@SpringBootApplication
public class IdeaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdeaServiceApplication.class, args);
    }
}