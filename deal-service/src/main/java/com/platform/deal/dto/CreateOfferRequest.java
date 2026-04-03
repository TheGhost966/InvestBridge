package com.platform.deal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class CreateOfferRequest {

    @NotBlank private String ideaId;
    @NotBlank private String founderId;
    @Positive private Double amount;
    private String message;

    public CreateOfferRequest() {}

    public String getIdeaId()                   { return ideaId; }
    public void   setIdeaId(String v)           { this.ideaId = v; }
    public String getFounderId()                { return founderId; }
    public void   setFounderId(String v)        { this.founderId = v; }
    public Double getAmount()                   { return amount; }
    public void   setAmount(Double v)           { this.amount = v; }
    public String getMessage()                  { return message; }
    public void   setMessage(String v)          { this.message = v; }
}
