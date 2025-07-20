package com.hager.shoppingbuddy.controller;

import com.hager.shoppingbuddy.dto.RegistrationRequest;
import com.hager.shoppingbuddy.dto.RegistrationResponse;
import com.hager.shoppingbuddy.exception.EmailAlreadyExistsException;
import com.hager.shoppingbuddy.exception.InvalidTokenException;
import com.hager.shoppingbuddy.exception.TokenExpiredException;
import com.hager.shoppingbuddy.exception.UserNotFoundException;
import com.hager.shoppingbuddy.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
}
