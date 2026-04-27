package com.platform.desktop.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

/**
 * Mirror of {@code com.platform.common.api.ApiResponse&lt;T&gt;}.
 *
 * <p>Generic envelope wrapping a typed {@code data} payload, an optional
 * {@code errors} list, and an {@code Instant} {@code timestamp}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiResponse<T> {

    public T data;
    public List<String> errors;
    public Instant timestamp;

    public ApiResponse() {}

    public boolean isSuccess() {
        return errors == null || errors.isEmpty();
    }
}
