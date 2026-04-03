package com.platform.idea.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document("ideas")
public class Idea {

    @Id private String id;
    private String title;
    private String summary;
    private String market;
    private String traction;
    private Double fundingNeeded;
    private String location;
    private List<String> tags;
    private String founderId;
    private IdeaStatus status = IdeaStatus.DRAFT;
    private String rejectionReason;
    private Instant createdAt = Instant.now();

    public Idea() {}

    public String getId()                      { return id; }
    public void   setId(String id)             { this.id = id; }
    public String getTitle()                   { return title; }
    public void   setTitle(String title)       { this.title = title; }
    public String getSummary()                 { return summary; }
    public void   setSummary(String summary)   { this.summary = summary; }
    public String getMarket()                  { return market; }
    public void   setMarket(String market)     { this.market = market; }
    public String getTraction()                { return traction; }
    public void   setTraction(String traction) { this.traction = traction; }
    public Double getFundingNeeded()                       { return fundingNeeded; }
    public void   setFundingNeeded(Double fundingNeeded)   { this.fundingNeeded = fundingNeeded; }
    public String getLocation()                    { return location; }
    public void   setLocation(String location)     { this.location = location; }
    public List<String> getTags()                  { return tags; }
    public void         setTags(List<String> tags) { this.tags = tags; }
    public String getFounderId()                       { return founderId; }
    public void   setFounderId(String founderId)       { this.founderId = founderId; }
    public IdeaStatus getStatus()                      { return status; }
    public void       setStatus(IdeaStatus status)     { this.status = status; }
    public String getRejectionReason()                         { return rejectionReason; }
    public void   setRejectionReason(String rejectionReason)   { this.rejectionReason = rejectionReason; }
    public Instant getCreatedAt()                  { return createdAt; }
    public void    setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
