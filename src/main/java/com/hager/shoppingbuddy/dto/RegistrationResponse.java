package com.hager.shoppingbuddy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegistrationResponse {
    private String message;
    private String email;
    private boolean success;
}

