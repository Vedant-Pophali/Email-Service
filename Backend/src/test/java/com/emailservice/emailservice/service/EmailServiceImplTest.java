package com.emailservice.emailservice.service;
import com.emailservice.emailservice.model.EmailRequest;
import com.emailservice.emailservice.model.EmailStatus;
import com.emailservice.emailservice.provide.EmailProvider;
import com.emailservice.emailservice.util.IdempotencyChecker;
import com.emailservice.emailservice.util.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailServiceImplTest {

    private EmailProvider mockProvider;
    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        // Mocks
        mockProvider = mock(EmailProvider.class);
        IdempotencyChecker idempotencyChecker = new IdempotencyChecker();
        RateLimiter rateLimiter = mock(RateLimiter.class);

        // Always allow in this test
        when(rateLimiter.allow()).thenReturn(true);

        // Use single mock provider
        emailService = new EmailServiceImpl(
                Collections.singletonList(mockProvider),
                idempotencyChecker,
                rateLimiter
        );
    }

    @Test
    void testSuccessfulSendViaFirstProvider() {
        // Arrange
        EmailRequest request = new EmailRequest("test-id-1", "user@example.com", "Hello", "Test Body");
        when(mockProvider.send(request)).thenReturn(true);
        when(mockProvider.getName()).thenReturn("MockProvider1");

        // Act
        EmailStatus status = emailService.sendEmail(request);

        // Assert
        assertEquals("SENT", status.getStatus());
        assertEquals("MockProvider1", status.getProviderUsed());
        assertEquals(1, status.getAttempts());
        assertEquals("Email sent successfully", status.getMessage());
    }
    @Test
    void testFallbackToSecondProvider() {
        // Arrange
        EmailRequest request = new EmailRequest("test-id-2", "fail@example.com", "Fallback Test", "Try fallback");

        // Mock provider 1: fails all attempts
        EmailProvider provider1 = mock(EmailProvider.class);
        when(provider1.send(request)).thenReturn(false);
        when(provider1.getName()).thenReturn("MockProvider1");

        // Mock provider 2: succeeds
        EmailProvider provider2 = mock(EmailProvider.class);
        when(provider2.send(request)).thenReturn(true);
        when(provider2.getName()).thenReturn("MockProvider2");

        // Fix: use real mock RateLimiter instead of lambda
        RateLimiter mockLimiter = mock(RateLimiter.class);
        when(mockLimiter.allow()).thenReturn(true);

        // Setup service with both providers
        EmailServiceImpl service = new EmailServiceImpl(
                List.of(provider1, provider2),
                new IdempotencyChecker(),
                mockLimiter
        );

        // Act
        EmailStatus status = service.sendEmail(request);

        // Assert
        assertEquals("SENT", status.getStatus());
        assertEquals("MockProvider2", status.getProviderUsed());
        assertTrue(status.getAttempts() >= 4); // 3 from provider1, 1 from provider2
        assertEquals("Email sent successfully", status.getMessage());
    }
    @Test
    void testIdempotencyAvoidsDuplicateSend() {
        // Arrange
        String requestId = "test-id-3";
        EmailRequest request = new EmailRequest(requestId, "once@example.com", "One Time", "Send only once");

        EmailProvider mockProvider = mock(EmailProvider.class);
        when(mockProvider.send(request)).thenReturn(true);
        when(mockProvider.getName()).thenReturn("MockProvider1");

        RateLimiter mockLimiter = mock(RateLimiter.class);
        when(mockLimiter.allow()).thenReturn(true);

        EmailServiceImpl service = new EmailServiceImpl(
                List.of(mockProvider),
                new IdempotencyChecker(),
                mockLimiter
        );

        // Act - First call
        EmailStatus firstResponse = service.sendEmail(request);

        // Act - Second call with same requestId
        EmailStatus secondResponse = service.sendEmail(request);

        // Assert status is cached
        assertEquals("SENT", firstResponse.getStatus());
        assertEquals(firstResponse.getRequestId(), secondResponse.getRequestId());//
        assertEquals(firstResponse.getStatus(), secondResponse.getStatus());
        assertEquals(firstResponse.getMessage(), secondResponse.getMessage());
        assertEquals(firstResponse.getProviderUsed(), secondResponse.getProviderUsed());
        assertEquals(firstResponse.getAttempts(), secondResponse.getAttempts());

        // Verify send() was called only once
        verify(mockProvider, times(1)).send(request);
    }
    @Test
    void testRateLimitBlocksSend() {
        // Arrange
        String requestId = "test-id-4";
        EmailRequest request = new EmailRequest(requestId, "limit@example.com", "Rate Check", "This should be blocked");

        EmailProvider mockProvider = mock(EmailProvider.class);
        when(mockProvider.send(any())).thenReturn(true);
        when(mockProvider.getName()).thenReturn("MockProvider1");

        RateLimiter mockLimiter = mock(RateLimiter.class);
        when(mockLimiter.allow()).thenReturn(false); // Simulate rate limit exceeded

        EmailServiceImpl service = new EmailServiceImpl(
                List.of(mockProvider),
                new IdempotencyChecker(),
                mockLimiter
        );

        // Act
        EmailStatus status = service.sendEmail(request);

        // Assert
        assertEquals("RATE_LIMITED", status.getStatus());
        assertEquals("Rate limit exceeded", status.getMessage());
        assertNull(status.getProviderUsed());
        assertEquals(0, status.getAttempts());

        // Ensure no provider was called
        verify(mockProvider, never()).send(any());
    }
    @Test
    void testAllProvidersFailEventually() {
        // Arrange
        EmailRequest request = new EmailRequest("test-id-5", "failall@example.com", "Failure", "No provider should succeed");

        // Mock both providers to always fail
        EmailProvider provider1 = mock(EmailProvider.class);
        when(provider1.send(request)).thenReturn(false);
        when(provider1.getName()).thenReturn("MockProvider1");

        EmailProvider provider2 = mock(EmailProvider.class);
        when(provider2.send(request)).thenReturn(false);
        when(provider2.getName()).thenReturn("MockProvider2");

        RateLimiter mockLimiter = mock(RateLimiter.class);
        when(mockLimiter.allow()).thenReturn(true);

        EmailServiceImpl service = new EmailServiceImpl(
                List.of(provider1, provider2),
                new IdempotencyChecker(),
                mockLimiter
        );

        // Act
        EmailStatus status = service.sendEmail(request);

        // Assert
        assertEquals("FAILED", status.getStatus());
        assertEquals("All providers failed", status.getMessage());
        assertEquals("MockProvider2", status.getProviderUsed()); // The last one try
        assertTrue(status.getAttempts() >= 6); // 3 + 3 retries
    }
    @Test
    void testProvider1FailsProvider2Succeeds() {
        // Arrange
        EmailRequest request = new EmailRequest("test-id-fallback-1", "user@domain.com", "Fallback Case 1", "Body");

        // Mock Provider 1: always fails
        EmailProvider provider1 = mock(EmailProvider.class);
        when(provider1.send(request)).thenReturn(false);
        when(provider1.getName()).thenReturn("MockProvider1");

        // Mock Provider 2: succeeds
        EmailProvider provider2 = mock(EmailProvider.class);
        when(provider2.send(request)).thenReturn(true);
        when(provider2.getName()).thenReturn("MockProvider2");

        RateLimiter limiter = mock(RateLimiter.class);
        when(limiter.allow()).thenReturn(true);

        EmailServiceImpl service = new EmailServiceImpl(
                List.of(provider1, provider2),
                new IdempotencyChecker(),
                limiter
        );

        // Act
        EmailStatus status = service.sendEmail(request);

        // Assert
        assertEquals("SENT", status.getStatus());
        assertEquals("MockProvider2", status.getProviderUsed());
        assertTrue(status.getAttempts() >= 4); // 3 failed + 1 successful
        assertEquals("Email sent successfully", status.getMessage());
    }
    @Test
    void testProvider2FailsProvider1Succeeds() {
        // Arrange
        EmailRequest request = new EmailRequest("test-id-fallback-2", "user@domain.com", "Fallback Case 2", "Body");

        // Mock Provider 1: succeeds
        EmailProvider provider1 = mock(EmailProvider.class);
        when(provider1.send(request)).thenReturn(true);
        when(provider1.getName()).thenReturn("MockProvider1");

        // Mock Provider 2: never called, but define anyway
        EmailProvider provider2 = mock(EmailProvider.class);
        when(provider2.send(request)).thenReturn(false);
        when(provider2.getName()).thenReturn("MockProvider2");

        RateLimiter limiter = mock(RateLimiter.class);
        when(limiter.allow()).thenReturn(true);

        EmailServiceImpl service = new EmailServiceImpl(
                List.of(provider1, provider2), // Provider1 comes first
                new IdempotencyChecker(),
                limiter
        );

        // Act
        EmailStatus status = service.sendEmail(request);

        // Assert
        assertEquals("SENT", status.getStatus());
        assertEquals("MockProvider1", status.getProviderUsed());
        assertTrue(status.getAttempts() <= 3); // Should succeed early
        assertEquals("Email sent successfully", status.getMessage());

        // Ensure provider2 was never called
        verify(provider2, never()).send(any());
    }
}