package com.hager.shoppingbuddy.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "item_id_seq")
    private Long id;

    @NotNull(message = "Shopping request cannot be null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shopping_request_id", referencedColumnName = "id")
    private ShoppingRequest shoppingRequest;

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
