package com.hager.shoppingbuddy.controller;

import com.hager.shoppingbuddy.dto.*;
import com.hager.shoppingbuddy.entity.UserRole;
import com.hager.shoppingbuddy.exception.*;
import com.hager.shoppingbuddy.service.ShoppingRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/shopping-requests")
@RequiredArgsConstructor
public class ShoppingRequestController {

    private final ShoppingRequestService shoppingRequestService;

    @PostMapping
    public ResponseEntity<ShoppingRequestResponse> createShoppingRequest(
            @Valid @RequestBody ShoppingRequestCreateRequest request,
            Authentication authentication
    ) throws CustomerNotFoundException, UnauthorizedRoleException, PaymentException {

        verifyUserRole(authentication, UserRole.CUSTOMER);
        log.info("Creating shopping request for user: {}", authentication.getName());

        ShoppingRequestResponse response = shoppingRequestService.createShoppingRequest(
                authentication.getName(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<ShoppingRequestResponse>> getAllPendingRequests() {
        log.info("Retrieving all pending shopping requests");

        List<ShoppingRequestResponse> responses = shoppingRequestService.getAllPendingRequests();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<ShoppingRequestResponse> getShoppingRequestById(@PathVariable Long requestId)
            throws ShoppingRequestNotFoundException {
        log.info("Retrieving shopping request with ID: {}", requestId);

        ShoppingRequestResponse response = shoppingRequestService.getShoppingRequestById(requestId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/customer/my-requests")
    public ResponseEntity<List<ShoppingRequestResponse>> getMyShoppingRequests(
            Authentication authentication
    ) throws CustomerNotFoundException, UnauthorizedRoleException {

        verifyUserRole(authentication, UserRole.CUSTOMER);
        log.info("Retrieving shopping requests for customer: {}", authentication.getName());

        List<ShoppingRequestResponse> responses = shoppingRequestService.getCustomerShoppingRequests(
                authentication.getName());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/shopper/my-requests")
    public ResponseEntity<List<ShoppingRequestResponse>> getMyShopperRequests(
            Authentication authentication
    ) throws ShopperNotFoundException, UnauthorizedRoleException {

        verifyUserRole(authentication, UserRole.SHOPPER);
        log.info("Retrieving shopping requests for shopper: {}", authentication.getName());

        List<ShoppingRequestResponse> responses = shoppingRequestService.getShopperShoppingRequests(
                authentication.getName());

        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{requestId}/accept")
    public ResponseEntity<ShoppingRequestResponse> acceptShoppingRequest(
            @PathVariable Long requestId,
            Authentication authentication
    ) throws ShopperNotFoundException, InvalidShoppingRequestActionException,
            UnauthorizedRoleException, ShoppingRequestNotFoundException {

        verifyUserRole(authentication, UserRole.SHOPPER);
        log.info("Accepting shopping request {} by shopper: {}", requestId, authentication.getName());

        ShoppingRequestResponse response = shoppingRequestService.acceptShoppingRequest(
                requestId, authentication.getName());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{requestId}/start-shopping")
    public ResponseEntity<ShoppingRequestResponse> startShopping(
            @PathVariable Long requestId,
            Authentication authentication
    ) throws ShopperNotFoundException, InvalidShoppingRequestActionException,
            UnauthorizedRoleException, ShoppingRequestNotFoundException {

        verifyUserRole(authentication, UserRole.SHOPPER);
        log.info("Starting shopping for request {} by shopper: {}", requestId, authentication.getName());

        ShoppingRequestResponse response = shoppingRequestService.startShopping(
                requestId, authentication.getName());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{requestId}/complete")
    public ResponseEntity<ShoppingRequestResponse> completeShopping(
            @PathVariable Long requestId,
            Authentication authentication
    ) throws ShopperNotFoundException, InvalidShoppingRequestActionException,
            UnauthorizedRoleException, ShoppingRequestNotFoundException {

        verifyUserRole(authentication, UserRole.SHOPPER);
        log.info("Completing shopping for request {} by shopper: {}", requestId, authentication.getName());

        ShoppingRequestResponse response = shoppingRequestService.completeShopping(
                requestId, authentication.getName());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{requestId}/abandon")
    public ResponseEntity<ShoppingRequestResponse> abandonShoppingRequest(
            @PathVariable Long requestId,
            Authentication authentication
    ) throws ShopperNotFoundException, InvalidShoppingRequestActionException,
            UnauthorizedRoleException, ShoppingRequestNotFoundException {

        verifyUserRole(authentication, UserRole.SHOPPER);
        log.info("Abandoning shopping request {} by shopper: {}", requestId, authentication.getName());

        ShoppingRequestResponse response = shoppingRequestService.abandonShoppingRequest(
                requestId, authentication.getName());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{requestId}/cancel")
    public ResponseEntity<ShoppingRequestResponse> cancelShoppingRequest(
            @PathVariable Long requestId,
            Authentication authentication
    ) throws InvalidShoppingRequestActionException, ShoppingRequestNotFoundException {

        log.info("Cancelling shopping request {} by user: {}", requestId, authentication.getName());

        ShoppingRequestResponse response = shoppingRequestService.cancelShoppingRequest(
                requestId, authentication.getName());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{requestId}")
    public ResponseEntity<ShoppingRequestResponse> updateShoppingRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody ShoppingRequestUpdateRequest request,
            Authentication authentication
    ) throws CustomerNotFoundException, UnauthorizedRoleException, ShoppingRequestNotFoundException, InvalidShoppingRequestActionException {

        verifyUserRole(authentication, UserRole.CUSTOMER);
        log.info("Updating shopping request {} for customer: {}", requestId, authentication.getName());

        ShoppingRequestResponse response = shoppingRequestService.updateShoppingRequest(
                requestId, authentication.getName(), request);

        return ResponseEntity.ok(response);
    }

    private void verifyUserRole(Authentication authentication, UserRole requiredRole) throws UnauthorizedRoleException {
        SimpleGrantedAuthority requiredAuthority = new SimpleGrantedAuthority(requiredRole.name());
        if (!authentication.getAuthorities().contains(requiredAuthority)) {
            throw new UnauthorizedRoleException("Access denied. Required role: " + requiredRole.name());
        }
    }
}
