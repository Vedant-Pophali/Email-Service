package com.emailservice.emailservice.service;


import com.emailservice.emailservice.model.EmailRequest;
import com.emailservice.emailservice.model.EmailStatus;

/**
 * EmailService defines the contract for sending and tracking emails.
 */
public interface EmailService {

    /**
     * Send an email with retry, fallback, and tracking.
     */
    EmailStatus sendEmail(EmailRequest request);

    /**
     * Retrieve email status by requestId.
     */
    EmailStatus getStatusByRequestId(String requestId);
}

