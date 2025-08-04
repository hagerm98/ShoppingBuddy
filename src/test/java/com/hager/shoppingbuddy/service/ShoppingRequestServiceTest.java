package com.hager.shoppingbuddy.service;

import com.hager.shoppingbuddy.dto.*;
import com.hager.shoppingbuddy.entity.*;
import com.hager.shoppingbuddy.exception.*;
import com.hager.shoppingbuddy.repository.*;
import com.google.maps.model.LatLng;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShoppingRequestService Tests")
class ShoppingRequestServiceTest {

    @Mock
    private ShoppingRequestRepository shoppingRequestRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ShopperRepository shopperRepository;

    @Mock
    private GeocodingService geocodingService;

    @Mock
    private ShoppingRequestNotificationService notificationService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private ShopperService shopperService;

    @InjectMocks
    private ShoppingRequestService shoppingRequestService;

    private final String customerEmail = "customer@example.com";
    private final String shopperEmail = "shopper@example.com";
    private final Long requestId = 1L;
    private final Long customerId = 2L;
    private final Long shopperId = 3L;

    @Nested
    @DisplayName("Create Shopping Request Tests")
    class CreateShoppingRequestTests {

        @Test
        @DisplayName("Should create shopping request successfully")
        void createShoppingRequest_WhenValidRequest_ShouldCreateRequest() throws CustomerNotFoundException, PaymentException {
            // Given
            ShoppingRequestCreateRequest request = createValidCreateRequest();
            Customer customer = createTestCustomer();
            LatLng location = new LatLng(53.3498, -6.2603);
            ShoppingRequest savedRequest = createTestShoppingRequest();

            when(customerRepository.findByUserEmail(customerEmail)).thenReturn(Optional.of(customer));
            when(geocodingService.getLatLngFromAddress(anyString())).thenReturn(location);
            when(shoppingRequestRepository.save(any(ShoppingRequest.class))).thenReturn(savedRequest);
            doNothing().when(paymentService).createPaymentIntent(anyLong(), anyLong(), anyDouble());
            doNothing().when(notificationService).notifyShoppingRequestCreated(any(ShoppingRequest.class));

            // When
            ShoppingRequestResponse result = shoppingRequestService.createShoppingRequest(customerEmail, request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(requestId);
            assertThat(result.getStatus()).isEqualTo(ShoppingRequestStatus.PENDING);
            assertThat(result.getDeliveryAddress()).isEqualTo("123 Test Street, Dublin");
            assertThat(result.getLatitude()).isEqualTo(53.3498);
            assertThat(result.getLongitude()).isEqualTo(-6.2603);

            verify(shoppingRequestRepository).save(argThat(sr ->
                sr.getCustomer().equals(customer) &&
                sr.getStatus() == ShoppingRequestStatus.PENDING &&
                sr.getPaymentStatus() == PaymentStatus.PENDING &&
                sr.getLatitude() == 53.3498 &&
                sr.getLongitude() == -6.2603
            ));
            verify(paymentService).createPaymentIntent(requestId, customer.getUser().getId(), 75.0);
            verify(notificationService).notifyShoppingRequestCreated(savedRequest);
        }

        @Test
        @DisplayName("Should throw CustomerNotFoundException when customer not found")
        void createShoppingRequest_WhenCustomerNotFound_ShouldThrowCustomerNotFoundException() throws PaymentException {
            // Given
            ShoppingRequestCreateRequest request = createValidCreateRequest();
            when(customerRepository.findByUserEmail(customerEmail)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> shoppingRequestService.createShoppingRequest(customerEmail, request))
                    .isInstanceOf(CustomerNotFoundException.class)
                    .hasMessageContaining("Customer not found with email");

            verify(shoppingRequestRepository, never()).save(any(ShoppingRequest.class));
            verify(paymentService, never()).createPaymentIntent(anyLong(), anyLong(), anyDouble());
        }
    }

    @Nested
    @DisplayName("Get Shopping Requests Tests")
    class GetShoppingRequestsTests {

        @Test
        @DisplayName("Should get all pending requests with authorized payment")
        void getAllPendingRequests_ShouldReturnAuthorizedPendingRequests() {
            // Given
            List<ShoppingRequest> pendingRequests = Collections.singletonList(createTestShoppingRequest());
            when(shoppingRequestRepository.findByStatusAndPaymentStatusOrderByCreatedAtDesc(
                    ShoppingRequestStatus.PENDING, PaymentStatus.AUTHORIZED))
                    .thenReturn(pendingRequests);
            when(paymentService.getPaymentByShoppingRequestId(anyLong())).thenReturn(createTestPayment());

            // When
            List<ShoppingRequestResponse> result = shoppingRequestService.getAllPendingRequests();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getStatus()).isEqualTo(ShoppingRequestStatus.PENDING);
            assertThat(result.getFirst().getPaymentStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
        }

        @Test
        @DisplayName("Should get shopping request by ID")
        void getShoppingRequestById_WhenRequestExists_ShouldReturnRequest() throws ShoppingRequestNotFoundException {
            // Given
            ShoppingRequest request = createTestShoppingRequest();
            when(shoppingRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
            when(paymentService.getPaymentByShoppingRequestId(requestId)).thenReturn(createTestPayment());

            // When
            ShoppingRequestResponse result = shoppingRequestService.getShoppingRequestById(requestId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(requestId);
        }

        @Test
        @DisplayName("Should throw ShoppingRequestNotFoundException when request not found")
        void getShoppingRequestById_WhenRequestNotFound_ShouldThrowShoppingRequestNotFoundException() {
            // Given
            when(shoppingRequestRepository.findById(requestId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> shoppingRequestService.getShoppingRequestById(requestId))
                    .isInstanceOf(ShoppingRequestNotFoundException.class)
                    .hasMessageContaining("Shopping request not found with ID");
        }

        @Test
        @DisplayName("Should get customer shopping requests")
        void getCustomerShoppingRequests_WhenCustomerExists_ShouldReturnRequests() throws CustomerNotFoundException {
            // Given
            Customer customer = createTestCustomer();
            List<ShoppingRequest> requests = Collections.singletonList(createTestShoppingRequest());

            when(customerRepository.findByUserEmail(customerEmail)).thenReturn(Optional.of(customer));
            when(shoppingRequestRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)).thenReturn(requests);
            when(paymentService.getPaymentByShoppingRequestId(anyLong())).thenReturn(createTestPayment());

            // When
            List<ShoppingRequestResponse> result = shoppingRequestService.getCustomerShoppingRequests(customerEmail);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getCustomerId()).isEqualTo(customer.getUser().getId());
        }

        @Test
        @DisplayName("Should get shopper shopping requests")
        void getShopperShoppingRequests_WhenShopperExists_ShouldReturnRequests() throws ShopperNotFoundException {
            // Given
            Shopper shopper = createTestShopper();
            List<ShoppingRequest> requests = Collections.singletonList(createTestShoppingRequest());

            when(shopperRepository.findByUserEmail(shopperEmail)).thenReturn(Optional.of(shopper));
            when(shoppingRequestRepository.findByShopperIdOrderByCreatedAtDesc(shopperId)).thenReturn(requests);
            when(paymentService.getPaymentByShoppingRequestId(anyLong())).thenReturn(createTestPayment());

            // When
            List<ShoppingRequestResponse> result = shoppingRequestService.getShopperShoppingRequests(shopperEmail);

            // Then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Accept Shopping Request Tests")
    class AcceptShoppingRequestTests {

        @Test
        @DisplayName("Should accept shopping request successfully")
        void acceptShoppingRequest_WhenValidRequest_ShouldAcceptRequest() throws ShopperNotFoundException, InvalidShoppingRequestActionException, ShoppingRequestNotFoundException {
            // Given
            Shopper shopper = createTestShopper();
            ShoppingRequest request = createTestShoppingRequest();
            request.setStatus(ShoppingRequestStatus.PENDING);
            request.setPaymentStatus(PaymentStatus.AUTHORIZED);

            ShoppingRequest acceptedRequest = createTestShoppingRequest();
            acceptedRequest.setShopper(shopper);
            acceptedRequest.setStatus(ShoppingRequestStatus.ACCEPTED);

            when(shopperRepository.findByUserEmail(shopperEmail)).thenReturn(Optional.of(shopper));
            when(shoppingRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
            when(shoppingRequestRepository.save(any(ShoppingRequest.class))).thenReturn(acceptedRequest);
            doNothing().when(notificationService).notifyShoppingRequestAccepted(any(ShoppingRequest.class));

            // When
            ShoppingRequestResponse result = shoppingRequestService.acceptShoppingRequest(requestId, shopperEmail);

            // Then
            assertThat(result.getStatus()).isEqualTo(ShoppingRequestStatus.ACCEPTED);
            assertThat(result.getShopperId()).isEqualTo(shopper.getUser().getId());

            verify(shoppingRequestRepository).save(argThat(sr ->
                sr.getShopper().equals(shopper) &&
                sr.getStatus() == ShoppingRequestStatus.ACCEPTED
            ));
            verify(notificationService).notifyShoppingRequestAccepted(acceptedRequest);
        }

        @Test
        @DisplayName("Should throw InvalidShoppingRequestActionException when request not pending")
        void acceptShoppingRequest_WhenRequestNotPending_ShouldThrowInvalidShoppingRequestActionException() {
            // Given
            Shopper shopper = createTestShopper();
            ShoppingRequest request = createTestShoppingRequest();
            request.setStatus(ShoppingRequestStatus.COMPLETED);

            when(shopperRepository.findByUserEmail(shopperEmail)).thenReturn(Optional.of(shopper));
            when(shoppingRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

            // When & Then
            assertThatThrownBy(() -> shoppingRequestService.acceptShoppingRequest(requestId, shopperEmail))
                    .isInstanceOf(InvalidShoppingRequestActionException.class)
                    .hasMessageContaining("Shopping request is not in PENDING status");
        }

        @Test
        @DisplayName("Should throw InvalidShoppingRequestActionException when payment not authorized")
        void acceptShoppingRequest_WhenPaymentNotAuthorized_ShouldThrowInvalidShoppingRequestActionException() {
            // Given
            Shopper shopper = createTestShopper();
            ShoppingRequest request = createTestShoppingRequest();
            request.setStatus(ShoppingRequestStatus.PENDING);
            request.setPaymentStatus(PaymentStatus.PENDING);

            when(shopperRepository.findByUserEmail(shopperEmail)).thenReturn(Optional.of(shopper));
            when(shoppingRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

            // When & Then
            assertThatThrownBy(() -> shoppingRequestService.acceptShoppingRequest(requestId, shopperEmail))
                    .isInstanceOf(InvalidShoppingRequestActionException.class)
                    .hasMessageContaining("Shopping request payment must be authorized before acceptance");
        }
    }

    @Nested
    @DisplayName("Start Shopping Tests")
    class StartShoppingTests {

        @Test
        @DisplayName("Should start shopping successfully")
        void startShopping_WhenValidRequest_ShouldStartShopping() throws InvalidShoppingRequestActionException, ShopperNotFoundException, ShoppingRequestNotFoundException {
            // Given
            ShoppingRequest request = createTestShoppingRequest();
            request.setStatus(ShoppingRequestStatus.ACCEPTED);
            request.setShopper(createTestShopper());

            ShoppingRequest inProgressRequest = createTestShoppingRequest();
            inProgressRequest.setShopper(createTestShopper());
            inProgressRequest.setStatus(ShoppingRequestStatus.IN_PROGRESS);

            when(shopperRepository.findByUserEmail(shopperEmail)).thenReturn(Optional.of(createTestShopper()));
            when(shoppingRequestRepository.findByIdAndShopperId(requestId, shopperId)).thenReturn(Optional.of(request));
            when(shoppingRequestRepository.save(any(ShoppingRequest.class))).thenReturn(inProgressRequest);
            doNothing().when(notificationService).notifyShoppingStarted(any(ShoppingRequest.class));

            // When
            ShoppingRequestResponse result = shoppingRequestService.startShopping(requestId, shopperEmail);

            // Then
            assertThat(result.getStatus()).isEqualTo(ShoppingRequestStatus.IN_PROGRESS);
            verify(shoppingRequestRepository).save(argThat(sr ->
                sr.getStatus() == ShoppingRequestStatus.IN_PROGRESS
            ));
            verify(notificationService).notifyShoppingStarted(inProgressRequest);
        }

        @Test
        @DisplayName("Should throw InvalidShoppingRequestActionException when request not accepted")
        void startShopping_WhenRequestNotAccepted_ShouldThrowInvalidShoppingRequestActionException() {
            // Given
            ShoppingRequest request = createTestShoppingRequest();
            request.setStatus(ShoppingRequestStatus.PENDING);

            when(shopperRepository.findByUserEmail(shopperEmail)).thenReturn(Optional.of(createTestShopper()));
            when(shoppingRequestRepository.findByIdAndShopperId(requestId, shopperId)).thenReturn(Optional.of(request));

            // When & Then
            assertThatThrownBy(() -> shoppingRequestService.startShopping(requestId, shopperEmail))
                    .isInstanceOf(InvalidShoppingRequestActionException.class)
                    .hasMessageContaining("Shopping request must be ACCEPTED to start shopping");
        }
    }

    @Nested
    @DisplayName("Complete Shopping Tests")
    class CompleteShoppingTests {

        @Test
        @DisplayName("Should complete shopping successfully")
        void completeShopping_WhenValidRequest_ShouldCompleteShopping() throws InvalidShoppingRequestActionException, ShopperNotFoundException, ShoppingRequestNotFoundException, PaymentException {
            // Given
            ShoppingRequest request = createTestShoppingRequest();
            request.setStatus(ShoppingRequestStatus.IN_PROGRESS);
            request.setShopper(createTestShopper());

            ShoppingRequest completedRequest = createTestShoppingRequest();
            completedRequest.setShopper(createTestShopper());
            completedRequest.setStatus(ShoppingRequestStatus.COMPLETED);
            completedRequest.setPaymentStatus(PaymentStatus.COMPLETED);

            Payment capturedPayment = createTestPayment();
            capturedPayment.setStatus(PaymentStatus.COMPLETED);

            when(shopperRepository.findByUserEmail(shopperEmail)).thenReturn(Optional.of(createTestShopper()));
            when(shoppingRequestRepository.findByIdAndShopperId(requestId, shopperId)).thenReturn(Optional.of(request));
            when(shoppingRequestRepository.save(any(ShoppingRequest.class))).thenReturn(completedRequest);
            when(paymentService.capturePayment(requestId)).thenReturn(capturedPayment);
            doNothing().when(shopperService).addToBalanceById(eq(shopperId), any(BigDecimal.class));
            doNothing().when(notificationService).notifyShoppingCompleted(any(ShoppingRequest.class));

            // When
            ShoppingRequestResponse result = shoppingRequestService.completeShopping(requestId, shopperEmail);

            // Then
            assertThat(result.getStatus()).isEqualTo(ShoppingRequestStatus.COMPLETED);
            verify(paymentService).capturePayment(requestId);
            verify(shopperService).addToBalanceById(eq(shopperId), any(BigDecimal.class));
            verify(notificationService).notifyShoppingCompleted(completedRequest);
        }

        @Test
        @DisplayName("Should throw InvalidShoppingRequestActionException when request not in progress")
        void completeShopping_WhenRequestNotInProgress_ShouldThrowInvalidShoppingRequestActionException() {
            // Given
            ShoppingRequest request = createTestShoppingRequest();
            request.setStatus(ShoppingRequestStatus.ACCEPTED);

            when(shopperRepository.findByUserEmail(shopperEmail)).thenReturn(Optional.of(createTestShopper()));
            when(shoppingRequestRepository.findByIdAndShopperId(requestId, shopperId)).thenReturn(Optional.of(request));

            // When & Then
            assertThatThrownBy(() -> shoppingRequestService.completeShopping(requestId, shopperEmail))
                    .isInstanceOf(InvalidShoppingRequestActionException.class)
                    .hasMessageContaining("Can only complete IN_PROGRESS requests");
        }

        @Test
        @DisplayName("Should throw InvalidShoppingRequestActionException when payment capture fails")
        void completeShopping_WhenPaymentCaptureFails_ShouldThrowInvalidShoppingRequestActionException() throws PaymentException {
            // Given
            ShoppingRequest request = createTestShoppingRequest();
            request.setStatus(ShoppingRequestStatus.IN_PROGRESS);
            request.setShopper(createTestShopper());

            when(shopperRepository.findByUserEmail(shopperEmail)).thenReturn(Optional.of(createTestShopper()));
            when(shoppingRequestRepository.findByIdAndShopperId(requestId, shopperId)).thenReturn(Optional.of(request));
            when(shoppingRequestRepository.save(any(ShoppingRequest.class))).thenReturn(request);
            when(paymentService.capturePayment(requestId)).thenThrow(new PaymentException("Payment capture failed"));

            // When & Then
            assertThatThrownBy(() -> shoppingRequestService.completeShopping(requestId, shopperEmail))
                    .isInstanceOf(InvalidShoppingRequestActionException.class)
                    .hasMessageContaining("Failed to capture payment for completed shopping request");
        }
    }

    @Nested
    @DisplayName("Abandon Shopping Request Tests")
    class AbandonShoppingRequestTests {

        @Test
        @DisplayName("Should abandon accepted shopping request successfully")
        void abandonShoppingRequest_WhenAcceptedRequest_ShouldAbandonRequest() throws InvalidShoppingRequestActionException, ShopperNotFoundException, ShoppingRequestNotFoundException {
            // Given
            Shopper shopper = createTestShopper();
            ShoppingRequest request = createTestShoppingRequest();
            request.setStatus(ShoppingRequestStatus.ACCEPTED);
            request.setShopper(shopper);

            ShoppingRequest abandonedRequest = createTestShoppingRequest();
            abandonedRequest.setStatus(ShoppingRequestStatus.PENDING);
            abandonedRequest.setShopper(null);

            when(shopperRepository.findByUserEmail(shopperEmail)).thenReturn(Optional.of(shopper));
            when(shoppingRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
            when(shoppingRequestRepository.save(any(ShoppingRequest.class))).thenReturn(abandonedRequest);
            doNothing().when(notificationService).notifyShoppingRequestAbandoned(any(ShoppingRequest.class), eq(shopperEmail));

            // When
            ShoppingRequestResponse result = shoppingRequestService.abandonShoppingRequest(requestId, shopperEmail);

            // Then
            assertThat(result.getStatus()).isEqualTo(ShoppingRequestStatus.PENDING);
            assertThat(result.getShopperId()).isNull();
            verify(shoppingRequestRepository).save(argThat(sr ->
                sr.getShopper() == null &&
                sr.getStatus() == ShoppingRequestStatus.PENDING
            ));
            verify(notificationService).notifyShoppingRequestAbandoned(abandonedRequest, shopperEmail);
        }

        @Test
        @DisplayName("Should abandon in-progress shopping request successfully")
        void abandonShoppingRequest_WhenInProgressRequest_ShouldAbandonRequest() throws InvalidShoppingRequestActionException, ShopperNotFoundException, ShoppingRequestNotFoundException {
            // Given
            Shopper shopper = createTestShopper();
            ShoppingRequest request = createTestShoppingRequest();
            request.setStatus(ShoppingRequestStatus.IN_PROGRESS);
            request.setShopper(shopper);

            when(shopperRepository.findByUserEmail(shopperEmail)).thenReturn(Optional.of(shopper));
            when(shoppingRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
            when(shoppingRequestRepository.save(any(ShoppingRequest.class))).thenReturn(request);

            // When
            shoppingRequestService.abandonShoppingRequest(requestId, shopperEmail);

            // Then
            verify(shoppingRequestRepository).save(argThat(sr ->
                sr.getShopper() == null &&
                sr.getStatus() == ShoppingRequestStatus.PENDING
            ));
        }

        @Test
        @DisplayName("Should throw InvalidShoppingRequestActionException when request not assigned to shopper")
        void abandonShoppingRequest_WhenRequestNotAssignedToShopper_ShouldThrowInvalidShoppingRequestActionException() {
            // Given
            Shopper shopper = createTestShopper();
            Shopper otherShopper = createTestShopper();
            otherShopper.setId(99L);

            ShoppingRequest request = createTestShoppingRequest();
            request.setShopper(otherShopper);

            when(shopperRepository.findByUserEmail(shopperEmail)).thenReturn(Optional.of(shopper));
            when(shoppingRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

            // When & Then
            assertThatThrownBy(() -> shoppingRequestService.abandonShoppingRequest(requestId, shopperEmail))
                    .isInstanceOf(InvalidShoppingRequestActionException.class)
                    .hasMessageContaining("Shopping request is not assigned to this shopper");
        }

        @Test
        @DisplayName("Should throw InvalidShoppingRequestActionException when request has invalid status")
        void abandonShoppingRequest_WhenInvalidStatus_ShouldThrowInvalidShoppingRequestActionException() {
            // Given
            Shopper shopper = createTestShopper();
            ShoppingRequest request = createTestShoppingRequest();
            request.setStatus(ShoppingRequestStatus.COMPLETED);
            request.setShopper(shopper);

            when(shopperRepository.findByUserEmail(shopperEmail)).thenReturn(Optional.of(shopper));
            when(shoppingRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

            // When & Then
            assertThatThrownBy(() -> shoppingRequestService.abandonShoppingRequest(requestId, shopperEmail))
                    .isInstanceOf(InvalidShoppingRequestActionException.class)
                    .hasMessageContaining("Can only abandon ACCEPTED or IN_PROGRESS shopping requests");
        }
    }

    @Nested
    @DisplayName("Cancel Shopping Request Tests")
    class CancelShoppingRequestTests {

        @Test
        @DisplayName("Should cancel shopping request successfully")
        void cancelShoppingRequest_WhenValidCustomerRequest_ShouldCancelRequest() throws InvalidShoppingRequestActionException, ShoppingRequestNotFoundException, PaymentException {
            // Given
            ShoppingRequest request = createTestShoppingRequest();
            request.setStatus(ShoppingRequestStatus.PENDING);

            ShoppingRequest cancelledRequest = createTestShoppingRequest();
            cancelledRequest.setStatus(ShoppingRequestStatus.CANCELLED);
            cancelledRequest.setPaymentStatus(PaymentStatus.CANCELLED);

            Payment cancelledPayment = createTestPayment();
            cancelledPayment.setStatus(PaymentStatus.CANCELLED);

            when(shoppingRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
            when(shoppingRequestRepository.save(any(ShoppingRequest.class))).thenReturn(cancelledRequest);
            when(paymentService.cancelPayment(requestId)).thenReturn(cancelledPayment);
            doNothing().when(notificationService).notifyShoppingRequestCancelled(any(ShoppingRequest.class), eq(customerEmail));

            // When
            ShoppingRequestResponse result = shoppingRequestService.cancelShoppingRequest(requestId, customerEmail);

            // Then
            assertThat(result.getStatus()).isEqualTo(ShoppingRequestStatus.CANCELLED);
            verify(paymentService).cancelPayment(requestId);
            verify(shoppingRequestRepository).save(argThat(sr ->
                sr.getStatus() == ShoppingRequestStatus.CANCELLED &&
                sr.getPaymentStatus() == PaymentStatus.CANCELLED
            ));
            verify(notificationService).notifyShoppingRequestCancelled(cancelledRequest, customerEmail);
        }

        @Test
        @DisplayName("Should throw InvalidShoppingRequestActionException when non-customer tries to cancel")
        void cancelShoppingRequest_WhenNonCustomerTriesToCancel_ShouldThrowInvalidShoppingRequestActionException() {
            // Given
            ShoppingRequest request = createTestShoppingRequest();
            when(shoppingRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

            // When & Then
            assertThatThrownBy(() -> shoppingRequestService.cancelShoppingRequest(requestId, "other@example.com"))
                    .isInstanceOf(InvalidShoppingRequestActionException.class)
                    .hasMessageContaining("Only customers can cancel shopping requests");
        }

        @Test
        @DisplayName("Should throw InvalidShoppingRequestActionException when trying to cancel completed request")
        void cancelShoppingRequest_WhenRequestCompleted_ShouldThrowInvalidShoppingRequestActionException() {
            // Given
            ShoppingRequest request = createTestShoppingRequest();
            request.setStatus(ShoppingRequestStatus.COMPLETED);
            when(shoppingRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

            // When & Then
            assertThatThrownBy(() -> shoppingRequestService.cancelShoppingRequest(requestId, customerEmail))
                    .isInstanceOf(InvalidShoppingRequestActionException.class)
                    .hasMessageContaining("Cannot cancel a COMPLETED request");
        }
    }

    @Nested
    @DisplayName("Update Shopping Request Tests")
    class UpdateShoppingRequestTests {

        @Test
        @DisplayName("Should update shopping request successfully")
        void updateShoppingRequest_WhenValidRequest_ShouldUpdateRequest() throws CustomerNotFoundException, ShoppingRequestNotFoundException, InvalidShoppingRequestActionException {
            // Given
            Customer customer = createTestCustomer();
            ShoppingRequest request = createTestShoppingRequest();
            request.setStatus(ShoppingRequestStatus.PENDING);
            request.setCustomer(customer);

            ShoppingRequestUpdateRequest updateRequest = createValidUpdateRequest();
            LatLng newLocation = new LatLng(53.4084, -8.2439);

            when(customerRepository.findByUserEmail(customerEmail)).thenReturn(Optional.of(customer));
            when(shoppingRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
            when(geocodingService.getLatLngFromAddress("456 Updated Street, Cork")).thenReturn(newLocation);
            when(shoppingRequestRepository.save(any(ShoppingRequest.class))).thenReturn(request);
            doNothing().when(notificationService).notifyShoppingRequestUpdated(any(ShoppingRequest.class));

            // When
            ShoppingRequestResponse result = shoppingRequestService.updateShoppingRequest(requestId, customerEmail, updateRequest);

            // Then
            verify(shoppingRequestRepository).save(argThat(sr ->
                sr.getDeliveryAddress().equals("456 Updated Street, Cork") &&
                sr.getEstimatedItemsPrice() == 60.0 &&
                sr.getDeliveryFee() == 12.0 &&
                sr.getLatitude() == 53.4084 &&
                sr.getLongitude() == -8.2439
            ));
            verify(notificationService).notifyShoppingRequestUpdated(request);
        }

        @Test
        @DisplayName("Should throw InvalidShoppingRequestActionException when customer not authorized")
        void updateShoppingRequest_WhenCustomerNotAuthorized_ShouldThrowInvalidShoppingRequestActionException() {
            // Given
            Customer customer = createTestCustomer();
            Customer otherCustomer = createTestCustomer();
            otherCustomer.setId(99L);

            ShoppingRequest request = createTestShoppingRequest();
            request.setCustomer(otherCustomer);

            when(customerRepository.findByUserEmail(customerEmail)).thenReturn(Optional.of(customer));
            when(shoppingRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

            // When & Then
            assertThatThrownBy(() -> shoppingRequestService.updateShoppingRequest(requestId, customerEmail, createValidUpdateRequest()))
                    .isInstanceOf(InvalidShoppingRequestActionException.class)
                    .hasMessageContaining("Customer not authorized to edit this request");
        }

        @Test
        @DisplayName("Should throw InvalidShoppingRequestActionException when request not pending")
        void updateShoppingRequest_WhenRequestNotPending_ShouldThrowInvalidShoppingRequestActionException() {
            // Given
            Customer customer = createTestCustomer();
            ShoppingRequest request = createTestShoppingRequest();
            request.setStatus(ShoppingRequestStatus.ACCEPTED);
            request.setCustomer(customer);

            when(customerRepository.findByUserEmail(customerEmail)).thenReturn(Optional.of(customer));
            when(shoppingRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

            // When & Then
            assertThatThrownBy(() -> shoppingRequestService.updateShoppingRequest(requestId, customerEmail, createValidUpdateRequest()))
                    .isInstanceOf(InvalidShoppingRequestActionException.class)
                    .hasMessageContaining("Can only edit shopping requests in PENDING status");
        }
    }

    @Nested
    @DisplayName("Update Payment Status Tests")
    class UpdatePaymentStatusTests {

        @Test
        @DisplayName("Should update payment status successfully")
        void updatePaymentStatus_WhenValidRequest_ShouldUpdatePaymentStatus() throws ShoppingRequestNotFoundException {
            // Given
            ShoppingRequest request = createTestShoppingRequest();
            when(shoppingRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
            when(shoppingRequestRepository.save(any(ShoppingRequest.class))).thenReturn(request);

            // When
            shoppingRequestService.updatePaymentStatus(requestId, PaymentStatus.AUTHORIZED);

            // Then
            verify(shoppingRequestRepository).save(argThat(sr ->
                sr.getPaymentStatus() == PaymentStatus.AUTHORIZED &&
                sr.getUpdatedAt() != null
            ));
        }

        @Test
        @DisplayName("Should throw ShoppingRequestNotFoundException when request not found")
        void updatePaymentStatus_WhenRequestNotFound_ShouldThrowShoppingRequestNotFoundException() {
            // Given
            when(shoppingRequestRepository.findById(requestId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> shoppingRequestService.updatePaymentStatus(requestId, PaymentStatus.AUTHORIZED))
                    .isInstanceOf(ShoppingRequestNotFoundException.class)
                    .hasMessageContaining("Shopping request not found with ID");
        }
    }

    // Helper methods
    private ShoppingRequestCreateRequest createValidCreateRequest() {
        return ShoppingRequestCreateRequest.builder()
                .deliveryAddress("123 Test Street, Dublin")
                .estimatedItemsPrice(65.0)
                .deliveryFee(10.0)
                .items(Arrays.asList(
                        ItemRequest.builder()
                                .name("Milk")
                                .description("2L Fresh Milk")
                                .amount(2)
                                .category("Dairy")
                                .build(),
                        ItemRequest.builder()
                                .name("Bread")
                                .description("Whole grain bread")
                                .amount(1)
                                .category("Bakery")
                                .build()
                ))
                .build();
    }

    private ShoppingRequestUpdateRequest createValidUpdateRequest() {
        return ShoppingRequestUpdateRequest.builder()
                .deliveryAddress("456 Updated Street, Cork")
                .estimatedItemsPrice(60.0)
                .deliveryFee(12.0)
                .items(Collections.singletonList(
                        ItemRequest.builder()
                                .name("Updated Milk")
                                .description("3L Fresh Milk")
                                .amount(3)
                                .category("Dairy")
                                .build()
                ))
                .build();
    }

    private Customer createTestCustomer() {
        User customerUser = User.builder()
                .id(customerId)
                .email(customerEmail)
                .firstName("John")
                .lastName("Customer")
                .build();

        return Customer.builder()
                .id(customerId)
                .user(customerUser)
                .build();
    }

    private Shopper createTestShopper() {
        User shopperUser = User.builder()
                .id(shopperId)
                .email(shopperEmail)
                .firstName("Jane")
                .lastName("Shopper")
                .build();

        return Shopper.builder()
                .id(shopperId)
                .user(shopperUser)
                .balance(BigDecimal.valueOf(100.0))
                .build();
    }

    private ShoppingRequest createTestShoppingRequest() {
        return ShoppingRequest.builder()
                .id(requestId)
                .customer(createTestCustomer())
                .status(ShoppingRequestStatus.PENDING)
                .deliveryAddress("123 Test Street, Dublin")
                .estimatedItemsPrice(65.0)
                .deliveryFee(10.0)
                .paymentStatus(PaymentStatus.AUTHORIZED)
                .latitude(53.3498)
                .longitude(-6.2603)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .items(new ArrayList<>(Collections.singletonList(
                        Item.builder()
                                .id(1L)
                                .name("Milk")
                                .description("2L Fresh Milk")
                                .amount(2)
                                .category("Dairy")
                                .build()
                )))
                .build();
    }

    private Payment createTestPayment() {
        return Payment.builder()
                .id(1L)
                .shoppingRequestId(requestId)
                .customerId(customerId)
                .amount(BigDecimal.valueOf(75.0))
                .status(PaymentStatus.AUTHORIZED)
                .stripePaymentIntentId("pi_test_123")
                .stripeClientSecret("pi_test_123_secret_456")
                .createdTimestamp(Instant.now())
                .build();
    }
}
