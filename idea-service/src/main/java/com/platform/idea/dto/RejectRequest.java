package com.platform.idea.dto;

import jakarta.validation.constraints.NotBlank;

public class RejectRequest {

    @NotBlank private String reason;

    public RejectRequest() {}
    public RejectRequest(String reason) { this.reason = reason; }

    public String getReason()              { return reason; }
    public void   setReason(String reason) { this.reason = reason; }
}
