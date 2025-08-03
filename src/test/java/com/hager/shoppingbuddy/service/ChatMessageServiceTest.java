package com.hager.shoppingbuddy.service;

import com.hager.shoppingbuddy.dto.ChatMessageRequest;
import com.hager.shoppingbuddy.dto.ChatMessageResponse;
import com.hager.shoppingbuddy.entity.*;
import com.hager.shoppingbuddy.exception.ChatMessageNotFoundException;
import com.hager.shoppingbuddy.exception.ShoppingRequestNotFoundException;
import com.hager.shoppingbuddy.exception.UnauthorizedRoleException;
import com.hager.shoppingbuddy.exception.UserNotFoundException;
import com.hager.shoppingbuddy.repository.ChatMessageRepository;
import com.hager.shoppingbuddy.repository.ShoppingRequestRepository;
import com.hager.shoppingbuddy.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMessageService Tests")
class ChatMessageServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ShoppingRequestRepository shoppingRequestRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChatMessageService chatMessageService;

    private User customerUser;
    private User shopperUser;
    private User unauthorizedUser;
    private Customer customer;
    private Shopper shopper;
    private ShoppingRequest shoppingRequest;
    private ChatMessage chatMessage;
    private ChatMessageRequest chatMessageRequest;

    @BeforeEach
    void setUp() {
        customerUser = User.builder()
                .id(1L)
                .firstName("Hager")
                .lastName("Khamis")
                .email("customer@example.com")
                .build();

        shopperUser = User.builder()
                .id(2L)
                .firstName("Hadeer")
                .lastName("Mansour")
                .email("shopper@example.com")
                .build();

        unauthorizedUser = User.builder()
                .id(3L)
                .firstName("Mo")
                .lastName("Mansour")
                .email("unauthorized@example.com")
                .build();

        customer = Customer.builder()
                .id(1L)
                .user(customerUser)
                .address("123 Main St")
                .build();

        shopper = Shopper.builder()
                .id(1L)
                .user(shopperUser)
                .build();

        shoppingRequest = ShoppingRequest.builder()
                .id(1L)
                .customer(customer)
                .shopper(shopper)
                .status(ShoppingRequestStatus.IN_PROGRESS)
                .build();

        chatMessage = ChatMessage.builder()
                .id(1L)
                .shoppingRequest(shoppingRequest)
                .sender(customerUser)
                .messageContent("Hello, I need help with shopping")
                .timestamp(Instant.now())
                .build();

        chatMessageRequest = ChatMessageRequest.builder()
                .shoppingRequestId(1L)
                .messageContent("Hello, I need help with shopping")
                .build();
    }

    @Nested
    @DisplayName("Send Message Tests")
    class SendMessageTests {

        @Test
        @DisplayName("Should send message successfully when customer sends message")
        void sendMessage_WhenCustomerSendsMessage_ShouldReturnChatMessageResponse() throws Exception {
            // Given
            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
            when(shoppingRequestRepository.findById(1L)).thenReturn(Optional.of(shoppingRequest));
            when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(chatMessage);

            // When
            ChatMessageResponse response = chatMessageService.sendMessage(chatMessageRequest, "customer@example.com");

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getShoppingRequestId()).isEqualTo(1L);
            assertThat(response.getSenderId()).isEqualTo(1L);
            assertThat(response.getSenderName()).isEqualTo("Hager Khamis");
            assertThat(response.getMessageContent()).isEqualTo("Hello, I need help with shopping");
            assertThat(response.getTimestamp()).isNotNull();

            verify(userRepository).findByEmail("customer@example.com");
            verify(shoppingRequestRepository).findById(1L);
            verify(chatMessageRepository).save(any(ChatMessage.class));
        }

        @Test
        @DisplayName("Should send message successfully when shopper sends message")
        void sendMessage_WhenShopperSendsMessage_ShouldReturnChatMessageResponse() throws Exception {
            // Given
            when(userRepository.findByEmail("shopper@example.com")).thenReturn(Optional.of(shopperUser));
            when(shoppingRequestRepository.findById(1L)).thenReturn(Optional.of(shoppingRequest));

            ChatMessage shopperMessage = ChatMessage.builder()
                    .id(2L)
                    .shoppingRequest(shoppingRequest)
                    .sender(shopperUser)
                    .messageContent("I can help you with that")
                    .timestamp(Instant.now())
                    .build();

            when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(shopperMessage);

            // When
            ChatMessageResponse response = chatMessageService.sendMessage(chatMessageRequest, "shopper@example.com");

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(2L);
            assertThat(response.getSenderId()).isEqualTo(2L);
            assertThat(response.getSenderName()).isEqualTo("Hadeer Mansour");

            verify(userRepository).findByEmail("shopper@example.com");
            verify(shoppingRequestRepository).findById(1L);
            verify(chatMessageRepository).save(any(ChatMessage.class));
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user does not exist")
        void sendMessage_WhenUserNotFound_ShouldThrowUserNotFoundException() {
            // Given
            when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> chatMessageService.sendMessage(chatMessageRequest, "nonexistent@example.com"))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found with email: nonexistent@example.com");

            verify(userRepository).findByEmail("nonexistent@example.com");
            verify(shoppingRequestRepository, never()).findById(any());
            verify(chatMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ShoppingRequestNotFoundException when shopping request does not exist")
        void sendMessage_WhenShoppingRequestNotFound_ShouldThrowShoppingRequestNotFoundException() {
            // Given
            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
            when(shoppingRequestRepository.findById(1L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> chatMessageService.sendMessage(chatMessageRequest, "customer@example.com"))
                    .isInstanceOf(ShoppingRequestNotFoundException.class)
                    .hasMessage("Shopping request not found with ID: 1");

            verify(userRepository).findByEmail("customer@example.com");
            verify(shoppingRequestRepository).findById(1L);
            verify(chatMessageRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw UnauthorizedRoleException when user is not authorized")
        void sendMessage_WhenUserNotAuthorized_ShouldThrowUnauthorizedRoleException() {
            // Given
            when(userRepository.findByEmail("unauthorized@example.com")).thenReturn(Optional.of(unauthorizedUser));
            when(shoppingRequestRepository.findById(1L)).thenReturn(Optional.of(shoppingRequest));

            // When & Then
            assertThatThrownBy(() -> chatMessageService.sendMessage(chatMessageRequest, "unauthorized@example.com"))
                    .isInstanceOf(UnauthorizedRoleException.class)
                    .hasMessage("User is not authorized to send messages in this shopping request");

            verify(userRepository).findByEmail("unauthorized@example.com");
            verify(shoppingRequestRepository).findById(1L);
            verify(chatMessageRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get Messages For Shopping Request Tests")
    class GetMessagesForShoppingRequestTests {

        @Test
        @DisplayName("Should return messages when customer requests messages")
        void getMessagesForShoppingRequest_WhenCustomerRequests_ShouldReturnMessages() throws Exception {
            // Given
            ChatMessage message2 = ChatMessage.builder()
                    .id(2L)
                    .shoppingRequest(shoppingRequest)
                    .sender(shopperUser)
                    .messageContent("I can help you")
                    .timestamp(Instant.now().plusSeconds(60))
                    .build();

            List<ChatMessage> messages = Arrays.asList(chatMessage, message2);

            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
            when(shoppingRequestRepository.findById(1L)).thenReturn(Optional.of(shoppingRequest));
            when(chatMessageRepository.findByShoppingRequestIdOrderByTimestampAsc(1L)).thenReturn(messages);

            // When
            List<ChatMessageResponse> responses = chatMessageService.getMessagesForShoppingRequest(1L, "customer@example.com");

            // Then
            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).getId()).isEqualTo(1L);
            assertThat(responses.get(0).getSenderName()).isEqualTo("Hager Khamis");
            assertThat(responses.get(1).getId()).isEqualTo(2L);
            assertThat(responses.get(1).getSenderName()).isEqualTo("Hadeer Mansour");

            verify(userRepository).findByEmail("customer@example.com");
            verify(shoppingRequestRepository).findById(1L);
            verify(chatMessageRepository).findByShoppingRequestIdOrderByTimestampAsc(1L);
        }

        @Test
        @DisplayName("Should return empty list when no messages exist")
        void getMessagesForShoppingRequest_WhenNoMessages_ShouldReturnEmptyList() throws Exception {
            // Given
            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
            when(shoppingRequestRepository.findById(1L)).thenReturn(Optional.of(shoppingRequest));
            when(chatMessageRepository.findByShoppingRequestIdOrderByTimestampAsc(1L)).thenReturn(Collections.emptyList());

            // When
            List<ChatMessageResponse> responses = chatMessageService.getMessagesForShoppingRequest(1L, "customer@example.com");

            // Then
            assertThat(responses).isEmpty();

            verify(userRepository).findByEmail("customer@example.com");
            verify(shoppingRequestRepository).findById(1L);
            verify(chatMessageRepository).findByShoppingRequestIdOrderByTimestampAsc(1L);
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user does not exist")
        void getMessagesForShoppingRequest_WhenUserNotFound_ShouldThrowUserNotFoundException() {
            // Given
            when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> chatMessageService.getMessagesForShoppingRequest(1L, "nonexistent@example.com"))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found with email: nonexistent@example.com");

            verify(userRepository).findByEmail("nonexistent@example.com");
            verify(shoppingRequestRepository, never()).findById(any());
            verify(chatMessageRepository, never()).findByShoppingRequestIdOrderByTimestampAsc(any());
        }

        @Test
        @DisplayName("Should throw ShoppingRequestNotFoundException when shopping request does not exist")
        void getMessagesForShoppingRequest_WhenShoppingRequestNotFound_ShouldThrowShoppingRequestNotFoundException() {
            // Given
            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
            when(shoppingRequestRepository.findById(1L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> chatMessageService.getMessagesForShoppingRequest(1L, "customer@example.com"))
                    .isInstanceOf(ShoppingRequestNotFoundException.class)
                    .hasMessage("Shopping request not found with ID: 1");

            verify(userRepository).findByEmail("customer@example.com");
            verify(shoppingRequestRepository).findById(1L);
            verify(chatMessageRepository, never()).findByShoppingRequestIdOrderByTimestampAsc(any());
        }

        @Test
        @DisplayName("Should throw UnauthorizedRoleException when user is not authorized")
        void getMessagesForShoppingRequest_WhenUserNotAuthorized_ShouldThrowUnauthorizedRoleException() {
            // Given
            when(userRepository.findByEmail("unauthorized@example.com")).thenReturn(Optional.of(unauthorizedUser));
            when(shoppingRequestRepository.findById(1L)).thenReturn(Optional.of(shoppingRequest));

            // When & Then
            assertThatThrownBy(() -> chatMessageService.getMessagesForShoppingRequest(1L, "unauthorized@example.com"))
                    .isInstanceOf(UnauthorizedRoleException.class)
                    .hasMessage("User is not authorized to view messages in this shopping request");

            verify(userRepository).findByEmail("unauthorized@example.com");
            verify(shoppingRequestRepository).findById(1L);
            verify(chatMessageRepository, never()).findByShoppingRequestIdOrderByTimestampAsc(any());
        }
    }

    @Nested
    @DisplayName("Get Message Tests")
    class GetMessageTests {

        @Test
        @DisplayName("Should return message when customer requests specific message")
        void getMessage_WhenCustomerRequestsMessage_ShouldReturnMessage() throws Exception {
            // Given
            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
            when(chatMessageRepository.findById(1L)).thenReturn(Optional.of(chatMessage));

            // When
            ChatMessageResponse response = chatMessageService.getMessage(1L, "customer@example.com");

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getShoppingRequestId()).isEqualTo(1L);
            assertThat(response.getSenderId()).isEqualTo(1L);
            assertThat(response.getSenderName()).isEqualTo("Hager Khamis");
            assertThat(response.getMessageContent()).isEqualTo("Hello, I need help with shopping");

            verify(userRepository).findByEmail("customer@example.com");
            verify(chatMessageRepository).findById(1L);
        }

        @Test
        @DisplayName("Should return message when shopper requests specific message")
        void getMessage_WhenShopperRequestsMessage_ShouldReturnMessage() throws Exception {
            // Given
            when(userRepository.findByEmail("shopper@example.com")).thenReturn(Optional.of(shopperUser));
            when(chatMessageRepository.findById(1L)).thenReturn(Optional.of(chatMessage));

            // When
            ChatMessageResponse response = chatMessageService.getMessage(1L, "shopper@example.com");

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);

            verify(userRepository).findByEmail("shopper@example.com");
            verify(chatMessageRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user does not exist")
        void getMessage_WhenUserNotFound_ShouldThrowUserNotFoundException() {
            // Given
            when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> chatMessageService.getMessage(1L, "nonexistent@example.com"))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found with email: nonexistent@example.com");

            verify(userRepository).findByEmail("nonexistent@example.com");
            verify(chatMessageRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should throw ChatMessageNotFoundException when message does not exist")
        void getMessage_WhenMessageNotFound_ShouldThrowChatMessageNotFoundException() {
            // Given
            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
            when(chatMessageRepository.findById(1L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> chatMessageService.getMessage(1L, "customer@example.com"))
                    .isInstanceOf(ChatMessageNotFoundException.class)
                    .hasMessage("Chat message not found with ID: 1");

            verify(userRepository).findByEmail("customer@example.com");
            verify(chatMessageRepository).findById(1L);
        }

        @Test
        @DisplayName("Should throw UnauthorizedRoleException when user is not authorized")
        void getMessage_WhenUserNotAuthorized_ShouldThrowUnauthorizedRoleException() {
            // Given
            when(userRepository.findByEmail("unauthorized@example.com")).thenReturn(Optional.of(unauthorizedUser));
            when(chatMessageRepository.findById(1L)).thenReturn(Optional.of(chatMessage));

            // When & Then
            assertThatThrownBy(() -> chatMessageService.getMessage(1L, "unauthorized@example.com"))
                    .isInstanceOf(UnauthorizedRoleException.class)
                    .hasMessage("User is not authorized to view this message");

            verify(userRepository).findByEmail("unauthorized@example.com");
            verify(chatMessageRepository).findById(1L);
        }
    }

    @Nested
    @DisplayName("Get Message Count Tests")
    class GetMessageCountTests {

        @Test
        @DisplayName("Should return message count when customer requests count")
        void getMessageCount_WhenCustomerRequestsCount_ShouldReturnCount() throws Exception {
            // Given
            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
            when(shoppingRequestRepository.findById(1L)).thenReturn(Optional.of(shoppingRequest));
            when(chatMessageRepository.countByShoppingRequestId(1L)).thenReturn(5L);

            // When
            long count = chatMessageService.getMessageCount(1L, "customer@example.com");

            // Then
            assertThat(count).isEqualTo(5L);

            verify(userRepository).findByEmail("customer@example.com");
            verify(shoppingRequestRepository).findById(1L);
            verify(chatMessageRepository).countByShoppingRequestId(1L);
        }

        @Test
        @DisplayName("Should return zero when no messages exist")
        void getMessageCount_WhenNoMessages_ShouldReturnZero() throws Exception {
            // Given
            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
            when(shoppingRequestRepository.findById(1L)).thenReturn(Optional.of(shoppingRequest));
            when(chatMessageRepository.countByShoppingRequestId(1L)).thenReturn(0L);

            // When
            long count = chatMessageService.getMessageCount(1L, "customer@example.com");

            // Then
            assertThat(count).isEqualTo(0L);

            verify(userRepository).findByEmail("customer@example.com");
            verify(shoppingRequestRepository).findById(1L);
            verify(chatMessageRepository).countByShoppingRequestId(1L);
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user does not exist")
        void getMessageCount_WhenUserNotFound_ShouldThrowUserNotFoundException() {
            // Given
            when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> chatMessageService.getMessageCount(1L, "nonexistent@example.com"))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found with email: nonexistent@example.com");

            verify(userRepository).findByEmail("nonexistent@example.com");
            verify(shoppingRequestRepository, never()).findById(any());
            verify(chatMessageRepository, never()).countByShoppingRequestId(any());
        }

        @Test
        @DisplayName("Should throw ShoppingRequestNotFoundException when shopping request does not exist")
        void getMessageCount_WhenShoppingRequestNotFound_ShouldThrowShoppingRequestNotFoundException() {
            // Given
            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
            when(shoppingRequestRepository.findById(1L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> chatMessageService.getMessageCount(1L, "customer@example.com"))
                    .isInstanceOf(ShoppingRequestNotFoundException.class)
                    .hasMessage("Shopping request not found with ID: 1");

            verify(userRepository).findByEmail("customer@example.com");
            verify(shoppingRequestRepository).findById(1L);
            verify(chatMessageRepository, never()).countByShoppingRequestId(any());
        }

        @Test
        @DisplayName("Should throw UnauthorizedRoleException when user is not authorized")
        void getMessageCount_WhenUserNotAuthorized_ShouldThrowUnauthorizedRoleException() {
            // Given
            when(userRepository.findByEmail("unauthorized@example.com")).thenReturn(Optional.of(unauthorizedUser));
            when(shoppingRequestRepository.findById(1L)).thenReturn(Optional.of(shoppingRequest));

            // When & Then
            assertThatThrownBy(() -> chatMessageService.getMessageCount(1L, "unauthorized@example.com"))
                    .isInstanceOf(UnauthorizedRoleException.class)
                    .hasMessage("User is not authorized to view message count for this shopping request");

            verify(userRepository).findByEmail("unauthorized@example.com");
            verify(shoppingRequestRepository).findById(1L);
            verify(chatMessageRepository, never()).countByShoppingRequestId(any());
        }
    }

    @Nested
    @DisplayName("Authorization Edge Cases Tests")
    class AuthorizationEdgeCasesTests {

        @Test
        @DisplayName("Should allow access when shopping request has no shopper assigned yet")
        void sendMessage_WhenNoShopperAssigned_CustomerShouldStillHaveAccess() throws Exception {
            // Given
            ShoppingRequest requestWithoutShopper = ShoppingRequest.builder()
                    .id(1L)
                    .customer(customer)
                    .shopper(null)
                    .status(ShoppingRequestStatus.PENDING)
                    .build();

            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
            when(shoppingRequestRepository.findById(1L)).thenReturn(Optional.of(requestWithoutShopper));
            when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(chatMessage);

            // When
            ChatMessageResponse response = chatMessageService.sendMessage(chatMessageRequest, "customer@example.com");

            // Then
            assertThat(response).isNotNull();
            verify(chatMessageRepository).save(any(ChatMessage.class));
        }

        @Test
        @DisplayName("Should deny access when shopping request has null customer and user is not shopper")
        void sendMessage_WhenNullCustomerAndNotShopper_ShouldThrowUnauthorizedException() {
            // Given
            ShoppingRequest requestWithoutCustomer = ShoppingRequest.builder()
                    .id(1L)
                    .customer(null)
                    .shopper(shopper)
                    .status(ShoppingRequestStatus.IN_PROGRESS)
                    .build();

            when(userRepository.findByEmail("unauthorized@example.com")).thenReturn(Optional.of(unauthorizedUser));
            when(shoppingRequestRepository.findById(1L)).thenReturn(Optional.of(requestWithoutCustomer));

            // When & Then
            assertThatThrownBy(() -> chatMessageService.sendMessage(chatMessageRequest, "unauthorized@example.com"))
                    .isInstanceOf(UnauthorizedRoleException.class)
                    .hasMessage("User is not authorized to send messages in this shopping request");
        }

        @Test
        @DisplayName("Should handle null user references in customer and shopper entities")
        void sendMessage_WhenCustomerUserIsNull_ShouldThrowUnauthorizedException() {
            // Given
            Customer customerWithNullUser = Customer.builder()
                    .id(1L)
                    .user(null)
                    .build();

            ShoppingRequest requestWithNullUserCustomer = ShoppingRequest.builder()
                    .id(1L)
                    .customer(customerWithNullUser)
                    .shopper(shopper)
                    .status(ShoppingRequestStatus.IN_PROGRESS)
                    .build();

            when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
            when(shoppingRequestRepository.findById(1L)).thenReturn(Optional.of(requestWithNullUserCustomer));

            // When & Then
            assertThatThrownBy(() -> chatMessageService.sendMessage(chatMessageRequest, "customer@example.com"))
                    .isInstanceOf(UnauthorizedRoleException.class)
                    .hasMessage("User is not authorized to send messages in this shopping request");
        }
    }
}
