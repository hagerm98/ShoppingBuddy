package com.hager.shoppingbuddy.entity;

import jakarta.persistence.*;
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
@Table(name = "ShoppingRequests")
public class ShoppingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "shopping_request_id_seq")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shopper_id", referencedColumnName = "id")
    private Shopper shopper;

    @Enumerated(EnumType.STRING)
    private ShoppingRequestStatus status;

    @OneToMany(mappedBy = "shoppingRequest", fetch = FetchType.LAZY)
    private List<Item> items;

    @OneToMany(mappedBy = "shoppingRequest", fetch = FetchType.LAZY)
    private List<ChatMessage> chatMessages;

    private Instant createdAt;

    private Instant updatedAt;

    private double totalPrice;

    private String deliveryAddress;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;
}
