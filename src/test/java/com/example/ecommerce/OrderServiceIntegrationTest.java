package com.example.ecommerce;

import com.example.ecommerce.dto.CreateOrderRequest;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.service.*;
import com.example.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@AutoConfigureTestDatabase
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @MockBean
    private MockEmailService emailService;

    @Test
    @DisplayName("Should rollback transaction when email sending fails")
    void shouldRollbackWhenEmailFails() {
        // Arrange
        Product product = new Product();
        product.setPrice(10.0);
        product.setStock(10);
        product.setReservedStock(0);
        product = productRepository.save(product);

        // Using your Item DTO structure
        CreateOrderRequest.Item item = new CreateOrderRequest.Item(product.getId(), 2);
        CreateOrderRequest request = new CreateOrderRequest(1L, List.of(item), null);

        // Act: Mock the failure
        doThrow(new RuntimeException("Email Server Down"))
            .when(emailService).sendOrderConfirmation(anyLong(), anyLong());

        // Assert: Ensure the exception is thrown
        assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(request);
        });

        // Verify Rollback: If transaction worked, stock remains 10
        Product updatedProduct = productRepository.findById(product.getId()).get();
        assertEquals(10, updatedProduct.getStock());
    }
}