package com.platform.desktop.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Offer {

    public String id;
    public String investorId;
    public String ideaId;
    public String founderId;
    public Double amount;
    public String message;
    public OfferStatus status;
    public Instant createdAt;

    public Offer() {}

    public double amountOrZero() {
        return amount == null ? 0.0 : amount;
    }
}
