package com.platform.deal.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document("investor_profiles")
public class InvestorProfile {

    @Id private String id;
    @Indexed(unique = true) private String investorId;
    private String bio;
    private List<String> sectors;
    private Double minInvestment;
    private Double maxInvestment;

    public InvestorProfile() {}

    public String getId()                          { return id; }
    public void   setId(String id)                 { this.id = id; }
    public String getInvestorId()                  { return investorId; }
    public void   setInvestorId(String investorId) { this.investorId = investorId; }
    public String getBio()                         { return bio; }
    public void   setBio(String bio)               { this.bio = bio; }
    public List<String> getSectors()               { return sectors; }
    public void         setSectors(List<String> s) { this.sectors = s; }
    public Double getMinInvestment()               { return minInvestment; }
    public void   setMinInvestment(Double v)       { this.minInvestment = v; }
    public Double getMaxInvestment()               { return maxInvestment; }
    public void   setMaxInvestment(Double v)       { this.maxInvestment = v; }
}
