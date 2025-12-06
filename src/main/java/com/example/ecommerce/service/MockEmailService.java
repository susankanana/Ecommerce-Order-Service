package com.example.ecommerce.service;

import org.slf4j.*;
import org.springframework.stereotype.Service;

/**
 * Mock email service. In real life this would be async and resilient.
 * Per requirement: if email fails, rollback order (so it's executed inside transaction).
 */
@Service
public class MockEmailService {
    private static final Logger log = LoggerFactory.getLogger(MockEmailService.class);

    public void sendOrderConfirmation(Long userId, Long orderId) {
        // Simulate sending: here we simply log. Throw RuntimeException to simulate a failure.
        log.info("Email sent to userId={} for orderId={}", userId, orderId);
    }
}

