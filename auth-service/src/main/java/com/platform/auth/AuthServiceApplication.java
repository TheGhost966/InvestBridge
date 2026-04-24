package com.platform.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;

/**
 * AUTH SERVICE — Handles all identity and access management.
 *
 * Responsibilities:
 *  - Register new users (hashes password with BCrypt, saves to MongoDB)
 *  - Login (verify password, issue JWT token)
 *  - Logout (blacklist token in Redis so it can't be reused)
 *  - Return current user profile (/auth/me)
 *  - Write audit log on every login/logout (raw JDBC to PostgreSQL)
 *
 * Why excluded auto-configs: the JDBC starter is on the classpath only to
 * let us hand-build a DataSource inside {@code AuditDataSourceConfig} when
 * {@code audit.jdbc.url} is present. Letting Spring Boot auto-configure a
 * Hikari DataSource would crash the context at startup in tests where no
 * URL is set. We own the DataSource lifecycle ourselves.
 *
 * Port: 8081 (internal only — NOT exposed outside Docker network)
 * Security: Rejects any request missing X-Internal-Request: true header
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class
})
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}