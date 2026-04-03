package com.platform.idea.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.List;

public class CreateIdeaRequest {

    @NotBlank private String title;
    @NotBlank private String summary;
    private String market;
    private String traction;
    @Positive  private Double fundingNeeded;
    private String location;
    private List<String> tags;

    public CreateIdeaRequest() {}

    public String getTitle()                   { return title; }
    public void   setTitle(String title)       { this.title = title; }
    public String getSummary()                 { return summary; }
    public void   setSummary(String summary)   { this.summary = summary; }
    public String getMarket()                  { return market; }
    public void   setMarket(String market)     { this.market = market; }
    public String getTraction()                { return traction; }
    public void   setTraction(String t)        { this.traction = t; }
    public Double getFundingNeeded()                     { return fundingNeeded; }
    public void   setFundingNeeded(Double f)             { this.fundingNeeded = f; }
    public String getLocation()                { return location; }
    public void   setLocation(String l)        { this.location = l; }
    public List<String> getTags()              { return tags; }
    public void         setTags(List<String> t){ this.tags = t; }
}
