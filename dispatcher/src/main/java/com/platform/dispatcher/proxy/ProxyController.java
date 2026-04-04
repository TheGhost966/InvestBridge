package com.platform.dispatcher.proxy;

import com.platform.dispatcher.interfaces.MetricsPublisher;
import com.platform.dispatcher.interfaces.RouteResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

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
 * The JwtAuthFilter has already validated the JWT before this controller runs.
 * Route resolution is delegated to RouteResolver (DIP).
 * Metrics are published via MetricsPublisher (DIP).
 */
@RestController
public class ProxyController {

    private static final Logger log = LoggerFactory.getLogger(ProxyController.class);

    // Hop-by-hop headers must not be forwarded to downstream services
    private static final Set<String> HOP_BY_HOP = Set.of(
            "transfer-encoding", "connection", "keep-alive",
            "proxy-authenticate", "proxy-authorization",
            "te", "trailers", "upgrade", "content-length"
    );

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_BACKOFF_MS = 200;

    private final RouteResolver routeResolver;
    private final MetricsPublisher metricsPublisher;
    private final RestClient restClient;

    public ProxyController(RouteResolver routeResolver, MetricsPublisher metricsPublisher) {
        this.routeResolver    = routeResolver;
        this.metricsPublisher = metricsPublisher;
        this.restClient       = RestClient.create();
    }

    /** Catch-all mapping — handles ALL paths. */
    @RequestMapping("/**")
    public ResponseEntity<byte[]> proxy(HttpServletRequest request) throws IOException {

        String path       = request.getRequestURI();
        String targetBase = routeResolver.resolve(path);

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

        // Copy inbound headers, skipping hop-by-hop headers that must not be forwarded
        HttpHeaders headers = new HttpHeaders();
        Collections.list(request.getHeaderNames()).forEach(name -> {
            if (!HOP_BY_HOP.contains(name.toLowerCase())) {
                headers.put(name, Collections.list(request.getHeaders(name)));
            }
        });

        // Mark request as internal so downstream InternalRequestFilters let it through
        headers.set("X-Internal-Request", "true");

        // Inject authenticated user identity so downstream services can enforce ownership
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof String userId) {
            headers.set("X-User-Id",   userId);
            headers.set("X-User-Role", auth.getAuthorities().stream()
                    .findFirst()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .orElse(""));
        }

        byte[] body  = request.getInputStream().readAllBytes();
        String route = extractRouteLabel(path);
        long   start = System.currentTimeMillis();

        ResponseEntity<byte[]> response = forwardWithRetry(
                request.getMethod(), targetUrl, headers, body, targetBase, MAX_RETRIES);

        long duration = System.currentTimeMillis() - start;
        metricsPublisher.recordRequest(route, request.getMethod(), response.getStatusCode().value(), duration);

        return response;
    }

    /**
     * Forward the request, retrying up to {@code retriesLeft} times on network failure.
     * Each retry waits RETRY_BACKOFF_MS × attempt before retrying (linear backoff).
     *
     * 4xx / 5xx responses from downstream services are forwarded as-is — the
     * dispatcher must never swallow them and replace with a generic Bad Gateway.
     */
    private ResponseEntity<byte[]> forwardWithRetry(
            String method, String targetUrl, HttpHeaders headers, byte[] body,
            String targetBase, int retriesLeft) {

        try {
            return restClient
                    .method(HttpMethod.valueOf(method))
                    .uri(targetUrl)
                    .headers(h -> h.addAll(headers))
                    .body(body)
                    .retrieve()
                    .toEntity(byte[].class);

        } catch (HttpStatusCodeException e) {
            // Downstream returned 4xx or 5xx — forward it transparently
            log.debug("Downstream {} for {}", e.getStatusCode(), targetUrl);
            return ResponseEntity.status(e.getStatusCode())
                    .headers(e.getResponseHeaders())
                    .body(e.getResponseBodyAsByteArray());

        } catch (ResourceAccessException e) {
            if (retriesLeft > 0) {
                int attempt = MAX_RETRIES - retriesLeft + 1;
                log.warn("Service unavailable (attempt {}): {} — retrying in {}ms",
                        attempt, targetUrl, RETRY_BACKOFF_MS * attempt);
                try {
                    Thread.sleep(RETRY_BACKOFF_MS * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                return forwardWithRetry(method, targetUrl, headers, body, targetBase, retriesLeft - 1);
            }
            log.error("Service unavailable after {} retries: {}", MAX_RETRIES, targetUrl);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(("Service unavailable: " + targetBase).getBytes());

        } catch (Exception e) {
            log.error("Proxy error for {}: {}", targetUrl, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Bad gateway".getBytes());
        }
    }

    /** Derive a short route label from the path for use in metrics tags. */
    private String extractRouteLabel(String path) {
        if (path.startsWith("/auth"))  return "auth";
        if (path.startsWith("/ideas")) return "ideas";
        if (path.startsWith("/deals")) return "deals";
        if (path.startsWith("/ai"))    return "ai";
        return "unknown";
    }
}
