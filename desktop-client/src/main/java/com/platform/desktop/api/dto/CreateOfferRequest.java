package com.platform.desktop.api.dto;

public class CreateOfferRequest {

    public String ideaId;
    public String founderId;
    public Double amount;
    public String message;

    public CreateOfferRequest() {}

    public CreateOfferRequest(String ideaId, String founderId, Double amount, String message) {
        this.ideaId = ideaId;
        this.founderId = founderId;
        this.amount = amount;
        this.message = message;
    }
}
