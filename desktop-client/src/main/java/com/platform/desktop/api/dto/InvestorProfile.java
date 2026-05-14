package com.platform.desktop.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InvestorProfile {

    public String id;
    public String investorId;
    public String bio;
    public List<String> sectors;
    public Double minInvestment;
    public Double maxInvestment;

    public InvestorProfile() {}
}
