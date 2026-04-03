package com.platform.deal.dto;

import jakarta.validation.constraints.NotBlank;

public class AbuseReportRequest {

    @NotBlank private String targetId;
    @NotBlank private String reason;

    public AbuseReportRequest() {}

    public String getTargetId()              { return targetId; }
    public void   setTargetId(String v)      { this.targetId = v; }
    public String getReason()                { return reason; }
    public void   setReason(String v)        { this.reason = v; }
}
