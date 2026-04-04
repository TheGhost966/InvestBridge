package com.platform.dispatcher.util;

/**
 * Utility for extracting the route prefix from an incoming request path.
 *
 * Examples:
 *   "/auth/register"   → "auth"
 *   "/ideas"           → "ideas"
 *   "/deals/offers/1"  → "deals"
 *   "/"                → ""
 */
public final class PathMatcher {

    private PathMatcher() {}

    /**
     * Returns the first path segment (the route prefix) of the given URI path.
     * Leading slash is stripped before splitting.
     *
     * @param path the full request URI (e.g. "/ideas/123?page=0")
     * @return the first segment (e.g. "ideas"), or an empty string if the path is root
     */
    public static String extractPrefix(String path) {
        if (path == null || path.isEmpty() || path.equals("/")) {
            return "";
        }
        // Strip leading slash, then take everything up to the next slash (or end)
        String stripped = path.startsWith("/") ? path.substring(1) : path;
        int nextSlash = stripped.indexOf('/');
        return nextSlash == -1 ? stripped : stripped.substring(0, nextSlash);
    }
}
