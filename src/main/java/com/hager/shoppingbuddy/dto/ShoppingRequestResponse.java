package com.hager.shoppingbuddy.dto;

import com.hager.shoppingbuddy.entity.ShoppingRequestStatus;
import com.hager.shoppingbuddy.entity.PaymentStatus;
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
public class ShoppingRequestResponse {

    private Long id;
    private Long customerId;
    private String customerName;
    private Long shopperId;
    private String shopperName;
    private ShoppingRequestStatus status;
    private List<ItemResponse> items;
    private Instant createdAt;
    private Instant updatedAt;
    private double estimatedItemsPrice;
    private double deliveryFee;
    private String deliveryAddress;
    private PaymentStatus paymentStatus;
    private Double latitude;
    private Double longitude;
    private String stripeClientSecret;
    private String stripePaymentIntentId;
}
