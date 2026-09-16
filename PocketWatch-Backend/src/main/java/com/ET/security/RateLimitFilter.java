package com.ET.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP-based rate limiter for public authentication endpoints.
 *
 * These endpoints are accessible without authentication, so the client IP
 * address is used as the rate-limiting key.
 *
 * The filter protects endpoints that are commonly targeted by automated
 * attacks or request spam, such as login attempts, password reset requests,
 * and verification-email requests.
 *
 * This filter is executed before JwtAuthenticationFilter because the
 * endpoints being protected do not require a valid JWT.
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    /**
     * Defines rate limits for specific public authentication endpoints.
     *
     * The map key is the request path and the value defines the maximum
     * number of requests allowed during the configured time window.
     *
     * Each endpoint has a separate limit because different authentication
     * operations have different security and resource requirements.
     */
    private static final Map<String, Bandwidth> LIMITED_PATHS = Map.of(

            // Login: protects against password brute-force attacks.
            "/api/v1/auth/login", limit(5, Duration.ofMinutes(1)),

            // Password reset and verification endpoints can trigger emails,
            // so stricter limits help prevent email/request abuse.
            "/api/v1/auth/forgot-password", limit(3, Duration.ofMinutes(10)),
            "/api/v1/auth/resend-verification", limit(3, Duration.ofMinutes(10)),

            // Registration and password-reset requests are also limited
            // to reduce automated request spam and token abuse.
            "/api/v1/auth/register", limit(5, Duration.ofMinutes(10)),
            "/api/v1/auth/reset-password", limit(5, Duration.ofMinutes(10))
    );

    /**
     * Stores one token bucket for each combination of client IP and endpoint.
     *
     * Example:
     *     192.168.1.10:/api/v1/auth/login
     *
     * Using the IP + endpoint combination ensures that limits for different
     * authentication operations are independent of each other.
     */
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Allows rate limiting to be disabled through application configuration.
     *
     * This is useful for automated tests where multiple test cases may call
     * the same authentication endpoint and unintentionally consume the
     * shared rate-limit bucket.
     */
    @Value("${app.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    /**
     * Creates a Bucket4j bandwidth configuration.
     *
     * @param capacity maximum number of requests allowed in the window
     * @param window   duration after which the tokens are refilled
     * @return configured bandwidth limit
     */
    private static Bandwidth limit(int capacity, Duration window) {
        return Bandwidth.classic(
                capacity,
                Refill.greedy(capacity, window)
        );
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        /*
         * Skip rate limiting when disabled through configuration.
         */
        if (!rateLimitEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        /*
         * Only apply rate limiting to endpoints explicitly listed in
         * LIMITED_PATHS. All other requests continue normally.
         */
        Bandwidth limitForPath = LIMITED_PATHS.get(request.getRequestURI());

        if (limitForPath == null) {
            filterChain.doFilter(request, response);
            return;
        }

        /*
         * Resolve the original client IP. This is important when the
         * application is running behind a reverse proxy or load balancer.
         */
        String clientIp = resolveClientIp(request);

        /*
         * Create a unique bucket for each IP + endpoint combination.
         *
         * This prevents requests to one endpoint from consuming the
         * rate-limit allowance of another endpoint.
         */
        String bucketKey = clientIp + ":" + request.getRequestURI();

        /*
         * Create the bucket lazily when the IP/endoint combination is
         * encountered for the first time.
         *
         * ConcurrentHashMap makes this safe when multiple requests arrive
         * simultaneously.
         */
        Bucket bucket = buckets.computeIfAbsent(
                bucketKey,
                key -> Bucket.builder()
                        .addLimit(limitForPath)
                        .build()
        );

        /*
         * Attempt to consume one token from the bucket.
         *
         * If a token is available, the request is allowed to continue.
         * Otherwise, the request has exceeded the configured rate limit.
         */
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn(
                "Rate limit exceeded for {} on {}",
                clientIp,
                request.getRequestURI()
        );

        rejectWithTooManyRequests(response);
    }

    /**
     * Sends a standard HTTP 429 response when the client exceeds
     * the configured rate limit.
     *
     * HTTP 429 indicates that the client has sent too many requests
     * within the allowed time period.
     */
    static void rejectWithTooManyRequests(
            HttpServletResponse response
    ) throws IOException {

        response.setStatus(429);
        response.setContentType("application/json");

        new ObjectMapper().writeValue(
                response.getWriter(),
                Map.of(
                        "success", false,
                        "message",
                        "Too many attempts. Please wait a moment and try again."
                )
        );
    }

    /**
     * Resolves the client's IP address.
     *
     * When the application is deployed behind a reverse proxy or load
     * balancer, getRemoteAddr() may return the proxy's IP instead of
     * the actual client IP.
     *
     * X-Forwarded-For is therefore checked first. When multiple IPs are
     * present, the first IP represents the original client in the usual
     * proxy configuration.
     */
    static String resolveClientIp(HttpServletRequest request) {

        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}