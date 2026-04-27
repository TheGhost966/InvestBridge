package com.platform.desktop.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.platform.desktop.api.dto.ApiError;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Thin synchronous HTTP client wrapping the JDK's {@link HttpClient}.
 *
 * <p>Single source of truth for the base URL, JSON (de)serialisation and
 * {@code Authorization: Bearer …} header injection. Endpoint-specific
 * classes (e.g. {@code AuthApi}) compose this client rather than inheriting
 * from it — Composition over inheritance.
 *
 * <p>Threading: all methods block. Call from a background thread
 * (a {@link javafx.concurrent.Task} or {@link java.util.concurrent.CompletableFuture#supplyAsync})
 * and deliver results back to the FX thread via {@link javafx.application.Platform#runLater}.
 */
public class ApiClient {

    public static final String DEFAULT_BASE_URL = "http://localhost:8080";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private final String baseUrl;
    private final HttpClient http;
    private final ObjectMapper mapper;

    public ApiClient() {
        this(DEFAULT_BASE_URL);
    }

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.http = HttpClient.newBuilder()
                .connectTimeout(DEFAULT_TIMEOUT)
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public String baseUrl() { return baseUrl; }
    public ObjectMapper mapper() { return mapper; }

    // ─── typed convenience helpers ──────────────────────────────────────────────

    public <T> T get(String path, Class<T> responseType) {
        return send("GET", path, null, responseType);
    }

    public <T> T post(String path, Object body, Class<T> responseType) {
        return send("POST", path, body, responseType);
    }

    public <T> T put(String path, Object body, Class<T> responseType) {
        return send("PUT", path, body, responseType);
    }

    public <T> T patch(String path, Object body, Class<T> responseType) {
        return send("PATCH", path, body, responseType);
    }

    public void delete(String path) {
        send("DELETE", path, null, Void.class);
    }

    // ─── core send ──────────────────────────────────────────────────────────────

    /**
     * @param responseType {@code Void.class} to ignore the response body
     *                     (also used for empty 204 responses).
     */
    public <T> T send(String method, String path, Object body, Class<T> responseType) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(DEFAULT_TIMEOUT)
                .header("Accept", "application/json");

        String token = SessionManager.token();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }

        HttpRequest.BodyPublisher publisher;
        if (body == null) {
            publisher = HttpRequest.BodyPublishers.noBody();
        } else {
            try {
                publisher = HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body));
                builder.header("Content-Type", "application/json");
            } catch (IOException e) {
                throw new ApiException("Failed to serialise request body", e);
            }
        }
        builder.method(method, publisher);

        HttpResponse<String> response;
        try {
            response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ApiException("Cannot reach " + baseUrl + path
                    + " — is the dispatcher running?", e);
        }

        int status = response.statusCode();
        String payload = response.body();

        if (status >= 200 && status < 300) {
            if (responseType == Void.class || payload == null || payload.isBlank()) {
                return null;
            }
            try {
                return mapper.readValue(payload, responseType);
            } catch (IOException e) {
                throw new ApiException("Failed to parse response from " + path, e);
            }
        }

        // Non-2xx: try to parse the {"error": "..."} envelope; fall back to raw body.
        String serverMessage = payload;
        if (payload != null && !payload.isBlank()) {
            try {
                ApiError err = mapper.readValue(payload, ApiError.class);
                if (err.error != null && !err.error.isBlank()) {
                    serverMessage = err.error;
                }
            } catch (IOException ignored) {
                // leave serverMessage as raw body
            }
        }
        throw new ApiException(status, serverMessage);
    }
}
