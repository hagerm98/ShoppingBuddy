package com.hager.shoppingbuddy.service;

import com.hager.shoppingbuddy.dto.ChatMessageRequest;
import com.hager.shoppingbuddy.dto.ChatMessageResponse;
import com.hager.shoppingbuddy.entity.ChatMessage;
import com.hager.shoppingbuddy.entity.ShoppingRequest;
import com.hager.shoppingbuddy.entity.User;
import com.hager.shoppingbuddy.exception.ChatMessageNotFoundException;
import com.hager.shoppingbuddy.exception.ShoppingRequestNotFoundException;
import com.hager.shoppingbuddy.exception.UnauthorizedRoleException;
import com.hager.shoppingbuddy.exception.UserNotFoundException;
import com.hager.shoppingbuddy.repository.ChatMessageRepository;
import com.hager.shoppingbuddy.repository.ShoppingRequestRepository;
import com.hager.shoppingbuddy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ShoppingRequestRepository shoppingRequestRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatMessageResponse sendMessage(ChatMessageRequest request, String senderEmail)
            throws UserNotFoundException, ShoppingRequestNotFoundException, UnauthorizedRoleException {
        log.info("Sending message for shopping request ID: {} from user: {}", request.getShoppingRequestId(), senderEmail);

        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + senderEmail));

        ShoppingRequest shoppingRequest = shoppingRequestRepository.findById(request.getShoppingRequestId())
                .orElseThrow(() -> new ShoppingRequestNotFoundException("Shopping request not found with ID: " + request.getShoppingRequestId()));

        if (isNotAuthorizedToChat(sender, shoppingRequest)) {
            throw new UnauthorizedRoleException("User is not authorized to send messages in this shopping request");
        }

        ChatMessage chatMessage = ChatMessage.builder()
                .shoppingRequest(shoppingRequest)
                .sender(sender)
                .messageContent(request.getMessageContent())
                .timestamp(Instant.now())
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
        log.info("Successfully sent message with ID: {}", savedMessage.getId());

        return mapToResponse(savedMessage);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessagesForShoppingRequest(Long shoppingRequestId, String userEmail)
            throws UserNotFoundException, ShoppingRequestNotFoundException, UnauthorizedRoleException {
        log.info("Retrieving messages for shopping request ID: {} for user: {}", shoppingRequestId, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + userEmail));

        ShoppingRequest shoppingRequest = shoppingRequestRepository.findById(shoppingRequestId)
                .orElseThrow(() -> new ShoppingRequestNotFoundException("Shopping request not found with ID: " + shoppingRequestId));

        if (isNotAuthorizedToChat(user, shoppingRequest)) {
            throw new UnauthorizedRoleException("User is not authorized to view messages in this shopping request");
        }

        List<ChatMessage> messages = chatMessageRepository.findByShoppingRequestIdOrderByTimestampAsc(shoppingRequestId);
        log.info("Retrieved {} messages for shopping request ID: {}", messages.size(), shoppingRequestId);

        return messages.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChatMessageResponse getMessage(Long messageId, String userEmail)
            throws UserNotFoundException, ChatMessageNotFoundException, UnauthorizedRoleException {
        log.info("Retrieving message with ID: {} for user: {}", messageId, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + userEmail));

        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new ChatMessageNotFoundException("Chat message not found with ID: " + messageId));

        if (isNotAuthorizedToChat(user, message.getShoppingRequest())) {
            throw new UnauthorizedRoleException("User is not authorized to view this message");
        }

        return mapToResponse(message);
    }

    @Transactional(readOnly = true)
    public long getMessageCount(Long shoppingRequestId, String userEmail)
            throws UserNotFoundException, ShoppingRequestNotFoundException, UnauthorizedRoleException {
        log.info("Counting messages for shopping request ID: {} for user: {}", shoppingRequestId, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + userEmail));

        ShoppingRequest shoppingRequest = shoppingRequestRepository.findById(shoppingRequestId)
                .orElseThrow(() -> new ShoppingRequestNotFoundException("Shopping request not found with ID: " + shoppingRequestId));

        if (isNotAuthorizedToChat(user, shoppingRequest)) {
            throw new UnauthorizedRoleException("User is not authorized to view message count for this shopping request");
        }

        return chatMessageRepository.countByShoppingRequestId(shoppingRequestId);
    }

    private boolean isNotAuthorizedToChat(User user, ShoppingRequest shoppingRequest) {
        boolean isCustomer = shoppingRequest.getCustomer() != null &&
                shoppingRequest.getCustomer().getUser() != null &&
                shoppingRequest.getCustomer().getUser().getId().equals(user.getId());
        boolean isShopper = shoppingRequest.getShopper() != null &&
                shoppingRequest.getShopper().getUser() != null &&
                shoppingRequest.getShopper().getUser().getId().equals(user.getId());

        return !isCustomer && !isShopper;
    }

    private ChatMessageResponse mapToResponse(ChatMessage chatMessage) {
        String senderName = chatMessage.getSender().getFirstName() + " " + chatMessage.getSender().getLastName();

        return ChatMessageResponse.builder()
                .id(chatMessage.getId())
                .shoppingRequestId(chatMessage.getShoppingRequest().getId())
                .senderId(chatMessage.getSender().getId())
                .senderName(senderName)
                .messageContent(chatMessage.getMessageContent())
                .timestamp(chatMessage.getTimestamp())
                .build();
    }
}
