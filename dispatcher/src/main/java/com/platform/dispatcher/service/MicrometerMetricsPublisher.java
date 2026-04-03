package com.platform.dispatcher.service;

import com.platform.dispatcher.interfaces.MetricsPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Publishes per-route request metrics to Prometheus via Micrometer.
 *
 * Metrics emitted:
 *   dispatcher_requests_total{route, method, status}  — counter
 *   dispatcher_request_duration_ms{route, method}     — timer (histogram)
 */
@Service
public class MicrometerMetricsPublisher implements MetricsPublisher {

    private final MeterRegistry registry;

    public MicrometerMetricsPublisher(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void recordRequest(String route, String method, int status, long durationMs) {
        String statusGroup = status / 100 + "xx"; // e.g. "2xx", "4xx", "5xx"

        registry.counter("dispatcher_requests_total",
                "route",  route,
                "method", method,
                "status", String.valueOf(status),
                "status_group", statusGroup
        ).increment();

        Timer.builder("dispatcher_request_duration_ms")
                .tag("route",  route)
                .tag("method", method)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }
}
