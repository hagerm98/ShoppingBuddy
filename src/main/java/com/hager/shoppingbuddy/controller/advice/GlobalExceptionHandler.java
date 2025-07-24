package com.hager.shoppingbuddy.controller.advice;

import com.hager.shoppingbuddy.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PasswordChangeException.class)
    public ResponseEntity<String> handlePasswordChangeException(PasswordChangeException ex) {
        log.error("PasswordChangeException: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> handleUserNotFoundException(UserNotFoundException ex) {
        log.error("UserNotFoundException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<String> handleEmailAlreadyExistsException(EmailAlreadyExistsException ex) {
        log.error("EmailAlreadyExistsException: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public RedirectView handleException(RuntimeException ex) {
        log.error("RuntimeException: {}", ex.getMessage());
        return createErrorRedirect(ex.getMessage(), "We encountered an unexpected issue while processing your request. Please try again, and if the problem continues, contact our support team.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public RedirectView handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((singleError) -> {
            String fieldName = ((FieldError) singleError).getField();
            String errorMessage = singleError.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn("Validation errors: {}", errors);

        String errorDetails = errors.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(", "));

        return createErrorRedirect("Please check the information you entered and correct any errors highlighted below.", errorDetails);
    }

    @ExceptionHandler(IllegalStateException.class)
    public RedirectView handleIllegalStateException(IllegalStateException ex) {
        log.error("IllegalStateException: {}", ex.getMessage());
        return createErrorRedirect(ex.getMessage(), "The system is currently unable to process this request due to its current state. Please try again later or contact support if this issue persists.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public RedirectView handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("IllegalArgumentException: {}", ex.getMessage());
        return createErrorRedirect(ex.getMessage(), "The information provided is not valid for this operation. Please check your input and try again.");
    }

    @ExceptionHandler(ShoppingBuddyException.class)
    public RedirectView handleShoppingBuddyException(ShoppingBuddyException ex) {
        log.error("ShoppingBuddyException: {}", ex.getMessage());
        return createErrorRedirect(
                ex.getClass().getSimpleName(),
                ex.getMessage()
        );
    }

    @ExceptionHandler(Exception.class)
    public RedirectView handleGenericException(Exception ex) {
        log.error("Unexpected error: ", ex);
        return createErrorRedirect(
                "We're sorry, but something went wrong on our end. ",
                ex.getClass().getSimpleName() + ": " + ex.getMessage()
        );
    }

    private RedirectView createErrorRedirect(String errorMessage, String errorDetails) {
        String redirectUrl = UriComponentsBuilder.fromPath("/error-page")
                .queryParam("errorMessage", errorMessage)
                .queryParam("errorDetails", errorDetails)
                .toUriString();

        return new RedirectView(redirectUrl);
    }
}
