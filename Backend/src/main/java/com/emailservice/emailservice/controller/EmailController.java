package com.emailservice.emailservice.controller;

import com.emailservice.emailservice.service.EmailService;
import com.emailservice.emailservice.model.EmailRequest;
import com.emailservice.emailservice.model.EmailStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email")
public class EmailController {

    private final EmailService emailService;

    // Constructor-based injection
    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Endpoint to send an email.
     */
    @PostMapping("/send")
    public ResponseEntity<String> sendEmail(@RequestBody EmailRequest request) {
        EmailStatus status = emailService.sendEmail(request);
        HttpStatus httpStatus;

        switch (status.getStatus()) {
            case "SENT":
                httpStatus = HttpStatus.OK;
                break;
            case "RATE_LIMITED":
                httpStatus = HttpStatus.TOO_MANY_REQUESTS;
                break;
            case "FAILED":
                httpStatus = HttpStatus.SERVICE_UNAVAILABLE;
                break;
            default:
                httpStatus = HttpStatus.BAD_REQUEST;
        }

        return ResponseEntity.status(httpStatus).body(status.getMessage());
    }

    /**
     * Endpoint to get status of email by requestId.
     */
    @GetMapping("/status/{requestId}")
    public ResponseEntity<EmailStatus> getEmailStatus(@PathVariable String requestId) {
        EmailStatus status = emailService.getStatusByRequestId(requestId);
        if (status != null) {
            return ResponseEntity.ok(status);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}