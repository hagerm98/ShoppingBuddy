package com.hager.shoppingbuddy.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payment_id_seq")
    private Long id;

    @NotNull(message = "Shopping request ID cannot be null")
    private Long shoppingRequestId;

    @NotNull(message = "Customer ID cannot be null")
    private Long customerId;

    @DecimalMin(value = "0.0", inclusive = false, message = "Payment amount must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Amount must have at most 8 integer digits and 2 decimal places")
    private BigDecimal amount;

    @NotNull(message = "Payment status cannot be null")
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Size(max = 255, message = "Stripe payment intent ID cannot exceed 255 characters")
    private String stripePaymentIntentId;

    @Size(max = 255, message = "Stripe client secret cannot exceed 255 characters")
    private String stripeClientSecret;

    @NotNull(message = "Created timestamp cannot be null")
    private Instant createdTimestamp;

    private Instant collectedTimestamp;
}
