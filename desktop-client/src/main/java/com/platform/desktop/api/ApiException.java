package com.platform.desktop.api;

/**
 * Thrown by {@link ApiClient} for any non-2xx response, or for transport / parse
 * failures. Controllers catch this to show status-appropriate messages
 * (401 → "Invalid credentials", 409 → "Email taken", 503 → "Service unavailable", …).
 */
public class ApiException extends RuntimeException {

    /** -1 when the failure happened before an HTTP response was received (I/O, parse). */
    private final int statusCode;
    /** Server-supplied error string ({@code {"error": "..."}}), or a client-side message. */
    private final String serverMessage;

    public ApiException(int statusCode, String serverMessage) {
        super("HTTP " + statusCode + (serverMessage != null ? " — " + serverMessage : ""));
        this.statusCode = statusCode;
        this.serverMessage = serverMessage;
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.serverMessage = message;
    }

    public int getStatusCode()       { return statusCode; }
    public String getServerMessage() { return serverMessage; }

    public boolean isTransportError() { return statusCode == -1; }
    public boolean isUnauthorized()   { return statusCode == 401; }
    public boolean isForbidden()      { return statusCode == 403; }
    public boolean isNotFound()       { return statusCode == 404; }
    public boolean isConflict()       { return statusCode == 409; }
}
