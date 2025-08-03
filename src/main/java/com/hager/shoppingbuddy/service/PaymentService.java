package com.hager.shoppingbuddy.service;

import com.hager.shoppingbuddy.entity.Payment;
import com.hager.shoppingbuddy.entity.PaymentStatus;
import com.hager.shoppingbuddy.exception.PaymentException;
import com.hager.shoppingbuddy.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Value("${shoppingbuddy.stripe.secretkey}")
    private String stripeSecretKey;

    @Getter
    @Value("${shoppingbuddy.stripe.publickey}")
    private String stripePublicKey;

    @Transactional
    public void createPaymentIntent(Long shoppingRequestId, Long customerId, double amount) throws PaymentException {
        log.info("Creating payment intent for shopping request: {} with amount: {}", shoppingRequestId, amount);

        Stripe.apiKey = stripeSecretKey;

        long amountInCents = Math.round(amount * 100);

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency("eur")
                .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL)
                .putMetadata("shopping_request_id", String.valueOf(shoppingRequestId))
                .putMetadata("customer_id", String.valueOf(customerId))
                .setDescription("Shopping Request #" + shoppingRequestId)
                .addPaymentMethodType("card")
                .build();

        PaymentIntent paymentIntent;
        try {
            paymentIntent = PaymentIntent.create(params);
        } catch (StripeException e) {
            throw new PaymentException("Failed to create payment intent: " + e.getMessage(), e);
        }

        Payment payment = Payment.builder()
                .shoppingRequestId(shoppingRequestId)
                .customerId(customerId)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .stripePaymentIntentId(paymentIntent.getId())
                .stripeClientSecret(paymentIntent.getClientSecret())
                .createdTimestamp(Instant.now())
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment intent created with ID: {} for shopping request: {}", paymentIntent.getId(), shoppingRequestId);

    }

    @Transactional
    public Payment authorizePayment(Long shoppingRequestId) throws PaymentException {
        log.info("Authorizing payment for shopping request: {}", shoppingRequestId);

        Payment payment = paymentRepository.findByShoppingRequestId(shoppingRequestId)
                .orElseThrow(() -> new PaymentException("Payment not found for shopping request: " + shoppingRequestId));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentException("Cannot authorize payment in status: " + payment.getStatus() + ". Payment must be PENDING.");
        }

        Stripe.apiKey = stripeSecretKey;

        PaymentIntent paymentIntent;
        try {
            paymentIntent = PaymentIntent.retrieve(payment.getStripePaymentIntentId());
        } catch (StripeException e) {
            throw new PaymentException("Failed to retrieve payment intent: " + e.getMessage(), e);
        }

        if (!"requires_capture".equals(paymentIntent.getStatus())) {
            throw new PaymentException("Payment intent is not authorized. Status: " + paymentIntent.getStatus());
        }

        payment.setStatus(PaymentStatus.AUTHORIZED);

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment authorized successfully for shopping request: {}", shoppingRequestId);

        return savedPayment;
    }

    @Transactional
    public Payment capturePayment(Long shoppingRequestId) throws PaymentException {
        log.info("Capturing payment for shopping request: {}", shoppingRequestId);

        Payment payment = paymentRepository.findByShoppingRequestId(shoppingRequestId)
                .orElseThrow(() -> new PaymentException("Payment not found for shopping request: " + shoppingRequestId));

        if (payment.getStatus() != PaymentStatus.AUTHORIZED && payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentException("Cannot capture payment in status: " + payment.getStatus() + ". Payment must be AUTHORIZED or PENDING.");
        }

        Stripe.apiKey = stripeSecretKey;

        PaymentIntent paymentIntent;
        try {
            paymentIntent = PaymentIntent.retrieve(payment.getStripePaymentIntentId());
        } catch (StripeException e) {
            throw new PaymentException("Failed to retrieve payment intent: " + e.getMessage(), e);
        }

        if (!"requires_capture".equals(paymentIntent.getStatus())) {
            throw new PaymentException("Payment intent is not ready for capture. Status: " + paymentIntent.getStatus());
        }

        try {
            paymentIntent.capture();
        } catch (StripeException e) {
            throw new PaymentException("Failed to capture payment intent: " + e.getMessage(), e);
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setCollectedTimestamp(Instant.now());

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment captured successfully for shopping request: {}", shoppingRequestId);

        return savedPayment;
    }

    @Transactional
    public Payment cancelPayment(Long shoppingRequestId) throws PaymentException {
        log.info("Cancelling payment for shopping request: {}", shoppingRequestId);

        Payment payment = paymentRepository.findByShoppingRequestId(shoppingRequestId)
                .orElseThrow(() -> new PaymentException("Payment not found for shopping request: " + shoppingRequestId));

        if (payment.getStatus() != PaymentStatus.AUTHORIZED && payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentException("Cannot cancel payment in status: " + payment.getStatus() + ". Payment must be AUTHORIZED or PENDING.");
        }

        Stripe.apiKey = stripeSecretKey;

        PaymentIntent paymentIntent;
        try {
            paymentIntent = PaymentIntent.retrieve(payment.getStripePaymentIntentId());
        } catch (StripeException e) {
            throw new PaymentException("Failed to retrieve payment intent: " + e.getMessage(), e);
        }

        if ("requires_capture".equals(paymentIntent.getStatus())) {
            try {
                paymentIntent.cancel();
            } catch (StripeException e) {
                throw new PaymentException("Failed to cancel payment intent: " + e.getMessage(), e);
            }
            log.info("Pre-authorized payment intent cancelled for shopping request: {}", shoppingRequestId);
        } else if ("requires_payment_method".equals(paymentIntent.getStatus()) ||
                   "requires_confirmation".equals(paymentIntent.getStatus()) ||
                   "requires_action".equals(paymentIntent.getStatus())) {
            try {
                paymentIntent.cancel();
            } catch (StripeException e) {
                throw new PaymentException("Failed to cancel pending payment intent: " + e.getMessage(), e);
            }
            log.info("Pending payment intent cancelled for shopping request: {}", shoppingRequestId);
        } else {
            log.warn("Payment intent status {} does not require cancellation for shopping request: {}",
                     paymentIntent.getStatus(), shoppingRequestId);
        }

        payment.setStatus(PaymentStatus.CANCELLED);

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment cancelled successfully for shopping request: {}", shoppingRequestId);

        return savedPayment;
    }

    public Payment getPaymentByShoppingRequestId(Long shoppingRequestId) {
        return paymentRepository.findByShoppingRequestId(shoppingRequestId)
                .orElse(null);
    }
}
