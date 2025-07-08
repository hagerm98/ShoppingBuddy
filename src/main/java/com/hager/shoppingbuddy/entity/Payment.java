package com.hager.shoppingbuddy.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    private Long shoppingRequestId;

    private Long customerId;

    private double amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private Instant createdTimestamp;

    private Instant collectedTimestamp;
}
