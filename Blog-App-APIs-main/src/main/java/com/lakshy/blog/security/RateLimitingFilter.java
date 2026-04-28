package com.lakshy.blog.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lakshy.blog.payloads.ErrorResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory rate limiter for authentication endpoints.
 * Allows 10 requests per IP per minute on /api/auth/login and /api/auth/register.
 * SINGLE-NODE ONLY — replace bucket store with distributed cache for clustered deployments.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int CAPACITY = 10;
    private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);
    private static final String[] RATE_LIMITED_PATHS = {
        "/api/auth/login",
        "/api/auth/register"
    };

    @Autowired
    private ObjectMapper objectMapper;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        for (String limited : RATE_LIMITED_PATHS) {
            if (path.equals(limited)) return false;
        }
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String clientIp = getClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(clientIp, this::newBucket);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "Too Many Requests",
                "Rate limit exceeded. Try again in 1 minute.",
                request.getRequestURI(),
                null);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(objectMapper.writeValueAsString(error));
        }
    }

    private Bucket newBucket(String key) {
        Bandwidth limit = Bandwidth.classic(CAPACITY, Refill.greedy(CAPACITY, REFILL_PERIOD));
        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIp(HttpServletRequest request) {
        // Use the direct TCP connection address only.
        // X-Forwarded-For is NOT trusted here: an attacker can set it to any value
        // and bypass per-IP rate limiting entirely. Enforce HTTPS + trusted-proxy
        // IP extraction at the reverse-proxy (nginx/ALB) level instead.
        return request.getRemoteAddr();
    }
}
