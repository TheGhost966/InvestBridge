package com.platform.dispatcher;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.*;

/**
 * Wraps an HttpServletRequest so we can remove or add headers before
 * forwarding the request to a downstream service.
 *
 * Usage:
 *   StripHeaderRequestWrapper wrapped = new StripHeaderRequestWrapper(request);
 *   wrapped.removeHeader("Authorization");   // strip inbound JWT before proxying
 *   wrapped.addHeader("X-Internal-Request", "true");
 */
public class StripHeaderRequestWrapper extends HttpServletRequestWrapper {

    private final Map<String, String> customHeaders = new HashMap<>();
    private final Set<String> removedHeaders       = new HashSet<>();

    public StripHeaderRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    /** Mark a header for removal. Case-insensitive. */
    public void removeHeader(String name) {
        removedHeaders.add(name.toLowerCase(Locale.ROOT));
    }

    /** Add or override a header value. */
    public void addHeader(String name, String value) {
        customHeaders.put(name.toLowerCase(Locale.ROOT), value);
    }

    @Override
    public String getHeader(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (removedHeaders.contains(lower)) return null;
        if (customHeaders.containsKey(lower))  return customHeaders.get(lower);
        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (removedHeaders.contains(lower)) return Collections.emptyEnumeration();
        if (customHeaders.containsKey(lower))
            return Collections.enumeration(List.of(customHeaders.get(lower)));
        return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> names = new HashSet<>();

        // Start with original header names
        Enumeration<String> original = super.getHeaderNames();
        while (original != null && original.hasMoreElements()) {
            String h = original.nextElement().toLowerCase(Locale.ROOT);
            if (!removedHeaders.contains(h)) names.add(h);
        }

        // Add our custom headers
        names.addAll(customHeaders.keySet());
        return Collections.enumeration(names);
    }
}