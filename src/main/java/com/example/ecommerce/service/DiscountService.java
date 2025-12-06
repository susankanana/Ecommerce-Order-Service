package com.example.ecommerce.service;

import com.example.ecommerce.entity.User;
import org.springframework.stereotype.Service;
import java.util.Optional;

/**
 * Single Responsibility: determines discount percent for a code and user.
 * Valid codes: SAVE10 (10%), VIP20 (20% only if user.vipStatus true)
 */
@Service
public class DiscountService {

    public Optional<Double> discountPercentForCode(String code, User user) {
        if (code == null || code.isBlank()) return Optional.empty();
        switch(code.trim().toUpperCase()) {
            case "SAVE10": return Optional.of(0.10);
            case "VIP20":
                if (user != null && user.isVipStatus()) return Optional.of(0.20);
                return Optional.empty(); // invalid for non-VIP users
            default:
                return Optional.empty(); // invalid code
        }
    }
}

