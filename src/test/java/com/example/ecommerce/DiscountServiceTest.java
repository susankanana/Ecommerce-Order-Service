package com.example.ecommerce;

import com.example.ecommerce.entity.User;
import com.example.ecommerce.service.DiscountService;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DiscountServiceTest {
    private final DiscountService discountService = new DiscountService();

    @Test
    void save10ShouldReturn10PercentForAnyUser() {
        User u = User.builder().id(1L).name("A").vipStatus(false).build();
        assertEquals(0.10, discountService.discountPercentForCode("SAVE10", u).orElse(0.0), 0.001);
    }

    @Test
    void vip20ShouldReturn20ForVip() {
        User vip = User.builder().id(2L).name("VIP").vipStatus(true).build();
        assertEquals(0.20, discountService.discountPercentForCode("VIP20", vip).orElse(0.0), 0.001);
    }

    @Test
    void vip20ShouldBeInvalidForNonVip() {
        User normal = User.builder().id(3L).name("N").vipStatus(false).build();
        assertTrue(discountService.discountPercentForCode("VIP20", normal).isEmpty());
    }

    @Test
    void invalidCodeReturnsEmpty() {
        User any = User.builder().id(4L).name("X").vipStatus(false).build();
        assertTrue(discountService.discountPercentForCode("NOPE", any).isEmpty());
    }
}
