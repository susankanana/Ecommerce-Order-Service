package com.example.ecommerce;

import com.example.ecommerce.dto.CreateOrderRequest;
import com.example.ecommerce.dto.CreateOrderResponse;
import com.example.ecommerce.entity.*;
import com.example.ecommerce.entity.enums.OrderStatus;
import com.example.ecommerce.repository.*;
import com.example.ecommerce.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private DiscountService discountService;
    @Mock private PaymentGatewaySimulator paymentGateway;
    @Mock private MockEmailService emailService;

    @InjectMocks
    private OrderService orderService;

    private User mockUser;
    private Product mockProduct;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);

        mockProduct = new Product();
        mockProduct.setId(100L);
        mockProduct.setPrice(10.0);
        mockProduct.setStock(10);
        mockProduct.setReservedStock(0);
    }

    @Test
    @DisplayName("Should create order successfully when all conditions are met")
    void createOrder_Success() {
        // Arrange
        CreateOrderRequest.Item item = new CreateOrderRequest.Item(100L, 2);
        CreateOrderRequest request = new CreateOrderRequest(1L, List.of(item), "SAVE10");

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(productRepository.findAllById(anySet())).thenReturn(List.of(mockProduct));
        when(discountService.discountPercentForCode("SAVE10", mockUser)).thenReturn(Optional.of(0.10)); // 10% discount
        when(paymentGateway.charge(anyLong(), anyDouble())).thenReturn(true);
        
        // Mocking the save to return an object with an ID
        Order savedOrder = Order.builder().id(500L).total(18.0).status(OrderStatus.CONFIRMED).build();
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // Act
        CreateOrderResponse response = orderService.createOrder(request);

        // Assert
        assertNotNull(response);
        assertEquals(500L, response.getOrderId());
        assertEquals(18.0, response.getTotal()); // (10*2) * 0.9 = 18.0
        
        verify(productRepository, atLeastOnce()).save(any(Product.class));
        verify(emailService).sendOrderConfirmation(eq(1L), eq(500L));
    }

    @Test
    @DisplayName("Should throw exception when stock is insufficient")
    void createOrder_InsufficientStock() {
        // Arrange
        CreateOrderRequest.Item item = new CreateOrderRequest.Item(100L, 20); // 20 requested, only 10 in stock
        CreateOrderRequest request = new CreateOrderRequest(1L, List.of(item), null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(productRepository.findAllById(anySet())).thenReturn(List.of(mockProduct));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            orderService.createOrder(request);
        });

        assertTrue(exception.getReason().contains("Insufficient stock"));
        verify(paymentGateway, never()).charge(anyLong(), anyDouble());
    }
}