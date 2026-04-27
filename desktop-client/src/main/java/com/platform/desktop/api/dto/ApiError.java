package com.platform.desktop.api.dto;

/** Mirror of the {@code {"error": "..."}} body returned by services on 4xx/5xx. */
public class ApiError {
    public String error;

    public ApiError() {}
}
