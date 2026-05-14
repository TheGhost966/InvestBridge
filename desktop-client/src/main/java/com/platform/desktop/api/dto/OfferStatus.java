package com.platform.desktop.api.dto;

/** Mirror of {@code com.platform.deal.domain.OfferStatus}. */
public enum OfferStatus {
    PENDING, ACCEPTED, REJECTED;

    public static OfferStatus fromString(String value) {
        if (value == null) return null;
        return OfferStatus.valueOf(value.trim().toUpperCase());
    }
}
