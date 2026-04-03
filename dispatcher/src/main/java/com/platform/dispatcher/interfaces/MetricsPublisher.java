package com.platform.dispatcher.interfaces;

/**
 * Abstraction for publishing request metrics.
 * Implementations may use Micrometer, a custom counter, or a no-op for tests.
 */
public interface MetricsPublisher {

    /**
     * Record a completed request.
     *
     * @param route      the matched route prefix (e.g. "auth", "ideas", "deals")
     * @param method     HTTP method (e.g. "GET", "POST")
     * @param status     HTTP response status code
     * @param durationMs wall-clock time from request receipt to response send, in ms
     */
    void recordRequest(String route, String method, int status, long durationMs);
}
