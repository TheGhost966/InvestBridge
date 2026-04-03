package com.platform.dispatcher.security;

import com.platform.dispatcher.JwtUtil;
import com.platform.dispatcher.interfaces.TokenValidator;
import org.springframework.stereotype.Component;

/**
 * Concrete implementation of TokenValidator backed by JwtUtil.
 * This is the only class that knows about the jjwt library internals.
 */
@Component
public class JwtTokenValidator implements TokenValidator {

    private final JwtUtil jwtUtil;

    public JwtTokenValidator(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public String extractUserId(String token) {
        return jwtUtil.extractUserId(token);
    }

    @Override
    public String extractRole(String token) {
        return jwtUtil.extractRole(token);
    }

    @Override
    public boolean isValid(String token) {
        return jwtUtil.isValid(token);
    }
}
