package com.platform.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Generic response envelope used by every microservice.
 *
 * <pre>
 * {
 *   "data":      T | null,
 *   "errors":    ["..."] | null,
 *   "timestamp": "2026-04-22T10:15:30Z"
 * }
 * </pre>
 *
 * Using a single shape across services lets the Android client parse every
 * endpoint with one Retrofit converter.
 *
 * @param <T> payload type (a DTO, list, page, primitive wrapper, ...)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ApiResponse<T> {

    private final T data;
    private final List<String> errors;
    private final Instant timestamp;

    private ApiResponse(T data, List<String> errors) {
        this.data = data;
        this.errors = errors;
        this.timestamp = Instant.now();
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data, null);
    }

    public static <T> ApiResponse<T> empty() {
        return new ApiResponse<>(null, null);
    }

    public static <T> ApiResponse<T> error(String... messages) {
        return new ApiResponse<>(null, List.of(messages));
    }

    public T getData() { return data; }
    public List<String> getErrors() { return errors; }
    public Instant getTimestamp() { return timestamp; }
}
