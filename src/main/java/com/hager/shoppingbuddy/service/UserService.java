package com.hager.shoppingbuddy.service;

import com.hager.shoppingbuddy.dto.RegistrationRequest;
import com.hager.shoppingbuddy.entity.Token;
import com.hager.shoppingbuddy.entity.User;
import com.hager.shoppingbuddy.entity.UserRole;
import com.hager.shoppingbuddy.repository.TokenRepository;
import com.hager.shoppingbuddy.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@AllArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final static String USER_NOT_FOUND_MESSAGE = "User with email %s not found";

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email).orElseThrow(() ->
            new UsernameNotFoundException(String.format(USER_NOT_FOUND_MESSAGE, email)));
    }

    public String signUpUser(User user) {
        boolean userExists = userRepository.findByEmail(user.getEmail()).isPresent();
        if (userExists) {
            throw new IllegalStateException("Email already taken");
        }

        String encodedPassword = bCryptPasswordEncoder.encode(user.getPassword());

        user.setPasswordHash(encodedPassword);
        userRepository.save(user);

        String token = UUID.randomUUID().toString();
        Token confirmationToken = Token.builder()
                .token(token)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60 * 60 * 24)) // 24 hours
                .user(user)
                .build();

        tokenRepository.save(confirmationToken);
        return token;
    }

    public void enableUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException(String.format(USER_NOT_FOUND_MESSAGE, email)));

        user.setEnabled(true);
        userRepository.save(user);
    }

    public String register(RegistrationRequest request) {
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(request.getPassword())
                .role(UserRole.CUSTOMER)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .isEnabled(false)
                .isLocked(false)
                .lastPasswordChange(Instant.now())
                .build();

        String token = signUpUser(user);
        String confirmationLink = "http://localhost:8080/registration/confirmation?token=" + token;

        emailService.send(request.getEmail(), buildEmail(request.getFirstName(), confirmationLink));

        return token;
    }

    @Transactional
    public String confirmToken(String token) {
        String loginLink = "http://localhost:8080/login";

        Token confirmationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalStateException("Token not found"));

        if (confirmationToken.getConfirmedAt() != null) {
            throw new IllegalStateException(buildConfirmationPage(loginLink, "Email already confirmed"));
        }

        Instant expiresAt = confirmationToken.getExpiresAt();

        if (expiresAt.isBefore(Instant.now())) {
            throw new IllegalStateException("Token expired");
        }

        confirmationToken.setConfirmedAt(Instant.now());
        tokenRepository.save(confirmationToken);

        enableUser(confirmationToken.getUser().getEmail());
        return buildConfirmationPage(loginLink, "Email confirmed successfully");
    }

    private String buildEmail(String name, String link) {
        return "<P> Hi " + name
                + ",</p>"
                + "<p>Thank you for registering with Shopping Buddy. Please click the link below to confirm your email address:</p>"
                + "<a href=\"" + link + "\">Confirm Email</a>"
                + "<p>Link will expire in one day.</p>"
                + "<p>Best regards,</p>"
                + "<p>The Shopping Buddy Team</p>";
    }

    private String buildConfirmationPage(String link, String message) {
        return "<p> " + message + "</p>";

        // Todo: Implement a proper HTML confirmation page
    }
}
