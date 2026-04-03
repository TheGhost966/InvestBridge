package com.platform.deal.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("offers")
public class Offer {

    @Id private String id;
    private String investorId;
    private String ideaId;
    private String founderId;
    private Double amount;
    private String message;
    private OfferStatus status = OfferStatus.PENDING;
    private Instant createdAt = Instant.now();

    public Offer() {}

    public String getId()                       { return id; }
    public void   setId(String id)              { this.id = id; }
    public String getInvestorId()               { return investorId; }
    public void   setInvestorId(String v)       { this.investorId = v; }
    public String getIdeaId()                   { return ideaId; }
    public void   setIdeaId(String v)           { this.ideaId = v; }
    public String getFounderId()                { return founderId; }
    public void   setFounderId(String v)        { this.founderId = v; }
    public Double getAmount()                   { return amount; }
    public void   setAmount(Double v)           { this.amount = v; }
    public String getMessage()                  { return message; }
    public void   setMessage(String v)          { this.message = v; }
    public OfferStatus getStatus()              { return status; }
    public void        setStatus(OfferStatus s) { this.status = s; }
    public Instant getCreatedAt()               { return createdAt; }
    public void    setCreatedAt(Instant v)      { this.createdAt = v; }
}
