package com.hager.shoppingbuddy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegistrationRequest {
    @NonNull
    private String firstName;

    @NonNull
    private String lastName;

    @NonNull
    @Email
    private String email;

    @NonNull
    @Size(min = 8)
    private String password;
}
