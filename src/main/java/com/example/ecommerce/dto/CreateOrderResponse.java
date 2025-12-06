package com.example.ecommerce.dto;


import lombok.*;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderResponse {
    private Long orderId;
    private double total;
    private String status;
    private Instant createdAt;
}
