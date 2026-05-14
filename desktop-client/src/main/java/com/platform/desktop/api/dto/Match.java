package com.platform.desktop.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Match {

    public String id;
    public String investorId;
    public String founderId;
    public String ideaId;
    public String offerId;
    public Instant createdAt;

    public Match() {}
}
