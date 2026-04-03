package com.platform.deal.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("abuse_reports")
public class AbuseReport {

    @Id private String id;
    private String reporterId;
    private String targetId;
    private String reason;
    private Instant createdAt = Instant.now();

    public AbuseReport() {}

    public AbuseReport(String reporterId, String targetId, String reason) {
        this.reporterId = reporterId;
        this.targetId   = targetId;
        this.reason     = reason;
    }

    public String getId()                    { return id; }
    public void   setId(String id)           { this.id = id; }
    public String getReporterId()            { return reporterId; }
    public void   setReporterId(String v)    { this.reporterId = v; }
    public String getTargetId()              { return targetId; }
    public void   setTargetId(String v)      { this.targetId = v; }
    public String getReason()                { return reason; }
    public void   setReason(String v)        { this.reason = v; }
    public Instant getCreatedAt()            { return createdAt; }
}
