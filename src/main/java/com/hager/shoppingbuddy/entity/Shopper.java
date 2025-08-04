package com.hager.shoppingbuddy.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "shoppers")
public class Shopper {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "shopper_id_seq")
    private Long id;

    @NotNull(message = "User cannot be null")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @NotNull(message = "Balance cannot be null")
    @DecimalMin(value = "0.0", message = "Balance cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "Balance must have at most 8 integer digits and 2 decimal places")
    @Column(name = "balance", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;
}
