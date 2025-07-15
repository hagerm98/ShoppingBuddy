package com.hager.shoppingbuddy.controller;

import com.hager.shoppingbuddy.dto.RegistrationRequest;
import com.hager.shoppingbuddy.dto.RegistrationResponse;
import com.hager.shoppingbuddy.dto.TokenConfirmationResponse;
import com.hager.shoppingbuddy.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/user")
@AllArgsConstructor
public class UserController {
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@RequestBody @Valid RegistrationRequest registrationRequest) {
        log.info("Received registration request for user: {}", registrationRequest.getEmail());

        boolean isSuccessful = userService.register(registrationRequest);
        HttpStatus status = isSuccessful ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(
                new RegistrationResponse(
                        isSuccessful ? "User registered successfully" : "User registration failed",
                        registrationRequest.getEmail(),
                        isSuccessful
                )
        );
    }

    @GetMapping("/confirm")
    public ResponseEntity<TokenConfirmationResponse> confirm(@RequestParam("token") String token) {
        log.info("Received confirmation request for token: {}", token);

        boolean isSuccessful = userService.confirmToken(token);
        HttpStatus status = isSuccessful ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(
                new TokenConfirmationResponse(
                        isSuccessful ? "Token confirmed successfully" : "Token confirmation failed",
                        isSuccessful,
                        isSuccessful ? "/user/login" : null
                )
        );
    }
}
