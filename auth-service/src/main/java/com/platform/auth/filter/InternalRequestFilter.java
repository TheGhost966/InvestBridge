package com.platform.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * STUB — does not enforce X-Internal-Request header yet.
 * Tests expecting 403 on missing header will FAIL (RED phase).
 */
public class InternalRequestFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        // STUB: no enforcement — GREEN phase will add the check
        filterChain.doFilter(request, response);
    }
}
