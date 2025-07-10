package com.emailservice.emailservice.service;

import com.emailservice.emailservice.model.EmailRequest;
import com.emailservice.emailservice.model.EmailStatus;
import com.emailservice.emailservice.provide.EmailProvider;
import com.emailservice.emailservice.util.IdempotencyChecker;
import com.emailservice.emailservice.util.RateLimiter;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements the resilient email sending logic.
 */
@Service
public class EmailServiceImpl implements EmailService {

    private final List<EmailProvider> providers;
    private final IdempotencyChecker idempotencyChecker;
    private final RateLimiter rateLimiter;

    // Stores statuses by requestId
    private final Map<String, EmailStatus> statusStore = new ConcurrentHashMap<>();

    public EmailServiceImpl(List<EmailProvider> providers,
                            IdempotencyChecker idempotencyChecker,
                            RateLimiter rateLimiter) {
        this.providers = providers;
        this.idempotencyChecker = idempotencyChecker;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public EmailStatus sendEmail(EmailRequest request) {
        String requestId = request.getRequestId();

        // ✅ 1. Idempotency check
        if (idempotencyChecker.isDuplicate(requestId)) {
            EmailStatus existingStatus = statusStore.get(requestId);
            return new EmailStatus(
                    existingStatus.getRequestId(),
                    existingStatus.getStatus(),
                    existingStatus.getProviderUsed(),
                    existingStatus.getAttempts(),
                    "⚠️ Already processed (idempotent). No new email sent.",
                    existingStatus.getTimestamp()
            );
        }

        // ✅ 2. Rate limiting
        if (!rateLimiter.allow()) {
            EmailStatus rateLimitedStatus = new EmailStatus(
                    requestId,
                    "RATE_LIMITED",
                    null,
                    0,
                    "Rate limit exceeded",
                    LocalDateTime.now()
            );
            statusStore.put(requestId, rateLimitedStatus);
            return rateLimitedStatus;
        }

        // ✅ 3. Attempt sending email with retries and fallback
        int maxRetries = 3;
        long baseDelayMs = 500;
        int totalAttempts = 0;
        String lastProviderTried = null;

        for (EmailProvider provider : providers) {
            lastProviderTried = provider.getName();

            for (int i = 0; i < maxRetries; i++) {
                totalAttempts++;
                boolean success = provider.send(request);

                if (success) {
                    EmailStatus successStatus = new EmailStatus(
                            requestId,
                            "SENT",
                            provider.getName(),
                            totalAttempts,
                            "Email sent successfully",
                            LocalDateTime.now()
                    );
                    statusStore.put(requestId, successStatus);
                    idempotencyChecker.markSent(requestId);
                    return successStatus;
                }

                // Exponential backoff between retries
                try {
                    Thread.sleep((long) (baseDelayMs * Math.pow(2, i)));
                } catch (InterruptedException ignored) {}
            }
        }

        // ✅ 4. All providers failed
        EmailStatus failureStatus = new EmailStatus(
                requestId,
                "FAILED",
                lastProviderTried, // ✅ Fix applied here
                totalAttempts,
                "All providers failed",
                LocalDateTime.now()
        );
        statusStore.put(requestId, failureStatus);
        idempotencyChecker.markSent(requestId);
        return failureStatus;
    }

    @Override
    public EmailStatus getStatusByRequestId(String requestId) {
        return statusStore.get(requestId);
    }
}