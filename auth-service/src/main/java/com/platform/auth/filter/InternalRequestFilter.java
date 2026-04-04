package com.platform.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rejects any request that does not carry the X-Internal-Request: true header.
 * This ensures auth-service is only reachable through the Dispatcher —
 * direct external calls are blocked with 403 Forbidden.
 */
public class InternalRequestFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Internal-Request";
    private static final String EXPECTED = "true";

    /** Prometheus scrapes /actuator/prometheus — let it through without the internal header. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String headerValue = request.getHeader(HEADER);
        if (!EXPECTED.equalsIgnoreCase(headerValue)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Direct access not allowed. Route through the Dispatcher.");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
