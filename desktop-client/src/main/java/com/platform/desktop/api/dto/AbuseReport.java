package com.platform.desktop.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AbuseReport {

    public String id;
    public String reporterId;
    public String targetId;
    public String reason;
    public Instant createdAt;

    public AbuseReport() {}
}
