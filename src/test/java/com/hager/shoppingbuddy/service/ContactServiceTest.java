package com.hager.shoppingbuddy.service;

import com.hager.shoppingbuddy.dto.ContactRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContactService Tests")
class ContactServiceTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ContactService contactService;

    private ContactRequest contactRequest;
    private final String companyEmail = "company@shopping-buddy.shop";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(contactService, "companyEmail", companyEmail);

        contactRequest = ContactRequest.builder()
                .name("Hager Khamis")
                .email("hager.khamis@example.com")
                .subject("Product Inquiry")
                .message("I have a question about your service.")
                .build();
    }

    @Nested
    @DisplayName("Process Contact Form Tests")
    class ProcessContactFormTests {

        @Test
        @DisplayName("Should process contact form successfully")
        void processContactForm_WhenValidRequest_ShouldSendEmail() {
            // Given
            doNothing().when(emailService).send(any(String.class), any(String.class), any(String.class));

            // When
            contactService.processContactForm(contactRequest);

            // Then
            verify(emailService).send(
                    eq(companyEmail),
                    eq("Contact Form Submission: Product Inquiry"),
                    any(String.class)
            );
        }

        @Test
        @DisplayName("Should build correct email body with proper formatting")
        void processContactForm_WhenValidRequest_ShouldBuildCorrectEmailBody() {
            // Given
            doNothing().when(emailService).send(any(String.class), any(String.class), any(String.class));

            // When
            contactService.processContactForm(contactRequest);

            // Then
            verify(emailService).send(
                    eq(companyEmail),
                    eq("Contact Form Submission: Product Inquiry"),
                    argThat(emailBody ->
                        emailBody.contains("Hager Khamis") &&
                        emailBody.contains("hager.khamis@example.com") &&
                        emailBody.contains("Product Inquiry") &&
                        emailBody.contains("I have a question about your service.") &&
                        emailBody.contains("<html>") &&
                        emailBody.contains("</html>") &&
                        emailBody.contains("New Contact Form Submission")
                    )
            );
        }

        @Test
        @DisplayName("Should handle message with line breaks correctly")
        void processContactForm_WhenMessageContainsLineBreaks_ShouldConvertToBrTags() {
            // Given
            ContactRequest requestWithLineBreaks = ContactRequest.builder()
                    .name("Hadeer Mansour")
                    .email("hadeer@example.com")
                    .subject("Multi-line Message")
                    .message("Line 1\nLine 2\nLine 3")
                    .build();

            doNothing().when(emailService).send(any(String.class), any(String.class), any(String.class));

            // When
            contactService.processContactForm(requestWithLineBreaks);

            // Then
            verify(emailService).send(
                    eq(companyEmail),
                    eq("Contact Form Submission: Multi-line Message"),
                    argThat(emailBody ->
                        emailBody.contains("Line 1<br>Line 2<br>Line 3")
                    )
            );
        }

        @Test
        @DisplayName("Should handle empty subject correctly")
        void processContactForm_WhenEmptySubject_ShouldHandleGracefully() {
            // Given
            ContactRequest requestWithEmptySubject = ContactRequest.builder()
                    .name("Test User")
                    .email("test@example.com")
                    .subject("")
                    .message("Test message")
                    .build();

            doNothing().when(emailService).send(any(String.class), any(String.class), any(String.class));

            // When
            contactService.processContactForm(requestWithEmptySubject);

            // Then
            verify(emailService).send(
                    eq(companyEmail),
                    eq("Contact Form Submission: "),
                    any(String.class)
            );
        }

        @Test
        @DisplayName("Should throw RuntimeException when email service fails")
        void processContactForm_WhenEmailServiceFails_ShouldThrowRuntimeException() {
            // Given
            doThrow(new RuntimeException("Email service unavailable"))
                    .when(emailService).send(any(String.class), any(String.class), any(String.class));

            // When & Then
            assertThatThrownBy(() -> contactService.processContactForm(contactRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Failed to process contact form submission");

            verify(emailService).send(
                    eq(companyEmail),
                    eq("Contact Form Submission: Product Inquiry"),
                    any(String.class)
            );
        }

        @Test
        @DisplayName("Should throw RuntimeException when email service throws IllegalStateException")
        void processContactForm_WhenEmailServiceThrowsIllegalStateException_ShouldThrowRuntimeException() {
            // Given
            doThrow(new IllegalStateException("Failed to send email"))
                    .when(emailService).send(any(String.class), any(String.class), any(String.class));

            // When & Then
            assertThatThrownBy(() -> contactService.processContactForm(contactRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Failed to process contact form submission");
        }

        @Test
        @DisplayName("Should handle special characters in contact form")
        void processContactForm_WhenSpecialCharactersInForm_ShouldHandleCorrectly() {
            // Given
            ContactRequest requestWithSpecialChars = ContactRequest.builder()
                    .name("Mo Mansour")
                    .email("mo@example.com")
                    .subject("Café & Résumé")
                    .message("Special chars: àáâãäåæçèéêë & <script>alert('test')</script>")
                    .build();

            doNothing().when(emailService).send(any(String.class), any(String.class), any(String.class));

            // When
            contactService.processContactForm(requestWithSpecialChars);

            // Then
            verify(emailService).send(
                    eq(companyEmail),
                    eq("Contact Form Submission: Café & Résumé"),
                    argThat(emailBody ->
                        emailBody.contains("Mo Mansour") &&
                        emailBody.contains("Special chars: àáâãäåæçèéêë & <script>alert('test')</script>")
                    )
            );
        }

        @Test
        @DisplayName("Should handle very long message correctly")
        void processContactForm_WhenVeryLongMessage_ShouldHandleCorrectly() {
            // Given
            String longMessage = "A".repeat(5000);
            ContactRequest requestWithLongMessage = ContactRequest.builder()
                    .name("Test User")
                    .email("test@example.com")
                    .subject("Long Message Test")
                    .message(longMessage)
                    .build();

            doNothing().when(emailService).send(any(String.class), any(String.class), any(String.class));

            // When
            contactService.processContactForm(requestWithLongMessage);

            // Then
            verify(emailService).send(
                    eq(companyEmail),
                    eq("Contact Form Submission: Long Message Test"),
                    argThat(emailBody -> emailBody.contains(longMessage))
            );
        }
    }
}
