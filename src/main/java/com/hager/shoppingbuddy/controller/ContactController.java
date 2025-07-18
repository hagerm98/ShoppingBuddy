package com.hager.shoppingbuddy.controller;

import com.hager.shoppingbuddy.dto.ContactRequest;
import com.hager.shoppingbuddy.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @PostMapping("/submit")
    public ResponseEntity<String> submitContactForm(@Valid @RequestBody ContactRequest contactRequest) {
        log.info("Received contact form submission from: {}", contactRequest.getEmail());

        try {
            contactService.processContactForm(contactRequest);
            return ResponseEntity.ok("Your message has been sent successfully. We'll get back to you soon!");
        } catch (Exception e) {
            log.error("Error processing contact form: {}", e.getMessage());
            return ResponseEntity.status(500).body("Sorry, there was an error sending your message. Please try again later.");
        }
    }
}
