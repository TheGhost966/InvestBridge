package com.platform.desktop.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.platform.desktop.api.dto.ApiError;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
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

    /** Tight: if the dispatcher socket isn't accepting, fail quickly. */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    /**
     * Loose: BCrypt + JDBC audit on auth, plus cold-cache MongoDB queries on
     * other services, can push individual responses past 10–15 seconds on
     * Windows/WSL2 — especially after a container restart. 30s gives us slack
     * without making "real" outages take forever to surface.
     */
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final String baseUrl;
    private final HttpClient http;
    private final ObjectMapper mapper;

    public ApiClient() {
        this(DEFAULT_BASE_URL);
    }

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.http = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
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

    // ─── parameterised-type variants (for ApiResponse<PagedResult<Idea>> etc.) ─

    public <T> T get(String path, TypeReference<T> typeRef) {
        return send("GET", path, null, typeRef);
    }

    public <T> T post(String path, Object body, TypeReference<T> typeRef) {
        return send("POST", path, body, typeRef);
    }

    // ─── core send ──────────────────────────────────────────────────────────────

    /**
     * @param responseType {@code Void.class} to ignore the response body
     *                     (also used for empty 204 responses).
     */
    public <T> T send(String method, String path, Object body, Class<T> responseType) {
        return sendInternal(method, path, body,
                payload -> mapper.readValue(payload, responseType),
                responseType == Void.class);
    }

    /** Use when {@code T} is a parameterised type, e.g. {@code ApiResponse<PagedResult<Idea>>}. */
    public <T> T send(String method, String path, Object body, TypeReference<T> typeRef) {
        return sendInternal(method, path, body,
                payload -> mapper.readValue(payload, typeRef),
                false);
    }

    @FunctionalInterface
    private interface PayloadParser<T> {
        T parse(String payload) throws IOException;
    }

    private <T> T sendInternal(String method,
                               String path,
                               Object body,
                               PayloadParser<T> parser,
                               boolean voidExpected) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(REQUEST_TIMEOUT)
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
        } catch (HttpTimeoutException e) {
            // Connected, but the response did not arrive within REQUEST_TIMEOUT.
            // Surface it distinctly so the user knows the server is reachable but slow.
            throw new ApiException("The server is taking too long to respond ("
                    + REQUEST_TIMEOUT.toSeconds() + "s timeout). Please try again.", e);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ApiException("Cannot reach " + baseUrl + path
                    + " — is the dispatcher running?", e);
        }

        int status = response.statusCode();
        String payload = response.body();

        if (status >= 200 && status < 300) {
            if (voidExpected || payload == null || payload.isBlank()) {
                return null;
            }
            try {
                return parser.parse(payload);
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
