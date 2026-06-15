package com.devtinder.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final int requestsPerMinute;
    private final Map<String, RequestWindow> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(@Value("${app.rate-limit.requests-per-minute:60}") int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (requestsPerMinute <= 0) {
            filterChain.doFilter(request, response);
            return;
        }

        RequestWindow window = windows.compute(clientKey(request), (key, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null || existing.isExpired(now)) {
                return new RequestWindow(now);
            }
            existing.increment();
            return existing;
        });

        int usedRequests = window.count();
        int remaining = Math.max(0, requestsPerMinute - usedRequests);
        long resetEpochSeconds = window.resetEpochSeconds();

        response.setHeader("X-RateLimit-Limit", String.valueOf(requestsPerMinute));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Reset", String.valueOf(resetEpochSeconds));

        if (usedRequests > requestsPerMinute) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("""
                    {"error":"Too Many Requests","message":"Rate limit exceeded. Try again later."}
                    """);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static final class RequestWindow {
        private final long startedAtMillis;
        private final AtomicInteger count = new AtomicInteger(1);

        private RequestWindow(long startedAtMillis) {
            this.startedAtMillis = startedAtMillis;
        }

        private void increment() {
            count.incrementAndGet();
        }

        private int count() {
            return count.get();
        }

        private boolean isExpired(long nowMillis) {
            return nowMillis - startedAtMillis >= WINDOW.toMillis();
        }

        private long resetEpochSeconds() {
            return Instant.ofEpochMilli(startedAtMillis)
                    .plus(WINDOW)
                    .getEpochSecond();
        }
    }
}
