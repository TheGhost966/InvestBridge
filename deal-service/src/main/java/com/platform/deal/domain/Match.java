package com.platform.deal.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("matches")
public class Match {

    @Id private String id;
    private String investorId;
    private String founderId;
    private String ideaId;
    private String offerId;
    private Instant createdAt = Instant.now();

    public Match() {}

    public Match(String investorId, String founderId, String ideaId, String offerId) {
        this.investorId = investorId;
        this.founderId  = founderId;
        this.ideaId     = ideaId;
        this.offerId    = offerId;
    }

    public String getId()                        { return id; }
    public void   setId(String id)               { this.id = id; }
    public String getInvestorId()                { return investorId; }
    public void   setInvestorId(String v)        { this.investorId = v; }
    public String getFounderId()                 { return founderId; }
    public void   setFounderId(String v)         { this.founderId = v; }
    public String getIdeaId()                    { return ideaId; }
    public void   setIdeaId(String v)            { this.ideaId = v; }
    public String getOfferId()                   { return offerId; }
    public void   setOfferId(String v)           { this.offerId = v; }
    public Instant getCreatedAt()                { return createdAt; }
}
