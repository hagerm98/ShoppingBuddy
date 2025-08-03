package com.hager.shoppingbuddy.controller;

import com.hager.shoppingbuddy.entity.Payment;
import com.hager.shoppingbuddy.service.PaymentService;
import com.hager.shoppingbuddy.service.ShoppingRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final ShoppingRequestService shoppingRequestService;

    @GetMapping("/public-key")
    @ResponseBody
    public ResponseEntity<String> getStripePublicKey() {
        try {
            String publicKey = paymentService.getStripePublicKey();
            if (publicKey == null || publicKey.isEmpty()) {
                log.error("Stripe public key is not configured");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Payment configuration error");
            }
            return ResponseEntity.ok(publicKey);
        } catch (Exception e) {
            log.error("Error retrieving Stripe public key", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Configuration error");
        }
    }

    @GetMapping("/{requestId}/authorize")
    public String authorizePaymentCallback(@PathVariable Long requestId,
                                           @RequestParam(required = false) String payment_intent,
                                           @RequestParam(required = false) String payment_intent_client_secret,
                                           @RequestParam(required = false) String redirect_status) {
        try {
            log.info("Processing payment authorization callback for request: {} with redirect_status: {}", requestId, redirect_status);

            if ("succeeded".equals(redirect_status)) {
                Payment payment = paymentService.authorizePayment(requestId);
                shoppingRequestService.updatePaymentStatus(requestId, payment.getStatus());
                log.info("Payment authorized successfully for shopping request: {}", requestId);
                return String.format("redirect:/checkout/%s/success", requestId);
            } else {
                log.warn("Payment authorization failed for request: {} with status: {}", requestId, redirect_status);
                return String.format("redirect:/checkout/%s/payment?error=authorization_failed", requestId);
            }
        } catch (Exception e) {
            log.error("Error processing payment authorization callback for shopping request: {}", requestId, e);
            return String.format("redirect:/checkout/%s/payment?error=processing_failed", requestId);
        }
    }
}
