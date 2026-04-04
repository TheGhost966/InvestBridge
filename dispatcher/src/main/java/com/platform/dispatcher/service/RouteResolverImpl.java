package com.platform.dispatcher.service;

import com.platform.dispatcher.interfaces.RouteResolver;
import com.platform.dispatcher.util.PathMatcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Resolves an incoming URL path to the base URL of the matching internal service.
 * Route table is configured via application.yml — no hardcoded URLs here.
 * Prefix extraction is delegated to {@link PathMatcher} (single responsibility).
 */
@Service
public class RouteResolverImpl implements RouteResolver {

    private final String authServiceUrl;
    private final String ideaServiceUrl;
    private final String dealServiceUrl;
    private final String aiServiceUrl;

    public RouteResolverImpl(
            @Value("${routes.auth-service}") String authServiceUrl,
            @Value("${routes.idea-service}") String ideaServiceUrl,
            @Value("${routes.deal-service}") String dealServiceUrl,
            @Value("${routes.ai-service}")   String aiServiceUrl) {
        this.authServiceUrl = authServiceUrl;
        this.ideaServiceUrl = ideaServiceUrl;
        this.dealServiceUrl = dealServiceUrl;
        this.aiServiceUrl   = aiServiceUrl;
    }

    @Override
    public String resolve(String path) {
        return switch (PathMatcher.extractPrefix(path)) {
            case "auth"  -> authServiceUrl;
            case "ideas" -> ideaServiceUrl;
            case "deals" -> dealServiceUrl;
            case "ai"    -> aiServiceUrl;
            default      -> null; // no matching route → caller returns 404
        };
    }
}
