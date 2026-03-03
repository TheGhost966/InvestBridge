package com.platform.deal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * DEAL SERVICE — Manages investment offers and matches.
 *
 * Responsibilities:
 *  - Investors create profiles (sectors, investment range)
 *  - Investors make offers on verified ideas
 *  - Founders accept or reject offers
 *  - When an offer is accepted → a Match is created
 *  - Any user can report abuse
 *
 * Port: 8083 (internal only — NOT exposed outside Docker network)
 * Security: Rejects any request missing X-Internal-Request: true header
 */
@SpringBootApplication
public class DealServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DealServiceApplication.class, args);
    }
}