package com.hager.shoppingbuddy.service;

import com.hager.shoppingbuddy.dto.RegistrationRequest;
import com.hager.shoppingbuddy.entity.Token;
import com.hager.shoppingbuddy.entity.User;
import com.hager.shoppingbuddy.repository.TokenRepository;
import com.hager.shoppingbuddy.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    private final static String USER_NOT_FOUND_MESSAGE = "User with email %s not found";
    private final static long TOKEN_EXPIRY_HOURS = 24;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("Loading user by email: {}", email);
        return userRepository.findByEmail(email).orElseThrow(() ->
            new UsernameNotFoundException(String.format(USER_NOT_FOUND_MESSAGE, email)));
    }

    public void enableUser(String email) {
        log.info("Enabling user with email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException(String.format(USER_NOT_FOUND_MESSAGE, email)));

        user.setEnabled(true);
        userRepository.save(user);
        log.info("User enabled: {}", email);
    }

    @Transactional
    public boolean register(RegistrationRequest request) {
        log.info("Registering user: {} with role: {}", request.getEmail(), request.getUserRole());

        try {
            User user = User.builder()
                    .firstName(request.getFirstName().trim())
                    .lastName(request.getLastName().trim())
                    .email(request.getEmail())
                    .passwordHash(request.getPassword())
                    .role(request.getUserRole())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .isEnabled(false)
                    .isLocked(false)
                    .lastPasswordChange(Instant.now())
                    .build();

            String token = createUserAndToken(user);
            String confirmationLink = "http://localhost:8080/user/confirm?token=" + token;

            log.info("Sending confirmation email to: {}", request.getEmail());
            sendConfirmationEmail(request.getEmail(), request.getFirstName(), confirmationLink);

            return true;
        } catch (Exception e) {
            log.error("Registration failed for user: {}", request.getEmail(), e);
            throw e;
        }
    }

    @Transactional
    public boolean confirmToken(String token) {
        log.info("Confirming token: {}", token);

        try {
            if (!StringUtils.hasText(token)) {
                throw new IllegalStateException("Token cannot be empty");
            }

            Token confirmationToken = tokenRepository.findByToken(token)
                    .orElseThrow(() -> new IllegalStateException("Invalid token"));

            log.info("Found confirmation token for user: {}", confirmationToken.getUser().getEmail());

            if (confirmationToken.getConfirmedAt() != null) {
                return true;
            }

            Instant expiresAt = confirmationToken.getExpiresAt();
            if (expiresAt.isBefore(Instant.now())) {
                log.warn("Expired token used: {}", token);
                throw new IllegalStateException("Confirmation Token expired.");
            }

            confirmationToken.setConfirmedAt(Instant.now());
            log.info("Setting confirmation time for token: {}", token);
            tokenRepository.save(confirmationToken);

            enableUser(confirmationToken.getUser().getEmail());

            return true;
        } catch (Exception e) {
            log.error("Token confirmation failed for token: {}", token, e);
            throw e;
        }
    }

    private String createUserAndToken(User user) {
        log.info("Signing up user: {}", user.getEmail());

        boolean userExists = userRepository.findByEmail(user.getEmail()).isPresent();
        if (userExists) {
            log.warn("Attempt to register with existing email: {}", user.getEmail());
            throw new IllegalStateException("Email already taken");
        }

        String encodedPassword = bCryptPasswordEncoder.encode(user.getPassword());
        user.setPasswordHash(encodedPassword);

        log.info("Saving user: {}", user.getEmail());
        userRepository.save(user);

        String token = UUID.randomUUID().toString();
        Token confirmationToken = Token.builder()
                .token(token)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60 * 60 * TOKEN_EXPIRY_HOURS))
                .user(user)
                .build();

        log.info("Saving confirmation token for user: {}", user.getEmail());
        tokenRepository.save(confirmationToken);
        return token;
    }

    private void sendConfirmationEmail(String email, String name, String confirmationLink) {
        String emailBody = "<P> Hi " + name
                + ",</p>"
                + "<p>Thank you for registering with Shopping Buddy. Please click the link below to confirm your email address:</p>"
                + "<a href=\"" + confirmationLink + "\">Confirm Email</a>"
                + "<p>Link will expire in one day.</p>"
                + "<p>Best regards,</p>"
                + "<p>The Shopping Buddy Team</p>";

        emailService.send(email, "Confirm Your Shopping Buddy New Account", emailBody);
    }
}
