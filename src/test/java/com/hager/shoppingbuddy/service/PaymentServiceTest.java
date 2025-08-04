package com.hager.shoppingbuddy.service;

import com.hager.shoppingbuddy.entity.Payment;
import com.hager.shoppingbuddy.entity.PaymentStatus;
import com.hager.shoppingbuddy.exception.PaymentException;
import com.hager.shoppingbuddy.repository.PaymentRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Tests")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentIntent paymentIntent;

    @InjectMocks
    private PaymentService paymentService;

    private final Long shoppingRequestId = 1L;
    private final Long customerId = 2L;
    private final double amount = 50.0;

    @BeforeEach
    void setUp() {
        String stripeSecretKey = "sk_test_123";
        ReflectionTestUtils.setField(paymentService, "stripeSecretKey", stripeSecretKey);
    }

    @Nested
    @DisplayName("Create Payment Intent Tests")
    class CreatePaymentIntentTests {

        @Test
        @DisplayName("Should create payment intent successfully")
        void createPaymentIntent_WhenValidParameters_ShouldCreatePaymentIntent() throws PaymentException {
            // Given
            String paymentIntentId = "pi_test_123";
            String clientSecret = "pi_test_123_secret_456";

            try (MockedStatic<PaymentIntent> mockedPaymentIntent = mockStatic(PaymentIntent.class)) {
                when(paymentIntent.getId()).thenReturn(paymentIntentId);
                when(paymentIntent.getClientSecret()).thenReturn(clientSecret);
                mockedPaymentIntent.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class))).thenReturn(paymentIntent);

                Payment savedPayment = Payment.builder()
                        .id(1L)
                        .shoppingRequestId(shoppingRequestId)
                        .customerId(customerId)
                        .amount(BigDecimal.valueOf(amount))
                        .status(PaymentStatus.PENDING)
                        .stripePaymentIntentId(paymentIntentId)
                        .stripeClientSecret(clientSecret)
                        .createdTimestamp(Instant.now())
                        .build();

                when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

                // When
                paymentService.createPaymentIntent(shoppingRequestId, customerId, amount);

                // Then
                verify(paymentRepository).save(argThat(payment ->
                    payment.getShoppingRequestId().equals(shoppingRequestId) &&
                    payment.getCustomerId().equals(customerId) &&
                    payment.getAmount().equals(BigDecimal.valueOf(amount)) &&
                    payment.getStatus() == PaymentStatus.PENDING &&
                    payment.getStripePaymentIntentId().equals(paymentIntentId) &&
                    payment.getStripeClientSecret().equals(clientSecret)
                ));
            }
        }

        @Test
        @DisplayName("Should throw PaymentException when Stripe fails")
        void createPaymentIntent_WhenStripeThrowsException_ShouldThrowPaymentException() {
            // Given
            try (MockedStatic<PaymentIntent> mockedPaymentIntent = mockStatic(PaymentIntent.class)) {
                mockedPaymentIntent.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                        .thenThrow(new StripeException("Stripe error", "request_123", "code_123", 400) {});

                // When & Then
                assertThatThrownBy(() -> paymentService.createPaymentIntent(shoppingRequestId, customerId, amount))
                        .isInstanceOf(PaymentException.class)
                        .hasMessageContaining("Failed to create payment intent");

                verify(paymentRepository, never()).save(any(Payment.class));
            }
        }
    }

    @Nested
    @DisplayName("Authorize Payment Tests")
    class AuthorizePaymentTests {

        @Test
        @DisplayName("Should authorize payment successfully")
        void authorizePayment_WhenValidRequest_ShouldAuthorizePayment() throws PaymentException {
            // Given
            Payment pendingPayment = Payment.builder()
                    .id(1L)
                    .shoppingRequestId(shoppingRequestId)
                    .customerId(customerId)
                    .amount(BigDecimal.valueOf(amount))
                    .status(PaymentStatus.PENDING)
                    .stripePaymentIntentId("pi_test_123")
                    .build();

            Payment authorizedPayment = Payment.builder()
                    .id(1L)
                    .shoppingRequestId(shoppingRequestId)
                    .customerId(customerId)
                    .amount(BigDecimal.valueOf(amount))
                    .status(PaymentStatus.AUTHORIZED)
                    .stripePaymentIntentId("pi_test_123")
                    .build();

            when(paymentRepository.findByShoppingRequestId(shoppingRequestId))
                    .thenReturn(Optional.of(pendingPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(authorizedPayment);

            try (MockedStatic<PaymentIntent> mockedPaymentIntent = mockStatic(PaymentIntent.class)) {
                when(paymentIntent.getStatus()).thenReturn("requires_capture");
                mockedPaymentIntent.when(() -> PaymentIntent.retrieve("pi_test_123")).thenReturn(paymentIntent);

                // When
                Payment result = paymentService.authorizePayment(shoppingRequestId);

                // Then
                assertThat(result.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
                verify(paymentRepository).save(argThat(payment ->
                    payment.getStatus() == PaymentStatus.AUTHORIZED
                ));
            }
        }

        @Test
        @DisplayName("Should throw PaymentException when payment not found")
        void authorizePayment_WhenPaymentNotFound_ShouldThrowPaymentException() {
            // Given
            when(paymentRepository.findByShoppingRequestId(shoppingRequestId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> paymentService.authorizePayment(shoppingRequestId))
                    .isInstanceOf(PaymentException.class)
                    .hasMessageContaining("Payment not found for shopping request");
        }

        @Test
        @DisplayName("Should throw PaymentException when payment status is not PENDING")
        void authorizePayment_WhenPaymentNotPending_ShouldThrowPaymentException() {
            // Given
            Payment completedPayment = Payment.builder()
                    .status(PaymentStatus.COMPLETED)
                    .build();

            when(paymentRepository.findByShoppingRequestId(shoppingRequestId))
                    .thenReturn(Optional.of(completedPayment));

            // When & Then
            assertThatThrownBy(() -> paymentService.authorizePayment(shoppingRequestId))
                    .isInstanceOf(PaymentException.class)
                    .hasMessageContaining("Cannot authorize payment in status: COMPLETED");
        }

        @Test
        @DisplayName("Should throw PaymentException when payment intent is not authorized")
        void authorizePayment_WhenPaymentIntentNotAuthorized_ShouldThrowPaymentException() {
            // Given
            Payment pendingPayment = Payment.builder()
                    .status(PaymentStatus.PENDING)
                    .stripePaymentIntentId("pi_test_123")
                    .build();

            when(paymentRepository.findByShoppingRequestId(shoppingRequestId))
                    .thenReturn(Optional.of(pendingPayment));

            try (MockedStatic<PaymentIntent> mockedPaymentIntent = mockStatic(PaymentIntent.class)) {
                when(paymentIntent.getStatus()).thenReturn("requires_payment_method");
                mockedPaymentIntent.when(() -> PaymentIntent.retrieve("pi_test_123")).thenReturn(paymentIntent);

                // When & Then
                assertThatThrownBy(() -> paymentService.authorizePayment(shoppingRequestId))
                        .isInstanceOf(PaymentException.class)
                        .hasMessageContaining("Payment intent is not authorized");
            }
        }
    }

    @Nested
    @DisplayName("Capture Payment Tests")
    class CapturePaymentTests {

        @Test
        @DisplayName("Should capture authorized payment successfully")
        void capturePayment_WhenAuthorizedPayment_ShouldCapturePayment() throws PaymentException, StripeException {
            // Given
            Payment authorizedPayment = Payment.builder()
                    .id(1L)
                    .shoppingRequestId(shoppingRequestId)
                    .status(PaymentStatus.AUTHORIZED)
                    .stripePaymentIntentId("pi_test_123")
                    .build();

            Payment capturedPayment = Payment.builder()
                    .id(1L)
                    .shoppingRequestId(shoppingRequestId)
                    .status(PaymentStatus.COMPLETED)
                    .stripePaymentIntentId("pi_test_123")
                    .collectedTimestamp(Instant.now())
                    .build();

            when(paymentRepository.findByShoppingRequestId(shoppingRequestId))
                    .thenReturn(Optional.of(authorizedPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(capturedPayment);

            try (MockedStatic<PaymentIntent> mockedPaymentIntent = mockStatic(PaymentIntent.class)) {
                when(paymentIntent.getStatus()).thenReturn("requires_capture");
                when(paymentIntent.capture()).thenReturn(paymentIntent);
                mockedPaymentIntent.when(() -> PaymentIntent.retrieve("pi_test_123")).thenReturn(paymentIntent);

                // When
                Payment result = paymentService.capturePayment(shoppingRequestId);

                // Then
                assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
                verify(paymentIntent).capture();
                verify(paymentRepository).save(argThat(payment ->
                    payment.getStatus() == PaymentStatus.COMPLETED &&
                    payment.getCollectedTimestamp() != null
                ));
            }
        }

        @Test
        @DisplayName("Should capture pending payment successfully")
        void capturePayment_WhenPendingPayment_ShouldCapturePayment() throws PaymentException, StripeException {
            // Given
            Payment pendingPayment = Payment.builder()
                    .id(1L)
                    .shoppingRequestId(shoppingRequestId)
                    .status(PaymentStatus.PENDING)
                    .stripePaymentIntentId("pi_test_123")
                    .build();

            when(paymentRepository.findByShoppingRequestId(shoppingRequestId))
                    .thenReturn(Optional.of(pendingPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(pendingPayment);

            try (MockedStatic<PaymentIntent> mockedPaymentIntent = mockStatic(PaymentIntent.class)) {
                when(paymentIntent.getStatus()).thenReturn("requires_capture");
                when(paymentIntent.capture()).thenReturn(paymentIntent);
                mockedPaymentIntent.when(() -> PaymentIntent.retrieve("pi_test_123")).thenReturn(paymentIntent);

                // When
                Payment result = paymentService.capturePayment(shoppingRequestId);

                // Then
                verify(paymentIntent).capture();
            }
        }

        @Test
        @DisplayName("Should throw PaymentException when payment has invalid status for capture")
        void capturePayment_WhenInvalidStatus_ShouldThrowPaymentException() {
            // Given
            Payment completedPayment = Payment.builder()
                    .status(PaymentStatus.COMPLETED)
                    .build();

            when(paymentRepository.findByShoppingRequestId(shoppingRequestId))
                    .thenReturn(Optional.of(completedPayment));

            // When & Then
            assertThatThrownBy(() -> paymentService.capturePayment(shoppingRequestId))
                    .isInstanceOf(PaymentException.class)
                    .hasMessageContaining("Cannot capture payment in status: COMPLETED");
        }
    }

    @Nested
    @DisplayName("Cancel Payment Tests")
    class CancelPaymentTests {

        @Test
        @DisplayName("Should cancel authorized payment successfully")
        void cancelPayment_WhenAuthorizedPayment_ShouldCancelPayment() throws PaymentException, StripeException {
            // Given
            Payment authorizedPayment = Payment.builder()
                    .id(1L)
                    .shoppingRequestId(shoppingRequestId)
                    .status(PaymentStatus.AUTHORIZED)
                    .stripePaymentIntentId("pi_test_123")
                    .build();

            when(paymentRepository.findByShoppingRequestId(shoppingRequestId))
                    .thenReturn(Optional.of(authorizedPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(authorizedPayment);

            try (MockedStatic<PaymentIntent> mockedPaymentIntent = mockStatic(PaymentIntent.class)) {
                when(paymentIntent.getStatus()).thenReturn("requires_capture");
                when(paymentIntent.cancel()).thenReturn(paymentIntent);
                mockedPaymentIntent.when(() -> PaymentIntent.retrieve("pi_test_123")).thenReturn(paymentIntent);

                // When
                Payment result = paymentService.cancelPayment(shoppingRequestId);

                // Then
                assertThat(result.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
                verify(paymentIntent).cancel();
                verify(paymentRepository).save(argThat(payment ->
                    payment.getStatus() == PaymentStatus.CANCELLED
                ));
            }
        }

        @Test
        @DisplayName("Should cancel pending payment successfully")
        void cancelPayment_WhenPendingPayment_ShouldCancelPayment() throws PaymentException, StripeException {
            // Given
            Payment pendingPayment = Payment.builder()
                    .id(1L)
                    .shoppingRequestId(shoppingRequestId)
                    .status(PaymentStatus.PENDING)
                    .stripePaymentIntentId("pi_test_123")
                    .build();

            when(paymentRepository.findByShoppingRequestId(shoppingRequestId))
                    .thenReturn(Optional.of(pendingPayment));
            when(paymentRepository.save(any(Payment.class))).thenReturn(pendingPayment);

            try (MockedStatic<PaymentIntent> mockedPaymentIntent = mockStatic(PaymentIntent.class)) {
                when(paymentIntent.getStatus()).thenReturn("requires_payment_method");
                when(paymentIntent.cancel()).thenReturn(paymentIntent);
                mockedPaymentIntent.when(() -> PaymentIntent.retrieve("pi_test_123")).thenReturn(paymentIntent);

                // When
                paymentService.cancelPayment(shoppingRequestId);

                // Then
                verify(paymentIntent).cancel();
            }
        }

        @Test
        @DisplayName("Should throw PaymentException when payment has invalid status for cancellation")
        void cancelPayment_WhenInvalidStatus_ShouldThrowPaymentException() {
            // Given
            Payment completedPayment = Payment.builder()
                    .status(PaymentStatus.COMPLETED)
                    .build();

            when(paymentRepository.findByShoppingRequestId(shoppingRequestId))
                    .thenReturn(Optional.of(completedPayment));

            // When & Then
            assertThatThrownBy(() -> paymentService.cancelPayment(shoppingRequestId))
                    .isInstanceOf(PaymentException.class)
                    .hasMessageContaining("Cannot cancel payment in status: COMPLETED");
        }
    }

    @Nested
    @DisplayName("Get Payment Tests")
    class GetPaymentTests {

        @Test
        @DisplayName("Should return payment when found")
        void getPaymentByShoppingRequestId_WhenPaymentExists_ShouldReturnPayment() {
            // Given
            Payment payment = Payment.builder()
                    .id(1L)
                    .shoppingRequestId(shoppingRequestId)
                    .build();

            when(paymentRepository.findByShoppingRequestId(shoppingRequestId))
                    .thenReturn(Optional.of(payment));

            // When
            Payment result = paymentService.getPaymentByShoppingRequestId(shoppingRequestId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getShoppingRequestId()).isEqualTo(shoppingRequestId);
        }

        @Test
        @DisplayName("Should return null when payment not found")
        void getPaymentByShoppingRequestId_WhenPaymentNotFound_ShouldReturnNull() {
            // Given
            when(paymentRepository.findByShoppingRequestId(shoppingRequestId))
                    .thenReturn(Optional.empty());

            // When
            Payment result = paymentService.getPaymentByShoppingRequestId(shoppingRequestId);

            // Then
            assertThat(result).isNull();
        }
    }
}
