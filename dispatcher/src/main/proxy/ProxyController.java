package com.platform.dispatcher.proxy;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * ProxyController — forwards every incoming request to the correct
 * internal service based on the URL prefix.
 *
 * Routing table:
 *   /auth/**  → auth-service:8081
 *   /ideas/** → idea-service:8082
 *   /deals/** → deal-service:8083
 *   /ai/**    → ai-service:8084
 *
 * Everything else → 404
 *
 * The JwtAuthFilter has already validated the JWT and added
 * X-Internal-Request, X-User-Id, X-User-Role headers before
 * this controller runs.
 */
@RestController
@Slf4j
public class ProxyController {

    @Value("${routes.auth-service}")
    private String authServiceUrl;

    @Value("${routes.idea-service}")
    private String ideaServiceUrl;

    @Value("${routes.deal-service}")
    private String dealServiceUrl;

    @Value("${routes.ai-service}")
    private String aiServiceUrl;

    private final RestClient restClient;

    public ProxyController() {
        this.restClient = RestClient.create();
    }

    /**
     * Catch-all mapping — handles ALL paths.
     * We determine the target service from the URL prefix.
     */
    @RequestMapping("/**")
    public ResponseEntity<byte[]> proxy(HttpServletRequest request) throws IOException {

        String path = request.getRequestURI();
        String targetBase = resolveTargetService(path);

        // Unknown route → 404
        if (targetBase == null) {
            log.warn("No route found for path: {}", path);
            return ResponseEntity.notFound().build();
        }

        String targetUrl = targetBase + path;
        String queryString = request.getQueryString();
        if (queryString != null) {
            targetUrl += "?" + queryString;
        }

        log.debug("Proxying {} {} → {}", request.getMethod(), path, targetUrl);

        // Copy all headers from the incoming request
        HttpHeaders headers = new HttpHeaders();
        Collections.list(request.getHeaderNames()).forEach(headerName ->
            headers.put(headerName, Collections.list(request.getHeaders(headerName)))
        );

        // Read request body
        byte[] body = request.getInputStream().readAllBytes();

        try {
            // Forward the request to the internal service
            ResponseEntity<byte[]> response = restClient
                    .method(HttpMethod.valueOf(request.getMethod()))
                    .uri(targetUrl)
                    .headers(h -> h.addAll(headers))
                    .body(body)
                    .retrieve()
                    .toEntity(byte[].class);

            return response;

        } catch (ResourceAccessException e) {
            // Service is down → 503 Service Unavailable
            log.error("Service unavailable: {} | error: {}", targetUrl, e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(("Service unavailable: " + targetBase).getBytes());
        } catch (Exception e) {
            log.error("Proxy error for {}: {}", targetUrl, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Bad gateway".getBytes());
        }
    }

    /**
     * Resolve which internal service to forward to based on URL prefix.
     * Returns null if no matching service is found → caller returns 404.
     */
    private String resolveTargetService(String path) {
        if (path.startsWith("/auth/"))  return authServiceUrl;
        if (path.startsWith("/ideas/") || path.equals("/ideas")) return ideaServiceUrl;
        if (path.startsWith("/deals/") || path.equals("/deals")) return dealServiceUrl;
        if (path.startsWith("/ai/"))    return aiServiceUrl;
        return null; // Unknown route → 404
    }
}