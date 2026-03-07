package com.example.ecommerce.service;

import com.example.ecommerce.dto.CreateOrderRequest;
import com.example.ecommerce.dto.CreateOrderResponse;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.entity.enums.OrderStatus;
import com.example.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles order creation, reservation, payment and confirmation within a transaction.
 */
@Service
@RequiredArgsConstructor
public class OrderService {
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DiscountService discountService;
    private final PaymentGatewaySimulator paymentGateway;
    private final MockEmailService emailService;

    private static final double MIN_TOTAL_AFTER_DISCOUNT = 5.0;

    @Transactional // everything in one DB transaction
    public CreateOrderResponse createOrder(CreateOrderRequest req) {
        // 1. load and validate user
        User user = userRepository.findById(req.getUserId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user"));
    	// User user = null;
        // 2. Fetch products and prepare map
        Map<Long, Product> products = productRepository.findAllById(
                req.getItems().stream().map(CreateOrderRequest.Item::getProductId).collect(Collectors.toSet())
            ).stream().collect(Collectors.toMap(Product::getId, p -> p));

        // 3. Validate items exist and reserve stock
        for (CreateOrderRequest.Item it : req.getItems()) {
            Product p = products.get(it.getProductId());
            if (p == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid product " + it.getProductId());
            if (p.getStock() - p.getReservedStock() < it.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient stock for product " + p.getId());
            }
            // reserve: increment reservedStock
            p.setReservedStock(p.getReservedStock() + it.getQuantity());
            productRepository.save(p);
        }

        // 4. Calculate total
        double rawTotal = req.getItems().stream()
            .mapToDouble(it -> {
                Product p = products.get(it.getProductId());
                return p.getPrice() * it.getQuantity();
            }).sum();

        // 5. Apply discount
        Optional<Double> perc = discountService.discountPercentForCode(req.getDiscountCode(), user);
        if (req.getDiscountCode() != null && !req.getDiscountCode().isBlank() && perc.isEmpty()) {
            // invalid code
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid discount code");
        }
        double discountPercent = perc.orElse(0.0);
        double afterDiscount = rawTotal * (1 - discountPercent);

        // 6. Minimum fee rule
        if (afterDiscount < MIN_TOTAL_AFTER_DISCOUNT) {
            afterDiscount = MIN_TOTAL_AFTER_DISCOUNT;
        }

        // 7. Simulate payment
        boolean charged = paymentGateway.charge(user.getId(), afterDiscount);
        if (!charged) {
            // Payment failed -> transactional rollback
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Payment failed");
        }

        // 8. Persist order (status CONFIRMED) and deduct actual stock
        Order order = Order.builder()
            .userId(user.getId())
            .total(afterDiscount)
            .createdAt(Instant.now())
            .status(OrderStatus.CONFIRMED)
            .build();
        order = orderRepository.save(order);

        List<OrderItem> savedItems = new ArrayList<>();
        for (CreateOrderRequest.Item it : req.getItems()) {
            Product p = products.get(it.getProductId());
            // deduct actual stock and reduce reservedStock
            p.setStock(p.getStock() - it.getQuantity());
            p.setReservedStock(p.getReservedStock() - it.getQuantity());
            productRepository.save(p);

            OrderItem oi = OrderItem.builder()
                .order(order)
                .productId(p.getId())
                .quantity(it.getQuantity())
                .priceAtOrder(p.getPrice())
                .build();
            savedItems.add(orderItemRepository.save(oi));
        }
        order.setItems(savedItems);
        orderRepository.save(order);

        // 9. Send confirmation email. Per requirement, if this fails, it should rollback.
        emailService.sendOrderConfirmation(user.getId(), order.getId());

        return new CreateOrderResponse(order.getId(), order.getTotal(), order.getStatus().name(), order.getCreatedAt());
    }

    /**
     * Cancel an order ONLY if PENDING and within 10 minutes of createdAt.
     * Reverts reservedStock on success.
     */
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only PENDING orders can be cancelled");
        }

        Duration age = Duration.between(order.getCreatedAt(), Instant.now());
        if (age.toMinutes() > 10) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cancellation window expired");
        }

        // revert reserved stock for each item
        for (OrderItem item : order.getItems()) {
            Product p = productRepository.findById(item.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Product missing"));
            p.setReservedStock(p.getReservedStock() - item.getQuantity());
            productRepository.save(p);
        }
        // mark order as cancelled -- per model we only have PENDING/CONFIRMED; we'll delete or set status to PENDING->(deleted).
        // Per requirement, we remove the order (or mark as cancelled). Here we'll delete it:
        orderRepository.delete(order);
    }
}

