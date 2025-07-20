package com.hager.shoppingbuddy.service;

import com.hager.shoppingbuddy.dto.ContactRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactService {

    private final EmailService emailService;

    @Value("${shoppingbuddy.email}")
    private String companyEmail;

    public void processContactForm(ContactRequest contactRequest) {
        log.info("Processing contact form submission from: {}", contactRequest.getEmail());

        String emailSubject = "Contact Form Submission: " + contactRequest.getSubject();
        String emailBody = buildContactEmailBody(contactRequest);

        try {
            emailService.send(companyEmail, emailSubject, emailBody);
            log.info("Contact form email sent successfully to {}", companyEmail);
        } catch (Exception e) {
            log.error("Failed to send contact form email: {}", e.getMessage());
            throw new RuntimeException("Failed to process contact form submission");
        }
    }

    private String buildContactEmailBody(ContactRequest contactRequest) {
        return String.format("""
            <html>
            <body>
                <h2>New Contact Form Submission</h2>
                <p><strong>From:</strong> %s (%s)</p>
                <p><strong>Subject:</strong> %s</p>
                <hr>
                <h3>Message:</h3>
                <p>%s</p>
                <hr>
                <p><em>This message was sent through the Shopping Buddy contact form.</em></p>
            </body>
            </html>
            """,
            contactRequest.getName(),
            contactRequest.getEmail(),
            contactRequest.getSubject(),
            contactRequest.getMessage().replace("\n", "<br>")
        );
    }
}
