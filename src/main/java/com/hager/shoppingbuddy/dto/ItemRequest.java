package com.hager.shoppingbuddy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ItemRequest {

    @NotBlank(message = "Item name is required")
    @Size(max = 100, message = "Item name cannot exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Min(value = 1, message = "Amount must be at least 1")
    private int amount;

    @Size(max = 50, message = "Category cannot exceed 50 characters")
    private String category;
}
