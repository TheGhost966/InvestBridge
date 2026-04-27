package com.platform.desktop.api.dto;

/** Mirror of {@code com.platform.idea.domain.IdeaStatus}. */
public enum IdeaStatus {
    DRAFT, SUBMITTED, VERIFIED, REJECTED;

    public static IdeaStatus fromString(String value) {
        if (value == null) return null;
        return IdeaStatus.valueOf(value.trim().toUpperCase());
    }
}
