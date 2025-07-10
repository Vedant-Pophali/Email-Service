package com.emailservice.emailservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Stores the result of an email send attempt.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailStatus {

    private String requestId;        // Same as EmailRequest ID
    private String status;           // SENT, FAILED, RATE_LIMITED
    private String providerUsed;     // MockProvider1 or MockProvider2
    private int attempts;            // Number of total attempts made
    private String message;          // Success/failure message
    private LocalDateTime timestamp; // Time of final attempt
}
