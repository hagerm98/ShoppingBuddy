package com.hager.shoppingbuddy.controller;

import com.hager.shoppingbuddy.dto.UserUpdateRequest;
import com.hager.shoppingbuddy.dto.UserResponse;
import com.hager.shoppingbuddy.dto.RegistrationRequest;
import com.hager.shoppingbuddy.dto.RegistrationResponse;
import com.hager.shoppingbuddy.entity.User;
import com.hager.shoppingbuddy.exception.*;
import com.hager.shoppingbuddy.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@Slf4j
@RestController
@RequestMapping("/api/user")
@AllArgsConstructor
public class UserController {
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@RequestBody @Valid RegistrationRequest registrationRequest) throws EmailAlreadyExistsException {
        log.info("Received registration request for user: {}", registrationRequest.getEmail());

        boolean isSuccessful = userService.register(registrationRequest);
        HttpStatus status = isSuccessful ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;

        String message = isSuccessful
                ? "Registration successful! We've sent a confirmation email to " + registrationRequest.getEmail() +
                ". Please check your inbox and click the confirmation link to activate your account."
                : "Registration failed. This email address may already be registered or there was a server error. Please try again.";

        return ResponseEntity.status(status).body(
                new RegistrationResponse(
                        message,
                        registrationRequest.getEmail(),
                        isSuccessful
                )
        );
    }

    @GetMapping("/confirm")
    public RedirectView confirm(@RequestParam("token") @NotBlank String token) throws UserNotFoundException, InvalidTokenException, TokenExpiredException {
        log.info("Received confirmation request for token: {}", token);

        userService.confirmToken(token);
        return new RedirectView("/login?confirmationSuccess=true");
    }

    @GetMapping
    public ResponseEntity<UserResponse> getUserProfile(Authentication authentication) throws UserNotFoundException {
        User currentUser = (User) authentication.getPrincipal();
        String currentEmail = currentUser.getEmail();

        User user = userService.findByEmail(currentEmail);
        UserResponse userResponse = UserResponse.fromUser(user);

        return ResponseEntity.ok(userResponse);
    }

    @PostMapping
    public ResponseEntity<String> updateProfile(@RequestBody @Valid UserUpdateRequest request,
                                                Authentication authentication) throws UserNotFoundException {
        User currentUser = (User) authentication.getPrincipal();
        String currentEmail = currentUser.getEmail();

        userService.updateProfile(currentEmail, request.getFirstName(),
                                request.getLastName(), request.getPhoneNumber());

        return ResponseEntity.ok("Profile updated successfully");
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestBody @Valid UserUpdateRequest request,
                                                 Authentication authentication)
            throws UserNotFoundException, PasswordChangeException {
        User currentUser = (User) authentication.getPrincipal();
        String currentEmail = currentUser.getEmail();

        if (request.getCurrentPassword() == null || request.getCurrentPassword().trim().isEmpty()) {
            throw new PasswordChangeException("Current password is required");
        }

        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            throw new PasswordChangeException("New password is required");
        }

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new PasswordChangeException("New password and confirmation do not match");
        }

        userService.changePassword(currentEmail, request.getCurrentPassword(), request.getNewPassword());

        return ResponseEntity.ok("Password changed successfully");
    }
}
