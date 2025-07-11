package com.emailservice.emailservice.util;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks processed requestIds to prevent duplicate email sending.
 */
@Component
public class IdempotencyChecker {

    // In-memory store of processed request IDs
    private final Set<String> processedIds = ConcurrentHashMap.newKeySet();

    /**
     * Check if this request has already been processed.
     */
    public boolean isDuplicate(String requestId) {
        return processedIds.contains(requestId);
    }

    /**
     * Mark this request ID as processed.
     */
    public void markSent(String requestId) {
        processedIds.add(requestId);
    }
}