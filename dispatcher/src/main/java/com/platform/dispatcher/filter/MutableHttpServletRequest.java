package com.platform.dispatcher.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.*;

/**
 * MutableHttpServletRequest
 *
 * HTTP requests are immutable by default — you can't add headers.
 * This wrapper lets us ADD headers before forwarding to internal services.
 *
 * Used by JwtAuthFilter to add:
 *   X-Internal-Request: true    ← proves request came through Dispatcher
 *   X-User-Id: <userId>         ← so services know who is calling
 *   X-User-Role: <role>         ← so services can do role checks locally
 *   X-Correlation-ID: <uuid>    ← for distributed log tracing
 */
public class MutableHttpServletRequest extends HttpServletRequestWrapper {

    private final Map<String, String> customHeaders = new HashMap<>();

    public MutableHttpServletRequest(HttpServletRequest request) {
        super(request);
    }

    public void putHeader(String name, String value) {
        customHeaders.put(name, value);
    }

    @Override
    public String getHeader(String name) {
        // Check our custom headers first
        if (customHeaders.containsKey(name)) {
            return customHeaders.get(name);
        }
        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (customHeaders.containsKey(name)) {
            return Collections.enumeration(List.of(customHeaders.get(name)));
        }
        return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> names = new HashSet<>(customHeaders.keySet());
        Enumeration<String> original = super.getHeaderNames();
        while (original.hasMoreElements()) {
            names.add(original.nextElement());
        }
        return Collections.enumeration(names);
    }
}