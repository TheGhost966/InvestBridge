package com.platform.auth.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("audit_logs")
public class AuditLog {

    @Id private String id;
    private String userId;
    private String email;
    private String event; // LOGIN, LOGOUT
    private Instant timestamp = Instant.now();

    public AuditLog() {}

    public AuditLog(String userId, String email, String event) {
        this.userId = userId;
        this.email  = email;
        this.event  = event;
    }

    public String getId()       { return id; }
    public String getUserId()   { return userId; }
    public String getEmail()    { return email; }
    public String getEvent()    { return event; }
    public Instant getTimestamp(){ return timestamp; }
}
