package com.platform.desktop.api.dto;

public enum Role {
    FOUNDER, INVESTOR, ADMIN;

    public static Role fromString(String value) {
        if (value == null) return null;
        return Role.valueOf(value.trim().toUpperCase());
    }
}
