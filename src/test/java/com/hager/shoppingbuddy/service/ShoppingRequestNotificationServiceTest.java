package com.hager.shoppingbuddy.service;

import com.hager.shoppingbuddy.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShoppingRequestNotificationService Tests")
class ShoppingRequestNotificationServiceTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ShoppingRequestNotificationService notificationService;

    private Customer customer;
    private ShoppingRequest shoppingRequest;

    @BeforeEach
    void setUp() {
        String baseUrl = "https://shopping-buddy.shop";
        ReflectionTestUtils.setField(notificationService, "baseUrl", baseUrl);

        User customerUser = User.builder()
                .id(1L)
                .firstName("Hager")
                .lastName("Khamis")
                .email("customer@example.com")
                .build();

        User shopperUser = User.builder()
                .id(2L)
                .firstName("Hadeer")
                .lastName("Mansour")
                .email("shopper@example.com")
                .build();

        customer = Customer.builder()
                .id(1L)
                .user(customerUser)
                .address("123 Main Street, Dublin")
                .build();

        Shopper shopper = Shopper.builder()
                .id(1L)
                .user(shopperUser)
                .build();

        shoppingRequest = ShoppingRequest.builder()
                .id(100L)
                .customer(customer)
                .shopper(shopper)
                .deliveryAddress("123 Main Street, Dublin")
                .estimatedItemsPrice(50.00)
                .deliveryFee(5.00)
                .status(ShoppingRequestStatus.PENDING)
                .build();
    }

    @Nested
    @DisplayName("Notify Shopping Request Created Tests")
    class NotifyShoppingRequestCreatedTests {

        @Test
        @DisplayName("Should send notification when shopping request is created")
        void notifyShoppingRequestCreated_WhenValidRequest_ShouldSendEmail() {
            // Given
            doNothing().when(emailService).send(anyString(), anyString(), anyString());

            // When
            notificationService.notifyShoppingRequestCreated(shoppingRequest);

            // Then
            verify(emailService).send(
                    eq("customer@example.com"),
                    eq("Shopping Request Created - #100"),
                    argThat(emailBody ->
                            emailBody.contains("Hager") &&
                            emailBody.contains("#100") &&
                            emailBody.contains("123 Main Street, Dublin") &&
                            emailBody.contains("50.00") &&
                            emailBody.contains("5.00") &&
                            emailBody.contains("PENDING") &&
                            emailBody.contains("shopping-requests/100")
                    )
            );
        }

        @Test
        @DisplayName("Should handle email service failure gracefully")
        void notifyShoppingRequestCreated_WhenEmailServiceFails_ShouldNotThrow() {
            // Given
            doThrow(new RuntimeException("Email service unavailable"))
                    .when(emailService).send(anyString(), anyString(), anyString());

            // When & Then - should not throw exception
            notificationService.notifyShoppingRequestCreated(shoppingRequest);

            verify(emailService).send(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should handle request with null customer gracefully")
        void notifyShoppingRequestCreated_WhenNullCustomer_ShouldNotThrow() {
            // Given
            ShoppingRequest requestWithNullCustomer = ShoppingRequest.builder()
                    .id(100L)
                    .customer(null)
                    .build();

            // When & Then - should not throw exception
            notificationService.notifyShoppingRequestCreated(requestWithNullCustomer);

            verify(emailService, never()).send(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Notify Shopping Request Accepted Tests")
    class NotifyShoppingRequestAcceptedTests {

        @Test
        @DisplayName("Should send notifications to both customer and shopper when request is accepted")
        void notifyShoppingRequestAccepted_WhenValidRequest_ShouldSendBothEmails() {
            // Given
            doNothing().when(emailService).send(anyString(), anyString(), anyString());

            // When
            notificationService.notifyShoppingRequestAccepted(shoppingRequest);

            // Then
            verify(emailService, times(2)).send(anyString(), anyString(), anyString());

            // Verify customer email
            verify(emailService).send(
                    eq("customer@example.com"),
                    eq("Your Shopping Request Has Been Accepted - #100"),
                    argThat(emailBody ->
                            emailBody.contains("Hager") &&
                            emailBody.contains("Hadeer Mansour") &&
                            emailBody.contains("shopper@example.com")
                    )
            );

            // Verify shopper email
            verify(emailService).send(
                    eq("shopper@example.com"),
                    eq("Shopping Request Accepted - #100"),
                    argThat(emailBody ->
                            emailBody.contains("Hadeer") &&
                            emailBody.contains("Hager Khamis") &&
                            emailBody.contains("customer@example.com")
                    )
            );
        }

        @Test
        @DisplayName("Should handle partial email failure gracefully")
        void notifyShoppingRequestAccepted_WhenOneEmailFails_ShouldContinue() {
            // Given
            doNothing().when(emailService).send(eq("customer@example.com"), anyString(), anyString());
            doThrow(new RuntimeException("Email failed"))
                    .when(emailService).send(eq("shopper@example.com"), anyString(), anyString());

            // When & Then - should not throw exception
            notificationService.notifyShoppingRequestAccepted(shoppingRequest);

            verify(emailService, times(2)).send(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Notify Shopping Started Tests")
    class NotifyShoppingStartedTests {

        @Test
        @DisplayName("Should send notification to customer when shopping starts")
        void notifyShoppingStarted_WhenValidRequest_ShouldSendEmail() {
            // Given
            doNothing().when(emailService).send(anyString(), anyString(), anyString());

            // When
            notificationService.notifyShoppingStarted(shoppingRequest);

            // Then
            verify(emailService).send(
                    eq("customer@example.com"),
                    eq("Shopping Started for Your Request - #100"),
                    argThat(emailBody ->
                            emailBody.contains("Hager") &&
                            emailBody.contains("Hadeer Mansour has started shopping") &&
                            emailBody.contains("Shopping in Progress")
                    )
            );
        }

        @Test
        @DisplayName("Should handle null shopper gracefully")
        void notifyShoppingStarted_WhenNullShopper_ShouldNotThrow() {
            // Given
            ShoppingRequest requestWithNullShopper = ShoppingRequest.builder()
                    .id(100L)
                    .customer(customer)
                    .shopper(null)
                    .build();

            // When & Then - should not throw exception
            notificationService.notifyShoppingStarted(requestWithNullShopper);

            verify(emailService, never()).send(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Notify Shopping Completed Tests")
    class NotifyShoppingCompletedTests {

        @Test
        @DisplayName("Should send notifications to both customer and shopper when shopping is completed")
        void notifyShoppingCompleted_WhenValidRequest_ShouldSendBothEmails() {
            // Given
            doNothing().when(emailService).send(anyString(), anyString(), anyString());

            // When
            notificationService.notifyShoppingCompleted(shoppingRequest);

            // Then
            verify(emailService, times(2)).send(anyString(), anyString(), anyString());

            // Verify customer email
            verify(emailService).send(
                    eq("customer@example.com"),
                    eq("Your Shopping Has Been Completed - #100"),
                    argThat(emailBody ->
                            emailBody.contains("Hager") &&
                            emailBody.contains("Hadeer Mansour has completed shopping") &&
                            emailBody.contains("Shopping Completed")
                    )
            );

            // Verify shopper email
            verify(emailService).send(
                    eq("shopper@example.com"),
                    eq("Shopping Completed - #100"),
                    argThat(emailBody ->
                            emailBody.contains("Hadeer") &&
                            emailBody.contains("Hager Khamis") &&
                            emailBody.contains("Well Done!")
                    )
            );
        }
    }

    @Nested
    @DisplayName("Notify Shopping Request Cancelled Tests")
    class NotifyShoppingRequestCancelledTests {

        @Test
        @DisplayName("Should send notification to shopper when customer cancels")
        void notifyShoppingRequestCancelled_WhenCustomerCancels_ShouldNotifyShopperOnly() {
            // Given
            doNothing().when(emailService).send(anyString(), anyString(), anyString());

            // When
            notificationService.notifyShoppingRequestCancelled(shoppingRequest, "customer@example.com");

            // Then
            verify(emailService, times(1)).send(anyString(), anyString(), anyString());

            verify(emailService).send(
                    eq("shopper@example.com"),
                    eq("Shopping Request Cancelled - #100"),
                    argThat(emailBody ->
                            emailBody.contains("Hadeer") &&
                            emailBody.contains("cancelled by the customer") &&
                            emailBody.contains("Hager Khamis (customer@example.com)")
                    )
            );
        }

        @Test
        @DisplayName("Should send notification to customer when shopper cancels")
        void notifyShoppingRequestCancelled_WhenShopperCancels_ShouldNotifyCustomerOnly() {
            // Given
            doNothing().when(emailService).send(anyString(), anyString(), anyString());

            // When
            notificationService.notifyShoppingRequestCancelled(shoppingRequest, "shopper@example.com");

            // Then
            verify(emailService, times(1)).send(anyString(), anyString(), anyString());

            verify(emailService).send(
                    eq("customer@example.com"),
                    eq("Shopping Request Cancelled - #100"),
                    argThat(emailBody ->
                            emailBody.contains("Hager") &&
                            emailBody.contains("cancelled by the shopper") &&
                            emailBody.contains("Hadeer Mansour (shopper@example.com)")
                    )
            );
        }

        @Test
        @DisplayName("Should send notifications to both parties when cancelled by third party")
        void notifyShoppingRequestCancelled_WhenThirdPartyCancels_ShouldNotifyBoth() {
            // Given
            doNothing().when(emailService).send(anyString(), anyString(), anyString());

            // When
            notificationService.notifyShoppingRequestCancelled(shoppingRequest, "admin@example.com");

            // Then
            verify(emailService, times(2)).send(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should handle request with no shopper assigned")
        void notifyShoppingRequestCancelled_WhenNoShopperAssigned_ShouldNotifyCustomerOnly() {
            // Given
            ShoppingRequest requestWithoutShopper = ShoppingRequest.builder()
                    .id(100L)
                    .customer(customer)
                    .shopper(null)
                    .build();

            doNothing().when(emailService).send(anyString(), anyString(), anyString());

            // When
            notificationService.notifyShoppingRequestCancelled(requestWithoutShopper, "admin@example.com");

            // Then
            verify(emailService, times(1)).send(anyString(), anyString(), anyString());

            verify(emailService).send(
                    eq("customer@example.com"),
                    eq("Shopping Request Cancelled - #100"),
                    anyString()
            );
        }
    }

    @Nested
    @DisplayName("Notify Shopping Request Updated Tests")
    class NotifyShoppingRequestUpdatedTests {

        @Test
        @DisplayName("Should send notification to shopper when request is updated")
        void notifyShoppingRequestUpdated_WhenValidRequest_ShouldSendEmail() {
            // Given
            doNothing().when(emailService).send(anyString(), anyString(), anyString());

            // When
            notificationService.notifyShoppingRequestUpdated(shoppingRequest);

            // Then
            verify(emailService).send(
                    eq("shopper@example.com"),
                    eq("Shopping Request Updated - #100"),
                    argThat(emailBody ->
                            emailBody.contains("Hadeer") &&
                            emailBody.contains("Hager Khamis has updated shopping request") &&
                            emailBody.contains("Request Updated")
                    )
            );
        }

        @Test
        @DisplayName("Should not send notification when no shopper assigned")
        void notifyShoppingRequestUpdated_WhenNoShopper_ShouldNotSendEmail() {
            // Given
            ShoppingRequest requestWithoutShopper = ShoppingRequest.builder()
                    .id(100L)
                    .customer(customer)
                    .shopper(null)
                    .build();

            // When
            notificationService.notifyShoppingRequestUpdated(requestWithoutShopper);

            // Then
            verify(emailService, never()).send(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Notify Shopping Request Abandoned Tests")
    class NotifyShoppingRequestAbandonedTests {

        @Test
        @DisplayName("Should send notification to customer when shopper abandons request")
        void notifyShoppingRequestAbandoned_WhenShopperAbandons_ShouldSendEmail() {
            // Given
            doNothing().when(emailService).send(anyString(), anyString(), anyString());

            // When
            notificationService.notifyShoppingRequestAbandoned(shoppingRequest, "shopper@example.com");

            // Then
            verify(emailService).send(
                    eq("customer@example.com"),
                    eq("Shopping Request Available Again - #100"),
                    argThat(emailBody ->
                            emailBody.contains("Hager") &&
                            emailBody.contains("shopper@example.com") &&
                            emailBody.contains("Request Abandoned") &&
                            emailBody.contains("123 Main Street, Dublin") &&
                            emailBody.contains("50.00") &&
                            emailBody.contains("5.00") &&
                            emailBody.contains("shopping-requests/100")
                    )
            );
        }

        @Test
        @DisplayName("Should handle email service failure gracefully")
        void notifyShoppingRequestAbandoned_WhenEmailServiceFails_ShouldNotThrow() {
            // Given
            doThrow(new RuntimeException("Email service unavailable"))
                    .when(emailService).send(anyString(), anyString(), anyString());

            // When & Then - should not throw exception
            notificationService.notifyShoppingRequestAbandoned(shoppingRequest, "shopper@example.com");

            verify(emailService).send(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should handle request with null customer gracefully")
        void notifyShoppingRequestAbandoned_WhenNullCustomer_ShouldNotThrow() {
            // Given
            ShoppingRequest requestWithNullCustomer = ShoppingRequest.builder()
                    .id(100L)
                    .customer(null)
                    .build();

            // When & Then - should not throw exception
            notificationService.notifyShoppingRequestAbandoned(requestWithNullCustomer, "shopper@example.com");

            verify(emailService, never()).send(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should send email with correct content when request has PENDING status")
        void notifyShoppingRequestAbandoned_WhenRequestIsPending_ShouldIncludeCorrectStatus() {
            // Given
            ShoppingRequest pendingRequest = ShoppingRequest.builder()
                    .id(100L)
                    .customer(customer)
                    .deliveryAddress("123 Main Street, Dublin")
                    .estimatedItemsPrice(50.00)
                    .deliveryFee(5.00)
                    .status(ShoppingRequestStatus.PENDING)
                    .build();

            doNothing().when(emailService).send(anyString(), anyString(), anyString());

            // When
            notificationService.notifyShoppingRequestAbandoned(pendingRequest, "shopper@example.com");

            // Then
            verify(emailService).send(
                    eq("customer@example.com"),
                    eq("Shopping Request Available Again - #100"),
                    argThat(emailBody ->
                            emailBody.contains("PENDING") &&
                            emailBody.contains("now open for other shoppers")
                    )
            );
        }

        @Test
        @DisplayName("Should handle different shopper email formats")
        void notifyShoppingRequestAbandoned_WhenDifferentShopperEmail_ShouldIncludeInMessage() {
            // Given
            String shopperEmail = "different.shopper@test.com";
            doNothing().when(emailService).send(anyString(), anyString(), anyString());

            // When
            notificationService.notifyShoppingRequestAbandoned(shoppingRequest, shopperEmail);

            // Then
            verify(emailService).send(
                    eq("customer@example.com"),
                    eq("Shopping Request Available Again - #100"),
                    argThat(emailBody -> emailBody.contains(shopperEmail))
            );
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    class EdgeCasesAndErrorHandlingTests {

        @Test
        @DisplayName("Should handle null customer user gracefully")
        void notifyShoppingRequestCreated_WhenNullCustomerUser_ShouldNotThrow() {
            // Given
            Customer customerWithNullUser = Customer.builder()
                    .id(1L)
                    .user(null)
                    .build();

            ShoppingRequest requestWithNullUser = ShoppingRequest.builder()
                    .id(100L)
                    .customer(customerWithNullUser)
                    .build();

            // When & Then - should not throw exception
            notificationService.notifyShoppingRequestCreated(requestWithNullUser);

            verify(emailService, never()).send(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should handle null shopper user gracefully")
        void notifyShoppingRequestAccepted_WhenNullShopperUser_ShouldNotThrow() {
            // Given
            Shopper shopperWithNullUser = Shopper.builder()
                    .id(1L)
                    .user(null)
                    .build();

            ShoppingRequest requestWithNullShopperUser = ShoppingRequest.builder()
                    .id(100L)
                    .customer(customer)
                    .shopper(shopperWithNullUser)
                    .build();

            // When & Then - should not throw exception
            notificationService.notifyShoppingRequestAccepted(requestWithNullShopperUser);

            verify(emailService, never()).send(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should handle special characters in user names")
        void notifyShoppingRequestCreated_WhenSpecialCharactersInNames_ShouldHandleCorrectly() {
            // Given
            User userWithSpecialChars = User.builder()
                    .id(1L)
                    .firstName("José María")
                    .lastName("O'Connor-Smith")
                    .email("jose@example.com")
                    .build();

            Customer customerWithSpecialChars = Customer.builder()
                    .id(1L)
                    .user(userWithSpecialChars)
                    .build();

            ShoppingRequest requestWithSpecialChars = ShoppingRequest.builder()
                    .id(100L)
                    .customer(customerWithSpecialChars)
                    .deliveryAddress("123 Main Street, Dublin")
                    .estimatedItemsPrice(50.00)
                    .deliveryFee(5.00)
                    .status(ShoppingRequestStatus.PENDING)
                    .build();

            doNothing().when(emailService).send(anyString(), anyString(), anyString());

            // When
            notificationService.notifyShoppingRequestCreated(requestWithSpecialChars);

            // Then
            verify(emailService).send(
                    eq("jose@example.com"),
                    anyString(),
                    argThat(emailBody -> emailBody.contains("José María"))
            );
        }

        @Test
        @DisplayName("Should handle zero amounts in shopping request")
        void notifyShoppingRequestCreated_WhenZeroAmounts_ShouldHandleCorrectly() {
            // Given
            ShoppingRequest requestWithZeroAmounts = ShoppingRequest.builder()
                    .id(100L)
                    .customer(customer)
                    .deliveryAddress("123 Main Street, Dublin")
                    .estimatedItemsPrice(0.00)
                    .deliveryFee(0.00)
                    .status(ShoppingRequestStatus.PENDING)
                    .build();

            doNothing().when(emailService).send(anyString(), anyString(), anyString());

            // When
            notificationService.notifyShoppingRequestCreated(requestWithZeroAmounts);

            // Then
            verify(emailService).send(
                    eq("customer@example.com"),
                    anyString(),
                    argThat(emailBody ->
                            emailBody.contains("0.00")
                    )
            );
        }

        @Test
        @DisplayName("Should handle very long delivery addresses")
        void notifyShoppingRequestCreated_WhenLongAddress_ShouldHandleCorrectly() {
            // Given
            String longAddress = "A".repeat(200) + " Street, Dublin, Ireland";
            ShoppingRequest requestWithLongAddress = ShoppingRequest.builder()
                    .id(100L)
                    .customer(customer)
                    .deliveryAddress(longAddress)
                    .estimatedItemsPrice(50.00)
                    .deliveryFee(5.00)
                    .status(ShoppingRequestStatus.PENDING)
                    .build();

            doNothing().when(emailService).send(anyString(), anyString(), anyString());

            // When
            notificationService.notifyShoppingRequestCreated(requestWithLongAddress);

            // Then
            verify(emailService).send(
                    eq("customer@example.com"),
                    anyString(),
                    argThat(emailBody -> emailBody.contains(longAddress))
            );
        }

        @Test
        @DisplayName("Should handle large monetary amounts correctly")
        void notifyShoppingRequestCreated_WhenLargeAmounts_ShouldFormatCorrectly() {
            // Given
            ShoppingRequest requestWithLargeAmounts = ShoppingRequest.builder()
                    .id(100L)
                    .customer(customer)
                    .deliveryAddress("123 Main Street, Dublin")
                    .estimatedItemsPrice(999999.99)
                    .deliveryFee(99.99)
                    .status(ShoppingRequestStatus.PENDING)
                    .build();

            doNothing().when(emailService).send(anyString(), anyString(), anyString());

            // When
            notificationService.notifyShoppingRequestCreated(requestWithLargeAmounts);

            // Then
            verify(emailService).send(
                    eq("customer@example.com"),
                    anyString(),
                    argThat(emailBody ->
                            emailBody.contains("999999.99") &&
                            emailBody.contains("99.99")
                    )
            );
        }
    }
}
