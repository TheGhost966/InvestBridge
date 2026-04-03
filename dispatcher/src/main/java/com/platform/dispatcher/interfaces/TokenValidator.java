package com.platform.dispatcher.interfaces;

/**
 * Abstraction for JWT token validation.
 * Depend on this interface — not on JwtUtil directly — to satisfy DIP.
 */
public interface TokenValidator {

    /** Extract the user ID (subject) from a valid token. Throws on invalid/expired. */
    String extractUserId(String token);

    /** Extract the role claim from a valid token. Throws on invalid/expired. */
    String extractRole(String token);

    /** Returns true only if the token is well-formed, signed correctly, and not expired. */
    boolean isValid(String token);
}
