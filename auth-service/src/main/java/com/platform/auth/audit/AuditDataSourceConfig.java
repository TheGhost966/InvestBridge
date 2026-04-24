package com.platform.auth.audit;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Wires a dedicated {@link DataSource} for the audit log PostgreSQL instance.
 *
 * <p>Active only when {@code audit.jdbc.url} is set. This guard keeps
 * unit tests (which don't start PostgreSQL) from failing at context load.</p>
 *
 * <p>The bean is <b>qualified</b> so it never conflicts with any future
 * default DataSource Spring Boot might auto-configure — injected everywhere
 * via {@link Qualifier}{@code ("auditDataSource")}.</p>
 */
@Configuration
@ConditionalOnProperty(name = "audit.jdbc.url")
public class AuditDataSourceConfig {

    @Bean(name = "auditDataSource")
    public DataSource auditDataSource(
            @Value("${audit.jdbc.url}")      String url,
            @Value("${audit.jdbc.user}")     String user,
            @Value("${audit.jdbc.password}") String password) {

        return DataSourceBuilder.create()
                .driverClassName("org.postgresql.Driver")
                .url(url)
                .username(user)
                .password(password)
                .build();
    }
}
