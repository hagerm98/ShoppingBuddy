package com.hager.shoppingbuddy.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "tokens")
public class Token {

    @Id
    @SequenceGenerator(name = "token_sequence", sequenceName = "token_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "token_sequence")
    private Long id;

    @NotBlank(message = "Token cannot be blank")
    @Size(max = 500, message = "Token cannot exceed 500 characters")
    @Column(nullable = false)
    private String token;

    @NotNull(message = "Created timestamp cannot be null")
    @Column(nullable = false)
    private Instant createdAt;

    @NotNull(message = "Expiration timestamp cannot be null")
    @Column(nullable = false)
    private Instant expiresAt;

    private Instant confirmedAt;

    @NotNull(message = "User cannot be null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
