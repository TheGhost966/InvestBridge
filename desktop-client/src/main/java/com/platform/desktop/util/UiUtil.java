package com.platform.desktop.util;

import com.platform.desktop.api.ApiException;

/**
 * Small view-layer helpers shared across controllers.
 */
public final class UiUtil {

    private UiUtil() {}

    /**
     * Map an exception from an API call to a message safe to show a user.
     * Falls back to the raw {@code getMessage()} for anything unrecognised.
     */
    public static String friendly(Throwable t) {
        if (t == null) return "Unknown error";
        Throwable root = t;
        if (!(root instanceof ApiException) && t.getCause() instanceof ApiException ce) {
            root = ce;
        }
        if (root instanceof ApiException ex) {
            if (ex.isTransportError()) {
                return "Cannot reach the server. Is the dispatcher running on :8080?";
            }
            return switch (ex.getStatusCode()) {
                case 400 -> "Invalid input: "     + orDefault(ex.getServerMessage(), "please check the form");
                case 401 -> "Invalid credentials or expired session.";
                case 403 -> "You don't have permission to do that.";
                case 404 -> "Not found: "         + orDefault(ex.getServerMessage(), "resource missing");
                case 409 -> orDefault(ex.getServerMessage(), "Conflict — the resource already exists.");
                case 503 -> "Service temporarily unavailable. Please try again.";
                default  -> "Error " + ex.getStatusCode() + ": "
                            + orDefault(ex.getServerMessage(), "unexpected response");
            };
        }
        return orDefault(t.getMessage(), t.getClass().getSimpleName());
    }

    private static String orDefault(String s, String fallback) {
        return (s == null || s.isBlank()) ? fallback : s;
    }
}
