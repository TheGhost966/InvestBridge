package com.platform.dispatcher.interfaces;

/**
 * Abstraction for resolving an incoming URL path to an internal service base URL.
 * Depend on this interface in ProxyController — not on raw @Value strings — to satisfy DIP.
 */
public interface RouteResolver {

    /**
     * Returns the base URL of the internal service that should handle this path,
     * or {@code null} if no matching route exists (caller should return 404).
     *
     * @param path the full request URI (e.g. "/ideas/123")
     * @return base URL such as "http://idea-service:8082", or null
     */
    String resolve(String path);
}
