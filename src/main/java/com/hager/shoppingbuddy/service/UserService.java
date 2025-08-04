package com.hager.shoppingbuddy.service;

import com.hager.shoppingbuddy.dto.RegistrationRequest;
import com.hager.shoppingbuddy.entity.*;
import com.hager.shoppingbuddy.exception.*;
import com.hager.shoppingbuddy.repository.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final CustomerRepository customerRepository;
    private final ShopperRepository shopperRepository;
    private final EmailService emailService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Value("${shoppingbuddy.base-url}")
    private String baseUrl;

    private final static String USER_NOT_FOUND_MESSAGE = "We couldn't find an account associated with the email address %s. Please check your email or create a new account.";
    private final static long TOKEN_EXPIRY_HOURS = 24;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("Loading user by email: {}", email);
        try {
            return findByEmail(email);
        } catch (UserNotFoundException e) {
            throw new UsernameNotFoundException(e.getMessage(), e);
        }
    }

    public void enableUser(String email) throws UserNotFoundException {
        log.info("Enabling user with email: {}", email);
        User user = findByEmail(email);
        user.setEnabled(true);
        userRepository.save(user);
        log.info("User enabled: {}", email);
    }

    @Transactional
    public boolean register(RegistrationRequest request) throws EmailAlreadyExistsException {
        log.info("Registering user: {} with role: {}", request.getEmail(), request.getUserRole());

        User user = User.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber().trim())
                .passwordHash(request.getPassword())
                .role(request.getUserRole())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .isEnabled(false)
                .isLocked(false)
                .lastPasswordChange(Instant.now())
                .build();

        String token = createUserAndToken(user);
        String confirmationLink = baseUrl + "/api/user/confirm?token=" + token;

        log.info("Sending confirmation email to: {}", request.getEmail());
        sendConfirmationEmail(request.getEmail(), request.getFirstName(), confirmationLink);

        return true;
    }

    @Transactional(noRollbackFor = {InvalidTokenException.class, TokenExpiredException.class})
    public void confirmToken(String token) throws InvalidTokenException, TokenExpiredException, UserNotFoundException {
        log.info("Confirming token: {}", token);

        if (!StringUtils.hasText(token)) {
            throw new InvalidTokenException("The confirmation link appears to be incomplete. Please check your email and click the complete confirmation link.");
        }

        Token confirmationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("This confirmation link is not valid or may have already been used. Please request a new confirmation email."));

        log.info("Found confirmation token for user: {}", confirmationToken.getUser().getEmail());

        if (confirmationToken.getConfirmedAt() != null) {
            return;
        }

        Instant expiresAt = confirmationToken.getExpiresAt();
        if (expiresAt.isBefore(Instant.now())) {
            log.warn("Expired token used: {}", token);
            throw new TokenExpiredException("Your confirmation link has expired. For security reasons, confirmation links are only valid for 24 hours. Please register again to receive a new confirmation email.");
        }

        confirmationToken.setConfirmedAt(Instant.now());
        log.info("Setting confirmation time for token: {}", token);
        tokenRepository.save(confirmationToken);

        enableUser(confirmationToken.getUser().getEmail());

    }

    private String createUserAndToken(User user) throws EmailAlreadyExistsException {
        log.info("Signing up user: {}", user.getEmail());

        boolean userExists = userRepository.findByEmail(user.getEmail()).isPresent();
        if (userExists) {
            log.warn("Attempt to register with existing email: {}", user.getEmail());
            throw new EmailAlreadyExistsException("An account with this email address already exists. Please try logging in instead, or use a different email address to create a new account.");
        }

        String encodedPassword = bCryptPasswordEncoder.encode(user.getPassword());
        user.setPasswordHash(encodedPassword);

        log.info("Saving user: {}", user.getEmail());
        User savedUser = userRepository.save(user);

        createRoleSpecificEntity(savedUser);

        String token = UUID.randomUUID().toString();
        Token confirmationToken = Token.builder()
                .token(token)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60 * 60 * TOKEN_EXPIRY_HOURS))
                .user(savedUser)
                .build();

        log.info("Saving confirmation token for user: {}", user.getEmail());
        tokenRepository.save(confirmationToken);
        return token;
    }

    private void createRoleSpecificEntity(User user) {
        log.info("Creating role-specific entity for user: {} with role: {}", user.getEmail(), user.getRole());

        if (user.getRole() == null) {
            log.error("User role is null for user: {}", user.getEmail());
            throw new IllegalArgumentException("Unknown user role: null");
        }

        switch (user.getRole()) {
            case CUSTOMER:
                Customer customer = Customer.builder()
                        .user(user)
                        .address(null)
                        .build();
                customerRepository.save(customer);
                log.info("Created Customer entity for user: {}", user.getEmail());
                break;

            case SHOPPER:
                Shopper shopper = Shopper.builder()
                        .user(user)
                        .build();
                shopperRepository.save(shopper);
                log.info("Created Shopper entity for user: {}", user.getEmail());
                break;

            default:
                log.error("Unknown user role: {} for user: {}", user.getRole(), user.getEmail());
                throw new IllegalArgumentException("Unknown user role: " + user.getRole());
        }
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

    public User findByEmail(String email) throws UserNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(String.format(USER_NOT_FOUND_MESSAGE, email)));
    }

    @Transactional
    public void updateProfile(String currentUserEmail, String firstName, String lastName, String phoneNumber)
            throws UserNotFoundException {
        log.info("Updating profile for user: {}", currentUserEmail);

        User user = findByEmail(currentUserEmail);

        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setPhoneNumber(phoneNumber.trim());
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);
        log.info("Profile updated successfully for user: {}", currentUserEmail);

    }

    @Transactional
    public void changePassword(String userEmail, String currentPassword, String newPassword)
            throws PasswordChangeException, UserNotFoundException {
        log.info("Changing password for user: {}", userEmail);

        User user = findByEmail(userEmail);

        if (!bCryptPasswordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new PasswordChangeException("Current password is incorrect");
        }

        String encodedNewPassword = bCryptPasswordEncoder.encode(newPassword);
        user.setPasswordHash(encodedNewPassword);
        user.setLastPasswordChange(Instant.now());
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);
        log.info("Password changed successfully for user: {}", userEmail);

    }
}
