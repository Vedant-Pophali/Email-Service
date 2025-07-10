package com.emailservice.emailservice.provide;

import com.emailservice.emailservice.model.EmailRequest;

public interface EmailProvider {
    /**
     * Attempts to send an email.
     *
     * @param request the email request payload
     * @return true if send was successful, false otherwise
     */
    boolean send(EmailRequest request);

    /**
     * Returns the name of the provider (e.g., MockProvider1).
     */
    String getName();
}