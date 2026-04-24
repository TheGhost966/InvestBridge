package com.platform.auth.audit;

import java.time.Instant;

/**
 * Plain domain record for an authentication audit event.
 * Persisted via JDBC into the {@code audit_logs} table in PostgreSQL.
 *
 * <p>Intentionally has no Spring Data or Mongo annotations — the storage
 * concern lives in {@link JdbcAuditLogWriter}, keeping this class a pure
 * POJO that satisfies the Single Responsibility Principle.</p>
 */
public final class AuditEvent {

    public static final String EVENT_LOGIN    = "LOGIN";
    public static final String EVENT_LOGOUT   = "LOGOUT";
    public static final String EVENT_REGISTER = "REGISTER";

    private Long id;
    private String userId;
    private String email;
    private String event;
    private Instant occurredAt;

    public AuditEvent() {}

    public AuditEvent(String userId, String email, String event) {
        this.userId     = userId;
        this.email      = email;
        this.event      = event;
        this.occurredAt = Instant.now();
    }

    public Long getId()             { return id; }
    public void setId(Long id)      { this.id = id; }
    public String getUserId()       { return userId; }
    public void setUserId(String u) { this.userId = u; }
    public String getEmail()        { return email; }
    public void setEmail(String e)  { this.email = e; }
    public String getEvent()        { return event; }
    public void setEvent(String e)  { this.event = e; }
    public Instant getOccurredAt()  { return occurredAt; }
    public void setOccurredAt(Instant t) { this.occurredAt = t; }
}
