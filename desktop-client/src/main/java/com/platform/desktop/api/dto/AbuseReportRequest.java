package com.platform.desktop.api.dto;

public class AbuseReportRequest {

    public String targetId;
    public String reason;

    public AbuseReportRequest() {}

    public AbuseReportRequest(String targetId, String reason) {
        this.targetId = targetId;
        this.reason = reason;
    }
}
