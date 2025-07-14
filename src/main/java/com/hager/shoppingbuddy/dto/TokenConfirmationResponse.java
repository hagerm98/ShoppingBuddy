package com.hager.shoppingbuddy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenConfirmationResponse {
    private String message;
    private boolean success;
    private String redirectUrl;
}

