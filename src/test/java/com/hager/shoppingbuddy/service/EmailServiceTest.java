package com.hager.shoppingbuddy.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Tests")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailService emailService;

    private final String toEmail = "test@example.com";
    private final String subject = "Test Subject";
    private final String emailContent = "<html><body>Test email content</body></html>";

    @BeforeEach
    void setUp() {
        String fromEmail = "noreply@shopping-buddy.shop";
        ReflectionTestUtils.setField(emailService, "fromEmail", fromEmail);
    }

    @Nested
    @DisplayName("Send Email Tests")
    class SendEmailTests {

        @Test
        @DisplayName("Should send email successfully")
        void send_WhenValidParameters_ShouldSendEmail() {
            // Given
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doNothing().when(mailSender).send(mimeMessage);

            // When
            emailService.send(toEmail, subject, emailContent);

            // Then
            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should send plain text email successfully")
        void send_WhenPlainTextEmail_ShouldSendEmail() {
            // Given
            String plainTextContent = "This is plain text email content";
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doNothing().when(mailSender).send(mimeMessage);

            // When
            emailService.send(toEmail, subject, plainTextContent);

            // Then
            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should handle empty subject")
        void send_WhenEmptySubject_ShouldSendEmail() {
            // Given
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doNothing().when(mailSender).send(mimeMessage);

            // When
            emailService.send(toEmail, "", emailContent);

            // Then
            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should handle empty email content")
        void send_WhenEmptyContent_ShouldSendEmail() {
            // Given
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doNothing().when(mailSender).send(mimeMessage);

            // When
            emailService.send(toEmail, subject, "");

            // Then
            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should handle single recipient email")
        void send_WhenSingleRecipient_ShouldSendEmail() {
            // Given
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doNothing().when(mailSender).send(mimeMessage);

            // When
            emailService.send(toEmail, subject, emailContent);

            // Then
            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should handle special characters in email content")
        void send_WhenSpecialCharactersInContent_ShouldSendEmail() {
            // Given
            String specialContent = "<html><body>Special chars: àáâãäåæçèéêë & symbols: €£¥</body></html>";
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doNothing().when(mailSender).send(mimeMessage);

            // When
            emailService.send(toEmail, subject, specialContent);

            // Then
            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should handle very long email content")
        void send_WhenVeryLongContent_ShouldSendEmail() {
            // Given
            String longContent = "<html><body>" + "A".repeat(10000) + "</body></html>";
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doNothing().when(mailSender).send(mimeMessage);

            // When
            emailService.send(toEmail, subject, longContent);

            // Then
            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should handle subject with special characters")
        void send_WhenSubjectWithSpecialChars_ShouldSendEmail() {
            // Given
            String specialSubject = "Test Subject with àáâãäåæçèéêë & symbols: €£¥";
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doNothing().when(mailSender).send(mimeMessage);

            // When
            emailService.send(toEmail, specialSubject, emailContent);

            // Then
            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should throw IllegalStateException when MessagingException occurs")
        void send_WhenMessagingExceptionOccurs_ShouldThrowIllegalStateException() {
            // Given
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doAnswer(_ -> {
                throw new MessagingException("Mail server error");
            }).when(mailSender).send(mimeMessage);

            // When & Then
            assertThatThrownBy(() -> emailService.send(toEmail, subject, emailContent))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Failed to send email");

            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should propagate RuntimeException when occurs during send")
        void send_WhenRuntimeExceptionDuringSend_ShouldPropagateException() {
            // Given
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doThrow(new RuntimeException("Mail server unavailable"))
                    .when(mailSender).send(mimeMessage);

            // When & Then
            assertThatThrownBy(() -> emailService.send(toEmail, subject, emailContent))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Mail server unavailable");

            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should propagate RuntimeException when MimeMessage creation fails")
        void send_WhenMimeMessageCreationFails_ShouldPropagateException() {
            // Given
            when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Failed to create message"));

            // When & Then
            assertThatThrownBy(() -> emailService.send(toEmail, subject, emailContent))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Failed to create message");

            verify(mailSender).createMimeMessage();
            verify(mailSender, never()).send(any(MimeMessage.class));
        }
    }

    @Nested
    @DisplayName("Validation Error Tests")
    class ValidationErrorTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException when recipient is null")
        void send_WhenNullRecipient_ShouldThrowIllegalArgumentException() {
            // Given
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // When & Then
            assertThatThrownBy(() -> emailService.send(null, subject, emailContent))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("To address must not be null");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when subject is null")
        void send_WhenNullSubject_ShouldThrowIllegalArgumentException() {
            // Given
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // When & Then
            assertThatThrownBy(() -> emailService.send(toEmail, null, emailContent))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Subject must not be null");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when email content is null")
        void send_WhenNullContent_ShouldThrowIllegalArgumentException() {
            // Given
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // When & Then
            assertThatThrownBy(() -> emailService.send(toEmail, subject, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Text must not be null");
        }

        @Test
        @DisplayName("Should handle invalid email format gracefully")
        void send_WhenInvalidEmailFormat_ShouldSendEmailIfPossible() {
            // Given - Invalid format may still be sent if mail server accepts it
            String invalidEmail = "invalid-email-format";
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doNothing().when(mailSender).send(mimeMessage);

            // When
            emailService.send(invalidEmail, subject, emailContent);

            // Then
            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should throw IllegalStateException when recipient is empty")
        void send_WhenEmptyRecipient_ShouldThrowIllegalStateException() {
            // Given
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

            // When & Then - Empty recipient causes email send to fail
            assertThatThrownBy(() -> emailService.send("", subject, emailContent))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Failed to send email");
        }

        @Test
        @DisplayName("Should handle very long subject")
        void send_WhenVeryLongSubject_ShouldSendEmail() {
            // Given
            String longSubject = "A".repeat(500) + " - Test Subject";
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doNothing().when(mailSender).send(mimeMessage);

            // When
            emailService.send(toEmail, longSubject, emailContent);

            // Then
            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
        }

        @Test
        @DisplayName("Should handle recipient with spaces")
        void send_WhenRecipientWithSpaces_ShouldSendEmail() {
            // Given
            String emailWithSpaces = " test@example.com ";
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
            doNothing().when(mailSender).send(mimeMessage);

            // When
            emailService.send(emailWithSpaces, subject, emailContent);

            // Then
            verify(mailSender).createMimeMessage();
            verify(mailSender).send(mimeMessage);
        }
    }
}
