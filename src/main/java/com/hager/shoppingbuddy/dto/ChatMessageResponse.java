package com.hager.shoppingbuddy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMessageResponse {

    private Long id;
    private Long shoppingRequestId;
    private Long senderId;
    private String senderName;
    private String messageContent;
    private Instant timestamp;
}
