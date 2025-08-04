package com.hager.shoppingbuddy.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "shopping_requests")
public class ShoppingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "shopping_request_id_seq")
    private Long id;

    @NotNull(message = "Customer cannot be null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shopper_id", referencedColumnName = "id")
    private Shopper shopper;

    @NotNull(message = "Status cannot be null")
    @Enumerated(EnumType.STRING)
    private ShoppingRequestStatus status;

    @OneToMany(mappedBy = "shoppingRequest", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Item> items;

    @OneToMany(mappedBy = "shoppingRequest", fetch = FetchType.LAZY)
    private List<ChatMessage> chatMessages;

    @NotNull(message = "Created date cannot be null")
    private Instant createdAt;

    private Instant updatedAt;

    @DecimalMin(value = "0.0", inclusive = false, message = "Estimated items price must be greater than 0")
    private double estimatedItemsPrice;

    @DecimalMin(value = "8.0", message = "Delivery fee must be at least 8 euros")
    private double deliveryFee;

    @NotBlank(message = "Delivery address is required")
    @Size(max = 500, message = "Delivery address cannot exceed 500 characters")
    private String deliveryAddress;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double longitude;

    @NotBlank(message = "Store name is required")
    @Size(max = 200, message = "Store name cannot exceed 200 characters")
    private String storeName;

    @NotBlank(message = "Store address is required")
    @Size(max = 500, message = "Store address cannot exceed 500 characters")
    private String storeAddress;

    @DecimalMin(value = "-90.0", message = "Store latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Store latitude must be between -90 and 90")
    private Double storeLatitude;

    @DecimalMin(value = "-180.0", message = "Store longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Store longitude must be between -180 and 180")
    private Double storeLongitude;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;
}
