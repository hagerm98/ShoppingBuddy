package com.hager.shoppingbuddy.controller;

import com.hager.shoppingbuddy.exception.ShopperNotFoundException;
import com.hager.shoppingbuddy.exception.UnauthorizedRoleException;
import com.hager.shoppingbuddy.service.ShopperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/api/shopper")
@RequiredArgsConstructor
public class ShopperController {

    private final ShopperService shopperService;

    @GetMapping("/balance")
    public ResponseEntity<BigDecimal> getBalance(Authentication authentication) throws ShopperNotFoundException, UnauthorizedRoleException {
        if (!authentication.getAuthorities().contains(new SimpleGrantedAuthority("SHOPPER"))) {
            throw new UnauthorizedRoleException("Access denied. Required role: SHOPPER");
        }

        BigDecimal balance = shopperService.getShopperBalance(authentication.getName());
        return ResponseEntity.ok(balance);
    }
}
