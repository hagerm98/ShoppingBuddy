package com.hager.shoppingbuddy.controller;

import com.hager.shoppingbuddy.dto.RegistrationRequest;
import com.hager.shoppingbuddy.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@AllArgsConstructor
public class UserController {
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid RegistrationRequest registrationRequest) {
        return ResponseEntity.ok(userService.register(registrationRequest));
    }

    @GetMapping("/confirm")
    public ResponseEntity<String> confirm(@RequestParam("token") String token) {
        return ResponseEntity.ok(userService.confirmToken(token));
    }
}
