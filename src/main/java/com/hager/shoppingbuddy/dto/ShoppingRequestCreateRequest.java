package com.hager.shoppingbuddy.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShoppingRequestCreateRequest {

    @NotEmpty(message = "Shopping list cannot be empty")
    @Valid
    private List<ItemRequest> items;

    @NotBlank(message = "Delivery address is required")
    @Size(max = 500, message = "Delivery address cannot exceed 500 characters")
    private String deliveryAddress;

    @NotNull(message = "Estimated items price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Estimated items price must be greater than 0")
    private Double estimatedItemsPrice;

    @NotNull(message = "Delivery fee is required")
    @DecimalMin(value = "8.0", message = "Delivery fee must be at least 8 euros")
    private Double deliveryFee;
}
