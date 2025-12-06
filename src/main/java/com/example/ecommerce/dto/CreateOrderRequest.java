package com.example.ecommerce.dto;

import lombok.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    @NotNull
    private Long userId;

    @NotEmpty
    private List<Item> items;

    private String discountCode; // optional

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        @NotNull
        private Long productId;

        @NotNull @Min(1)
        private Integer quantity;
    }
}

