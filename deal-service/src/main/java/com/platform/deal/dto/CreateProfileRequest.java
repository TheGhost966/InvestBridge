package com.platform.deal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.List;

public class CreateProfileRequest {

    @NotBlank private String bio;
    private List<String> sectors;
    @Positive private Double minInvestment;
    @Positive private Double maxInvestment;

    public CreateProfileRequest() {}

    public String getBio()                         { return bio; }
    public void   setBio(String bio)               { this.bio = bio; }
    public List<String> getSectors()               { return sectors; }
    public void         setSectors(List<String> s) { this.sectors = s; }
    public Double getMinInvestment()               { return minInvestment; }
    public void   setMinInvestment(Double v)       { this.minInvestment = v; }
    public Double getMaxInvestment()               { return maxInvestment; }
    public void   setMaxInvestment(Double v)       { this.maxInvestment = v; }
}
