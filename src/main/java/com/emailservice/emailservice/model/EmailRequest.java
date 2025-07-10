package com.emailservice.emailservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the input payload to send an email.
 */
@Data                       // Generates getters, setters, equals, hashCode, toString
@NoArgsConstructor          // Generates a no-arg constructor
@AllArgsConstructor         // Generates a constructor with all fields
public class EmailRequest {
    private String requestId;   // Unique ID for idempotency
    private String to;          // Recipient's email address
    private String subject;     // Email subject line
    private String body;        // Email content
}