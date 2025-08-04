package com.hager.shoppingbuddy.service;

import com.hager.shoppingbuddy.entity.Shopper;
import com.hager.shoppingbuddy.entity.User;
import com.hager.shoppingbuddy.exception.ShopperNotFoundException;
import com.hager.shoppingbuddy.repository.ShopperRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShopperService Tests")
class ShopperServiceTest {

    @Mock
    private ShopperRepository shopperRepository;

    @InjectMocks
    private ShopperService shopperService;

    private Shopper shopper;
    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .firstName("Hager")
                .lastName("Khamis")
                .email("shopper@example.com")
                .build();

        shopper = Shopper.builder()
                .id(1L)
                .user(user)
                .balance(BigDecimal.valueOf(50.00))
                .build();
    }

    @Nested
    @DisplayName("Get Shopper Balance Tests")
    class GetShopperBalanceTests {

        @Test
        @DisplayName("Should return shopper balance when shopper exists")
        void getShopperBalance_WhenShopperExists_ShouldReturnBalance() throws ShopperNotFoundException {
            // Given
            when(shopperRepository.findByUserEmail("shopper@example.com")).thenReturn(Optional.of(shopper));

            // When
            BigDecimal balance = shopperService.getShopperBalance("shopper@example.com");

            // Then
            assertThat(balance).isEqualTo(BigDecimal.valueOf(50.00));
            verify(shopperRepository).findByUserEmail("shopper@example.com");
        }

        @Test
        @DisplayName("Should throw ShopperNotFoundException when shopper does not exist")
        void getShopperBalance_WhenShopperDoesNotExist_ShouldThrowException() {
            // Given
            when(shopperRepository.findByUserEmail("nonexistent@example.com")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> shopperService.getShopperBalance("nonexistent@example.com"))
                    .isInstanceOf(ShopperNotFoundException.class)
                    .hasMessage("Shopper not found with email: nonexistent@example.com");

            verify(shopperRepository).findByUserEmail("nonexistent@example.com");
        }

        @Test
        @DisplayName("Should return zero balance for new shopper")
        void getShopperBalance_WhenNewShopper_ShouldReturnZero() throws ShopperNotFoundException {
            // Given
            Shopper newShopper = Shopper.builder()
                    .id(2L)
                    .user(user)
                    .balance(BigDecimal.ZERO)
                    .build();

            when(shopperRepository.findByUserEmail("shopper@example.com")).thenReturn(Optional.of(newShopper));

            // When
            BigDecimal balance = shopperService.getShopperBalance("shopper@example.com");

            // Then
            assertThat(balance).isEqualTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Add To Balance By ID Tests")
    class AddToBalanceByIdTests {

        @Test
        @DisplayName("Should add amount to balance when shopper ID exists")
        void addToBalanceById_WhenShopperExists_ShouldAddToBalance() throws ShopperNotFoundException {
            // Given
            BigDecimal amountToAdd = BigDecimal.valueOf(30.00);
            BigDecimal expectedNewBalance = BigDecimal.valueOf(80.00);

            when(shopperRepository.findById(1L)).thenReturn(Optional.of(shopper));
            when(shopperRepository.save(any(Shopper.class))).thenReturn(shopper);

            // When
            shopperService.addToBalanceById(1L, amountToAdd);

            // Then
            verify(shopperRepository).findById(1L);
            verify(shopperRepository).save(argThat(savedShopper ->
                savedShopper.getBalance().equals(expectedNewBalance)
            ));
        }

        @Test
        @DisplayName("Should throw ShopperNotFoundException when shopper ID does not exist")
        void addToBalanceById_WhenShopperDoesNotExist_ShouldThrowException() {
            // Given
            when(shopperRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> shopperService.addToBalanceById(999L, BigDecimal.valueOf(10.00)))
                    .isInstanceOf(ShopperNotFoundException.class)
                    .hasMessage("Shopper not found with ID: 999");

            verify(shopperRepository).findById(999L);
            verify(shopperRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle decimal precision correctly")
        void addToBalanceById_WhenAddingDecimalAmount_ShouldMaintainPrecision() throws ShopperNotFoundException {
            // Given
            BigDecimal preciseAmount = new BigDecimal("15.77");
            BigDecimal expectedBalance = new BigDecimal("65.77");

            when(shopperRepository.findById(1L)).thenReturn(Optional.of(shopper));
            when(shopperRepository.save(any(Shopper.class))).thenReturn(shopper);

            // When
            shopperService.addToBalanceById(1L, preciseAmount);

            // Then
            verify(shopperRepository).save(argThat(savedShopper ->
                savedShopper.getBalance().equals(expectedBalance)
            ));
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null email gracefully")
        void getShopperBalance_WhenEmailIsNull_ShouldThrowException() {
            // Given
            when(shopperRepository.findByUserEmail(null)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> shopperService.getShopperBalance(null))
                    .isInstanceOf(ShopperNotFoundException.class)
                    .hasMessage("Shopper not found with email: null");
        }

        @Test
        @DisplayName("Should handle empty email gracefully")
        void getShopperBalance_WhenEmailIsEmpty_ShouldThrowException() {
            // Given
            when(shopperRepository.findByUserEmail("")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> shopperService.getShopperBalance(""))
                    .isInstanceOf(ShopperNotFoundException.class)
                    .hasMessage("Shopper not found with email: ");
        }
    }
}
