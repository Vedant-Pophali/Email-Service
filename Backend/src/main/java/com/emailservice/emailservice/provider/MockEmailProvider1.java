package com.emailservice.emailservice.provider;
import com.emailservice.emailservice.model.EmailRequest;
import com.emailservice.emailservice.provide.EmailProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Simulates a mock email provider with ~70% success rate.
 */
@Component
public class MockEmailProvider1 implements EmailProvider {

    private static final Logger logger = LoggerFactory.getLogger(MockEmailProvider1.class);
    private static final Random random = new Random();

    @Override
    public boolean send(EmailRequest request) {
        // Simulate 70% success rate
        boolean success = random.nextDouble() < 0.7;

        logger.info("[MockEmailProvider1] Sending to: {}, success={}", request.getTo(), success);
        return success;
    }

    @Override
    public String getName() {
        return "MockProvider1";
    }
}