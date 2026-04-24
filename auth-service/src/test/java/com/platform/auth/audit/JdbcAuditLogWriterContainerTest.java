package com.platform.auth.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test for {@link JdbcAuditLogWriter} that boots a real
 * PostgreSQL container and exercises every branch of the raw-JDBC code.
 *
 * <p>Unlike {@link com.platform.auth.AuthLoginTest} and friends, which
 * mock the {@link AuditLogWriter} interface, this test runs the actual
 * {@link java.sql.PreparedStatement} / {@link java.sql.ResultSet} code
 * against a real database. That is the only way to catch SQL typos,
 * column-type mismatches, or generated-key handling bugs.</p>
 *
 * <p>The same {@code db/init.sql} the docker-compose service mounts is
 * run inside the container, so schema drift is impossible — if the SQL
 * doesn't compile, the container fails to start and these tests fail
 * loudly.</p>
 *
 * <p><b>Gated behind {@code -DrunDockerITs=true}</b> so the default
 * {@code mvn test} build doesn't fail on machines where Docker isn't
 * reachable by Testcontainers (e.g. Windows + Docker Desktop without
 * the TCP socket exposed). To run:</p>
 * <pre>
 *     mvn test -DrunDockerITs=true -Dtest=JdbcAuditLogWriterContainerTest
 * </pre>
 */
@Testcontainers
@EnabledIfSystemProperty(named = "runDockerITs", matches = "true")
class JdbcAuditLogWriterContainerTest {

    /**
     * One container per class — startup is the slow part (~3s), so
     * sharing across all tests keeps the suite snappy. Each test
     * TRUNCATEs in @BeforeEach to stay independent.
     */
    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("audit")
                    .withUsername("audit")
                    .withPassword("audit")
                    .withInitScript("db/init.sql");

    private DataSource dataSource;
    private JdbcAuditLogWriter writer;

    @BeforeEach
    void setup() throws Exception {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl(postgres.getJdbcUrl());
        ds.setUsername(postgres.getUsername());
        ds.setPassword(postgres.getPassword());
        this.dataSource = ds;
        this.writer = new JdbcAuditLogWriter(ds);

        // Reset between tests — cheaper than restarting the container.
        try (Connection c = ds.getConnection();
             Statement st = c.createStatement()) {
            st.execute("TRUNCATE TABLE audit_logs RESTART IDENTITY");
        }
    }

    @Test
    void log_insertsRow_andPopulatesGeneratedId() {
        AuditEvent in = new AuditEvent("user-42", "zoe@example.com", AuditEvent.EVENT_LOGIN);

        AuditEvent out = writer.log(in);

        assertThat(out.getId()).isNotNull().isPositive();
        assertThat(out.getUserId()).isEqualTo("user-42");
        assertThat(out.getEmail()).isEqualTo("zoe@example.com");
        assertThat(out.getEvent()).isEqualTo(AuditEvent.EVENT_LOGIN);
        assertThat(writer.count()).isEqualTo(1);
    }

    @Test
    void log_withNullOccurredAt_defaultsToNow() {
        AuditEvent e = new AuditEvent();
        e.setUserId("user-1");
        e.setEmail(null);                       // logout path — email is null
        e.setEvent(AuditEvent.EVENT_LOGOUT);
        e.setOccurredAt(null);                  // writer must fill this in

        AuditEvent saved = writer.log(e);

        assertThat(saved.getOccurredAt()).isNotNull();
        long delta = Math.abs(ChronoUnit.SECONDS.between(saved.getOccurredAt(), Instant.now()));
        assertThat(delta).isLessThanOrEqualTo(5);

        // Re-read through findRecent to prove the row is really in the DB.
        List<AuditEvent> reloaded = writer.findRecent("user-1", 10);
        assertThat(reloaded).hasSize(1);
        assertThat(reloaded.get(0).getEmail()).isNull();
        assertThat(reloaded.get(0).getEvent()).isEqualTo(AuditEvent.EVENT_LOGOUT);
    }

    @Test
    void findRecent_returnsNewestFirst_andRespectsLimit() throws Exception {
        Instant base = Instant.now().minusSeconds(60);
        writer.log(eventAt("user-X", AuditEvent.EVENT_LOGIN,    base));
        writer.log(eventAt("user-X", AuditEvent.EVENT_LOGOUT,   base.plusSeconds(10)));
        writer.log(eventAt("user-X", AuditEvent.EVENT_LOGIN,    base.plusSeconds(20)));
        writer.log(eventAt("user-Y", AuditEvent.EVENT_LOGIN,    base.plusSeconds(30)));   // noise

        List<AuditEvent> recent = writer.findRecent("user-X", 2);

        assertThat(recent).hasSize(2);
        // DESC ordering: most recent first
        assertThat(recent.get(0).getOccurredAt()).isAfter(recent.get(1).getOccurredAt());
        assertThat(recent).allSatisfy(e -> assertThat(e.getUserId()).isEqualTo("user-X"));
    }

    @Test
    void findRecent_unknownUser_returnsEmptyList() {
        writer.log(new AuditEvent("somebody-else", "a@b.c", AuditEvent.EVENT_LOGIN));

        List<AuditEvent> recent = writer.findRecent("no-such-user", 10);

        assertThat(recent).isEmpty();
    }

    @Test
    void count_reflectsInsertions() {
        assertThat(writer.count()).isZero();
        writer.log(new AuditEvent("u1", "a@x", AuditEvent.EVENT_LOGIN));
        writer.log(new AuditEvent("u2", "b@x", AuditEvent.EVENT_REGISTER));
        assertThat(writer.count()).isEqualTo(2);
    }

    @Test
    void log_wrapsSqlException_inAuditPersistenceException() throws Exception {
        // Drop the table → next INSERT fails with "relation does not exist".
        try (Connection c = dataSource.getConnection();
             Statement st = c.createStatement()) {
            st.execute("DROP TABLE audit_logs");
        }

        AuditEvent e = new AuditEvent("u1", "x@y", AuditEvent.EVENT_LOGIN);

        assertThatThrownBy(() -> writer.log(e))
                .isInstanceOf(AuditPersistenceException.class)
                .hasCauseInstanceOf(java.sql.SQLException.class);

        // Restore schema so @BeforeEach TRUNCATE for the next test still works.
        try (Connection c = dataSource.getConnection();
             Statement st = c.createStatement()) {
            st.execute("""
                CREATE TABLE audit_logs (
                    id           BIGSERIAL    PRIMARY KEY,
                    user_id      TEXT         NOT NULL,
                    email        TEXT,
                    event        TEXT         NOT NULL,
                    occurred_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
                )
            """);
        }
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private static AuditEvent eventAt(String userId, String event, Instant when) {
        AuditEvent e = new AuditEvent(userId, userId + "@x.io", event);
        e.setOccurredAt(when);
        return e;
    }
}
