package com.hager.shoppingbuddy.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
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
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "chat_message_id_seq")
    private Long id;

    @NotNull(message = "Shopping request cannot be null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shopping_request_id", referencedColumnName = "id")
    private ShoppingRequest shoppingRequest;

    @NotNull(message = "Sender cannot be null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", referencedColumnName = "id")
    private User sender;

    @NotBlank(message = "Message content cannot be empty")
    @Size(max = 1000, message = "Message content cannot exceed 1000 characters")
    private String messageContent;

    @NotNull(message = "Timestamp cannot be null")
    private Instant timestamp;
}
