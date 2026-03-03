package com.platform.dispatcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * DISPATCHER — The only public-facing service.
 *
 * Responsibilities:
 *  - Validates JWT tokens on every incoming request
 *  - Checks user role has permission for the requested route
 *  - Forwards the request to the correct internal service
 *  - Records metrics (request count, latency) for Grafana
 *  - Injects correlation_id into every log line
 *
 * Port: 8080 (the ONLY port exposed to the internet)
 */
@SpringBootApplication
public class DispatcherApplication {

    public static void main(String[] args) {
        SpringApplication.run(DispatcherApplication.class, args);
    }
}