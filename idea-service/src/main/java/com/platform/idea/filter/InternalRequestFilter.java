package com.platform.idea.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rejects any request missing X-Internal-Request: true.
 * Ensures idea-service is only reachable through the Dispatcher.
 */
public class InternalRequestFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        if (!"true".equalsIgnoreCase(request.getHeader("X-Internal-Request"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Direct access not allowed.");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
