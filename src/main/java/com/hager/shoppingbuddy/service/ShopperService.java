package com.hager.shoppingbuddy.service;

import com.hager.shoppingbuddy.entity.Shopper;
import com.hager.shoppingbuddy.exception.ShopperNotFoundException;
import com.hager.shoppingbuddy.repository.ShopperRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopperService {

    private final ShopperRepository shopperRepository;

    public BigDecimal getShopperBalance(String shopperEmail) throws ShopperNotFoundException {
        log.info("Retrieving balance for shopper: {}", shopperEmail);

        Shopper shopper = shopperRepository.findByUserEmail(shopperEmail)
                .orElseThrow(() -> new ShopperNotFoundException("Shopper not found with email: " + shopperEmail));

        return shopper.getBalance();
    }

    @Transactional
    public void addToBalanceById(Long shopperId, BigDecimal amount) throws ShopperNotFoundException {
        log.info("Adding {} to balance for shopper ID: {}", amount, shopperId);

        Shopper shopper = shopperRepository.findById(shopperId)
                .orElseThrow(() -> new ShopperNotFoundException("Shopper not found with ID: " + shopperId));

        BigDecimal newBalance = shopper.getBalance().add(amount);
        shopper.setBalance(newBalance);

        shopperRepository.save(shopper);
        log.info("Balance updated for shopper ID: {}. New balance: {}", shopperId, newBalance);
    }
}
