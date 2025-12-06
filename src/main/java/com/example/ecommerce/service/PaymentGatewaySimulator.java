package com.example.ecommerce.service;


import org.springframework.stereotype.Service;

/**
 * Simulated payment gateway.
 * Returns true for success or throws/returns false to simulate failure.
 */
@Service
public class PaymentGatewaySimulator {
    public boolean charge(Long userId, double amount) {
        // simulate success. To test failures in integration tests, you can mock this bean.
        return true;
    }
}

