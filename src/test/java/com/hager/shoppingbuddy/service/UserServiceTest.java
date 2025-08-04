package com.hager.shoppingbuddy.service;

import com.hager.shoppingbuddy.dto.RegistrationRequest;
import com.hager.shoppingbuddy.entity.*;
import com.hager.shoppingbuddy.exception.*;
import com.hager.shoppingbuddy.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ShopperRepository shopperRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @InjectMocks
    private UserService userService;

    private final String userEmail = "test@example.com";
    private final String userPassword = "password123";
    private final String encodedPassword = "$2a$10$encoded.password.hash";

    @BeforeEach
    void setUp() {
        String baseUrl = "http://localhost:8080";
        ReflectionTestUtils.setField(userService, "baseUrl", baseUrl);
    }

    @Nested
    @DisplayName("Load User by Username Tests")
    class LoadUserByUsernameTests {

        @Test
        @DisplayName("Should load user successfully when user exists")
        void loadUserByUsername_WhenUserExists_ShouldReturnUserDetails() {
            // Given
            User user = createTestUser();
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));

            // When
            UserDetails result = userService.loadUserByUsername(userEmail);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo(userEmail);
            verify(userRepository).findByEmail(userEmail);
        }

        @Test
        @DisplayName("Should throw UsernameNotFoundException when user not found")
        void loadUserByUsername_WhenUserNotFound_ShouldThrowUsernameNotFoundException() {
            // Given
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.loadUserByUsername(userEmail))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("We couldn't find an account associated with the email address");
        }
    }

    @Nested
    @DisplayName("Enable User Tests")
    class EnableUserTests {

        @Test
        @DisplayName("Should enable user successfully")
        void enableUser_WhenUserExists_ShouldEnableUser() throws UserNotFoundException {
            // Given
            User user = createTestUser();
            user.setEnabled(false);
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            // When
            userService.enableUser(userEmail);

            // Then
            verify(userRepository).save(argThat(User::isEnabled));
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user not found")
        void enableUser_WhenUserNotFound_ShouldThrowUserNotFoundException() {
            // Given
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.enableUser(userEmail))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Should register customer successfully")
        void register_WhenValidCustomerRequest_ShouldRegisterUser() throws EmailAlreadyExistsException {
            // Given
            RegistrationRequest request = createRegistrationRequest(UserRole.CUSTOMER);
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());
            when(bCryptPasswordEncoder.encode(userPassword)).thenReturn(encodedPassword);

            User savedUser = createTestUser();
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            Customer savedCustomer = Customer.builder().user(savedUser).build();
            when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);

            Token savedToken = createTestToken(savedUser);
            when(tokenRepository.save(any(Token.class))).thenReturn(savedToken);

            // When
            boolean result = userService.register(request);

            // Then
            assertThat(result).isTrue();
            verify(userRepository).save(argThat(user ->
                user.getEmail().equals(userEmail) &&
                user.getRole() == UserRole.CUSTOMER &&
                !user.isEnabled() &&
                user.getPasswordHash().equals(encodedPassword)
            ));
            verify(customerRepository).save(any(Customer.class));
            verify(tokenRepository).save(any(Token.class));
            verify(emailService).send(eq(userEmail), anyString(), anyString());
        }

        @Test
        @DisplayName("Should register shopper successfully")
        void register_WhenValidShopperRequest_ShouldRegisterUser() throws EmailAlreadyExistsException {
            // Given
            RegistrationRequest request = createRegistrationRequest(UserRole.SHOPPER);
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());
            when(bCryptPasswordEncoder.encode(userPassword)).thenReturn(encodedPassword);

            User savedUser = createTestUser();
            savedUser.setRole(UserRole.SHOPPER);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            Shopper savedShopper = Shopper.builder()
                    .user(savedUser)
                    .balance(BigDecimal.ZERO)
                    .build();
            when(shopperRepository.save(any(Shopper.class))).thenReturn(savedShopper);

            Token savedToken = createTestToken(savedUser);
            when(tokenRepository.save(any(Token.class))).thenReturn(savedToken);

            // When
            boolean result = userService.register(request);

            // Then
            assertThat(result).isTrue();
            verify(userRepository).save(argThat(user ->
                user.getRole() == UserRole.SHOPPER
            ));
            verify(shopperRepository).save(any(Shopper.class));
            verify(tokenRepository).save(any(Token.class));
            verify(emailService).send(eq(userEmail), anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw EmailAlreadyExistsException when email exists")
        void register_WhenEmailExists_ShouldThrowEmailAlreadyExistsException() {
            // Given
            RegistrationRequest request = createRegistrationRequest(UserRole.CUSTOMER);
            User existingUser = createTestUser();
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(existingUser));

            // When & Then
            assertThatThrownBy(() -> userService.register(request))
                    .isInstanceOf(EmailAlreadyExistsException.class)
                    .hasMessageContaining("An account with this email address already exists");

            verify(userRepository, never()).save(any(User.class));
            verify(customerRepository, never()).save(any(Customer.class));
            verify(shopperRepository, never()).save(any(Shopper.class));
            verify(emailService, never()).send(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for unknown user role")
        void register_WhenUnknownRole_ShouldThrowIllegalArgumentException() {
            // Given
            RegistrationRequest request = createRegistrationRequest(null);
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());
            when(bCryptPasswordEncoder.encode(userPassword)).thenReturn(encodedPassword);

            User savedUser = createTestUser();
            savedUser.setRole(null);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            // When & Then
            assertThatThrownBy(() -> userService.register(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown user role");
        }
    }

    @Nested
    @DisplayName("Confirm Token Tests")
    class ConfirmTokenTests {

        @Test
        @DisplayName("Should confirm token successfully")
        void confirmToken_WhenValidToken_ShouldConfirmToken() throws InvalidTokenException, TokenExpiredException, UserNotFoundException {
            // Given
            String tokenValue = "valid-token-123";
            User user = createTestUser();
            user.setEnabled(false);
            Token token = createTestToken(user);
            token.setToken(tokenValue);
            token.setExpiresAt(Instant.now().plusSeconds(3600));

            when(tokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(token));
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));
            when(tokenRepository.save(any(Token.class))).thenReturn(token);
            when(userRepository.save(any(User.class))).thenReturn(user);

            // When
            userService.confirmToken(tokenValue);

            // Then
            verify(tokenRepository).save(argThat(savedToken ->
                savedToken.getConfirmedAt() != null
            ));
            verify(userRepository).save(argThat(User::isEnabled
            ));
        }

        @Test
        @DisplayName("Should do nothing when token already confirmed")
        void confirmToken_WhenTokenAlreadyConfirmed_ShouldDoNothing() throws InvalidTokenException, TokenExpiredException, UserNotFoundException {
            // Given
            String tokenValue = "confirmed-token-123";
            User user = createTestUser();
            Token token = createTestToken(user);
            token.setToken(tokenValue);
            token.setConfirmedAt(Instant.now());

            when(tokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(token));

            // When
            userService.confirmToken(tokenValue);

            // Then
            verify(tokenRepository, never()).save(any(Token.class));
            verify(userRepository, never()).findByEmail(anyString());
        }

        @Test
        @DisplayName("Should throw InvalidTokenException when token is null or empty")
        void confirmToken_WhenTokenNullOrEmpty_ShouldThrowInvalidTokenException() {
            // When & Then
            assertThatThrownBy(() -> userService.confirmToken(null))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("The confirmation link appears to be incomplete");

            assertThatThrownBy(() -> userService.confirmToken(""))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("The confirmation link appears to be incomplete");
        }

        @Test
        @DisplayName("Should throw InvalidTokenException when token not found")
        void confirmToken_WhenTokenNotFound_ShouldThrowInvalidTokenException() {
            // Given
            String tokenValue = "invalid-token-123";
            when(tokenRepository.findByToken(tokenValue)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.confirmToken(tokenValue))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("This confirmation link is not valid");
        }

        @Test
        @DisplayName("Should throw TokenExpiredException when token is expired")
        void confirmToken_WhenTokenExpired_ShouldThrowTokenExpiredException() {
            // Given
            String tokenValue = "expired-token-123";
            User user = createTestUser();
            Token token = createTestToken(user);
            token.setToken(tokenValue);
            token.setExpiresAt(Instant.now().minusSeconds(3600));

            when(tokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(token));

            // When & Then
            assertThatThrownBy(() -> userService.confirmToken(tokenValue))
                    .isInstanceOf(TokenExpiredException.class)
                    .hasMessageContaining("Your confirmation link has expired");
        }
    }

    @Nested
    @DisplayName("Find User Tests")
    class FindUserTests {

        @Test
        @DisplayName("Should find user by email successfully")
        void findByEmail_WhenUserExists_ShouldReturnUser() throws UserNotFoundException {
            // Given
            User user = createTestUser();
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));

            // When
            User result = userService.findByEmail(userEmail);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo(userEmail);
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user not found")
        void findByEmail_WhenUserNotFound_ShouldThrowUserNotFoundException() {
            // Given
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.findByEmail(userEmail))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("We couldn't find an account associated with the email address");
        }
    }

    @Nested
    @DisplayName("Update Profile Tests")
    class UpdateProfileTests {

        @Test
        @DisplayName("Should update profile successfully")
        void updateProfile_WhenValidData_ShouldUpdateProfile() throws UserNotFoundException {
            // Given
            String newFirstName = "UpdatedFirst";
            String newLastName = "UpdatedLast";
            String newPhoneNumber = "+353123456789";

            User user = createTestUser();
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            // When
            userService.updateProfile(userEmail, newFirstName, newLastName, newPhoneNumber);

            // Then
            verify(userRepository).save(argThat(savedUser ->
                savedUser.getFirstName().equals(newFirstName) &&
                savedUser.getLastName().equals(newLastName) &&
                savedUser.getPhoneNumber().equals(newPhoneNumber) &&
                savedUser.getUpdatedAt() != null
            ));
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user not found")
        void updateProfile_WhenUserNotFound_ShouldThrowUserNotFoundException() {
            // Given
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.updateProfile(userEmail, "First", "Last", "Phone"))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Change Password Tests")
    class ChangePasswordTests {

        @Test
        @DisplayName("Should change password successfully")
        void changePassword_WhenValidCurrentPassword_ShouldChangePassword() throws PasswordChangeException, UserNotFoundException {
            // Given
            String currentPassword = "currentPassword";
            String newPassword = "newPassword123";
            String newEncodedPassword = "$2a$10$new.encoded.password.hash";

            User user = createTestUser();
            user.setPasswordHash(encodedPassword);

            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));
            when(bCryptPasswordEncoder.matches(currentPassword, encodedPassword)).thenReturn(true);
            when(bCryptPasswordEncoder.encode(newPassword)).thenReturn(newEncodedPassword);
            when(userRepository.save(any(User.class))).thenReturn(user);

            // When
            userService.changePassword(userEmail, currentPassword, newPassword);

            // Then
            verify(userRepository).save(argThat(savedUser ->
                savedUser.getPasswordHash().equals(newEncodedPassword) &&
                savedUser.getLastPasswordChange() != null &&
                savedUser.getUpdatedAt() != null
            ));
        }

        @Test
        @DisplayName("Should throw PasswordChangeException when current password is incorrect")
        void changePassword_WhenIncorrectCurrentPassword_ShouldThrowPasswordChangeException() {
            // Given
            String currentPassword = "wrongPassword";
            String newPassword = "newPassword123";

            User user = createTestUser();
            user.setPasswordHash(encodedPassword);

            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));
            when(bCryptPasswordEncoder.matches(currentPassword, encodedPassword)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> userService.changePassword(userEmail, currentPassword, newPassword))
                    .isInstanceOf(PasswordChangeException.class)
                    .hasMessageContaining("Current password is incorrect");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user not found")
        void changePassword_WhenUserNotFound_ShouldThrowUserNotFoundException() {
            // Given
            when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> userService.changePassword(userEmail, "current", "new"))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // Helper methods
    private User createTestUser() {
        return User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email(userEmail)
                .phoneNumber("+353123456789")
                .passwordHash(encodedPassword)
                .role(UserRole.CUSTOMER)
                .isEnabled(true)
                .isLocked(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .lastPasswordChange(Instant.now())
                .build();
    }

    private RegistrationRequest createRegistrationRequest(UserRole role) {
        RegistrationRequest request = new RegistrationRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail(userEmail);
        request.setPhoneNumber("+353123456789");
        request.setPassword(userPassword);
        request.setUserRole(role);
        return request;
    }

    private Token createTestToken(User user) {
        return Token.builder()
                .id(1L)
                .token("test-token-123")
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(86400))
                .user(user)
                .build();
    }
}
