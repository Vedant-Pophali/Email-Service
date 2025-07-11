package com.emailservice.emailservice.util;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple rate limiter: allows N requests per T seconds.
 */
@Component
public class RateLimiter {

    private static final int MAX_REQUESTS = 5;            // Limit
    private static final int INTERVAL_SECONDS = 10;       // Window

    private AtomicInteger requestCount = new AtomicInteger(0);
    private Instant windowStart = Instant.now();

    /**
     * Allow the request if under the rate limit.
     */
    public synchronized boolean allow() {
        Instant now = Instant.now();
        if (now.isAfter(windowStart.plusSeconds(INTERVAL_SECONDS))) {
            // Reset window
            windowStart = now;
            requestCount.set(0);
        }

        if (requestCount.get() < MAX_REQUESTS) {
            requestCount.incrementAndGet();
            return true;
        } else {
            return false;
        }
    }
}