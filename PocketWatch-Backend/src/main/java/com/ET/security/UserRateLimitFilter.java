package com.ET.security;

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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter for authenticated, resource-intensive endpoints.
 *
 * Unlike RateLimitFilter, which limits requests using the client IP,
 * this filter identifies clients using their authenticated user identity.
 *
 * This is useful for endpoints such as AI chat and file uploads where
 * the primary concern is protecting application resources and external
 * service costs on a per-user basis.
 *
 * This filter must execute after JwtAuthenticationFilter so that the
 * SecurityContext contains the authenticated user's identity.
 */
@Slf4j
@Component
public class UserRateLimitFilter extends OncePerRequestFilter {

    /**
     * Defines rate limits for authenticated, resource-intensive endpoints.
     *
     * AI chat requests can directly consume an external LLM API quota,
     * while uploads can consume significant CPU, memory, storage,
     * and potentially AI-processing resources.
     */
    private static final Map<String, Bandwidth> LIMITED_PATHS = Map.of(

            // Limits the number of AI requests made by a single user.
            "/api/v1/ai/chat", limit(20, Duration.ofMinutes(1)),

            // Limits repeated statement uploads and protects processing resources.
            "/api/v1/uploads", limit(5, Duration.ofMinutes(1))
    );

    /**
     * Stores one token bucket for each user + endpoint combination.
     *
     * Example:
     *     user@example.com:/api/v1/ai/chat
     *
     * A separate bucket is maintained for each endpoint so that usage
     * on one endpoint does not consume the user's allowance on another.
     */
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Allows rate limiting to be disabled through application configuration.
     *
     * This is particularly useful during automated integration tests.
     */
    @Value("${app.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    /**
     * Creates a Bucket4j bandwidth configuration.
     *
     * @param capacity maximum number of requests allowed
     * @param window   refill period
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
         * Skip the filter when rate limiting has been disabled
         * through application configuration.
         */
        if (!rateLimitEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        /*
         * Check whether the current endpoint has a configured
         * authenticated-user rate limit.
         */
        Bandwidth limitForPath = LIMITED_PATHS.get(request.getRequestURI());

        if (limitForPath == null) {
            filterChain.doFilter(request, response);
            return;
        }

        /*
         * Resolve the authenticated user's identity.
         *
         * JwtAuthenticationFilter should have already populated the
         * SecurityContext before this filter executes.
         */
        String userKey = resolveUserKey(request);

        /*
         * Create a separate bucket for each user and endpoint.
         */
        String bucketKey = userKey + ":" + request.getRequestURI();

        /*
         * Create the bucket only when this user/endpoint combination
         * is encountered for the first time.
         */
        Bucket bucket = buckets.computeIfAbsent(
                bucketKey,
                key -> Bucket.builder()
                        .addLimit(limitForPath)
                        .build()
        );

        /*
         * Consume one token for the current request.
         *
         * If no token is available, the user has exceeded the configured
         * rate limit and the request is rejected with HTTP 429.
         */
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn(
                "Rate limit exceeded for user {} on {}",
                userKey,
                request.getRequestURI()
        );

        RateLimitFilter.rejectWithTooManyRequests(response);
    }

    /**
     * Resolves the key used for rate limiting.
     *
     * Normally this is the authenticated user's identity obtained from
     * SecurityContextHolder.
     *
     * An IP address is used as a defensive fallback if authentication
     * information is unexpectedly unavailable.
     */
    private String resolveUserKey(HttpServletRequest request) {

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth != null
                && auth.isAuthenticated()
                && auth.getName() != null) {

            return auth.getName();
        }

        return RateLimitFilter.resolveClientIp(request);
    }
}