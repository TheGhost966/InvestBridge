package com.platform.deal.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String HEADER  = "X-Correlation-Id";
    private static final String MDC_KEY = "correlation_id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String correlationId = request.getHeader(HEADER);
        if (correlationId != null && !correlationId.isBlank()) {
            MDC.put(MDC_KEY, correlationId);
        }
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
