package com.hager.shoppingbuddy.dto;

import com.hager.shoppingbuddy.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String role;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean enabled;

    public static UserResponse fromUser(User user) {
        return UserResponse.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .enabled(user.isEnabled())
                .build();
    }
}
