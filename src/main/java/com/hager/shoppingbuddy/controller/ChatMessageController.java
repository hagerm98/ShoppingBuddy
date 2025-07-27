package com.hager.shoppingbuddy.controller;

import com.hager.shoppingbuddy.dto.ChatMessageRequest;
import com.hager.shoppingbuddy.dto.ChatMessageResponse;
import com.hager.shoppingbuddy.exception.ChatMessageNotFoundException;
import com.hager.shoppingbuddy.exception.ShoppingRequestNotFoundException;
import com.hager.shoppingbuddy.exception.UnauthorizedRoleException;
import com.hager.shoppingbuddy.exception.UserNotFoundException;
import com.hager.shoppingbuddy.service.ChatMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @PostMapping
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @Valid @RequestBody ChatMessageRequest request,
            Authentication authentication
    ) throws UserNotFoundException, UnauthorizedRoleException, ShoppingRequestNotFoundException {
        log.info("Sending message for shopping request ID: {} from user: {}",
                request.getShoppingRequestId(), authentication.getName());

        ChatMessageResponse response = chatMessageService.sendMessage(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/shopping-request/{shoppingRequestId}")
    public ResponseEntity<List<ChatMessageResponse>> getMessagesForShoppingRequest(
            @PathVariable Long shoppingRequestId,
            Authentication authentication
    ) throws UserNotFoundException, UnauthorizedRoleException, ShoppingRequestNotFoundException {
        log.info("Retrieving messages for shopping request ID: {} for user: {}",
                shoppingRequestId, authentication.getName());

        List<ChatMessageResponse> messages = chatMessageService.getMessagesForShoppingRequest(
                shoppingRequestId, authentication.getName());
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/{messageId}")
    public ResponseEntity<ChatMessageResponse> getMessage(
            @PathVariable Long messageId,
            Authentication authentication
    ) throws UserNotFoundException, ChatMessageNotFoundException, UnauthorizedRoleException {
        log.info("Retrieving message with ID: {} for user: {}", messageId, authentication.getName());

        ChatMessageResponse message = chatMessageService.getMessage(messageId, authentication.getName());
        return ResponseEntity.ok(message);
    }

    @GetMapping("/count/shopping-request/{shoppingRequestId}")
    public ResponseEntity<Long> getMessageCount(
            @PathVariable Long shoppingRequestId,
            Authentication authentication
    ) throws UserNotFoundException, UnauthorizedRoleException, ShoppingRequestNotFoundException {
        log.info("Getting message count for shopping request ID: {} for user: {}",
                shoppingRequestId, authentication.getName());

        long count = chatMessageService.getMessageCount(shoppingRequestId, authentication.getName());
        return ResponseEntity.ok(count);
    }
}
