package com.platform.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI SERVICE — Optional bonus service for AI-powered features.
 *
 * Responsibilities:
 *  - Analyze idea quality and return feedback
 *  - Suggest matching investors for an idea
 *  - Return a risk/fraud score for an idea
 *  - Rate limiting per user (Redis counter)
 *  - Cache results in MongoDB (same input = same output for 24h)
 *
 * Port: 8084 (internal only — NOT exposed outside Docker network)
 * Security: Rejects any request missing X-Internal-Request: true header
 */
@SpringBootApplication
public class AiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}