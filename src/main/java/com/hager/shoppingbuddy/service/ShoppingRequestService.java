package com.hager.shoppingbuddy.service;

import com.hager.shoppingbuddy.dto.*;
import com.hager.shoppingbuddy.entity.*;
import com.hager.shoppingbuddy.exception.*;
import com.hager.shoppingbuddy.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingRequestService {

    private final ShoppingRequestRepository shoppingRequestRepository;
    private final CustomerRepository customerRepository;
    private final ShopperRepository shopperRepository;
    private final GeocodingService geocodingService;
    private final ShoppingRequestNotificationService notificationService;
    private final PaymentService paymentService;

    @Transactional
    public ShoppingRequestResponse createShoppingRequest(String customerEmail, ShoppingRequestCreateRequest request)
            throws CustomerNotFoundException, PaymentException {
        log.info("Creating shopping request for customer: {}", customerEmail);

        Customer customer = customerRepository.findByUserEmail(customerEmail)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with email: " + customerEmail));

        com.google.maps.model.LatLng location = geocodingService.getLatLngFromAddress(request.getDeliveryAddress());

        ShoppingRequest shoppingRequest = ShoppingRequest.builder()
                .customer(customer)
                .status(ShoppingRequestStatus.PENDING)
                .deliveryAddress(request.getDeliveryAddress())
                .latitude(location != null ? location.lat : null)
                .longitude(location != null ? location.lng : null)
                .estimatedItemsPrice(request.getEstimatedItemsPrice())
                .deliveryFee(request.getDeliveryFee())
                .paymentStatus(PaymentStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ShoppingRequest savedRequest = populateShoppingRequestItems(shoppingRequest, request.getItems());

        double totalAmount = savedRequest.getEstimatedItemsPrice() + savedRequest.getDeliveryFee();
        paymentService.createPaymentIntent(
                savedRequest.getId(),
                customer.getUser().getId(),
                totalAmount
        );

        notificationService.notifyShoppingRequestCreated(savedRequest);

        log.info("Successfully created shopping request with ID: {}", savedRequest.getId());
        return convertToResponse(savedRequest);
    }

    public List<ShoppingRequestResponse> getAllPendingRequests() {
        log.info("Retrieving all pending shopping requests");
        List<ShoppingRequest> pendingRequests = shoppingRequestRepository.findByStatusOrderByCreatedAtDesc(ShoppingRequestStatus.PENDING);
        return pendingRequests.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public ShoppingRequestResponse getShoppingRequestById(Long requestId) throws ShoppingRequestNotFoundException {
        log.info("Retrieving shopping request with ID: {}", requestId);
        ShoppingRequest request = shoppingRequestRepository.findById(requestId)
                .orElseThrow(() -> new ShoppingRequestNotFoundException("Shopping request not found with ID: " + requestId));
        return convertToResponse(request);
    }

    public List<ShoppingRequestResponse> getCustomerShoppingRequests(String customerEmail) throws CustomerNotFoundException {
        log.info("Retrieving shopping requests for customer: {}", customerEmail);
        Customer customer = customerRepository.findByUserEmail(customerEmail)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with email: " + customerEmail));

        List<ShoppingRequest> requests = shoppingRequestRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId());
        return requests.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<ShoppingRequestResponse> getShopperShoppingRequests(String shopperEmail) throws ShopperNotFoundException {
        log.info("Retrieving shopping requests for shopper: {}", shopperEmail);
        Shopper shopper = shopperRepository.findByUserEmail(shopperEmail)
                .orElseThrow(() -> new ShopperNotFoundException("Shopper not found with email: " + shopperEmail));

        List<ShoppingRequest> requests = shoppingRequestRepository.findByShopperIdOrderByCreatedAtDesc(shopper.getId());
        return requests.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ShoppingRequestResponse acceptShoppingRequest(Long requestId, String shopperEmail)
            throws ShopperNotFoundException, InvalidShoppingRequestActionException, ShoppingRequestNotFoundException {
        log.info("Shopper {} accepting shopping request: {}", shopperEmail, requestId);

        Shopper shopper = shopperRepository.findByUserEmail(shopperEmail)
                .orElseThrow(() -> new ShopperNotFoundException("Shopper not found with email: " + shopperEmail));

        ShoppingRequest request = shoppingRequestRepository.findById(requestId)
                .orElseThrow(() -> new ShoppingRequestNotFoundException("Shopping request not found with ID: " + requestId));

        if (request.getStatus() != ShoppingRequestStatus.PENDING) {
            throw new InvalidShoppingRequestActionException("Shopping request is not in PENDING status");
        }

        request.setShopper(shopper);
        request.setStatus(ShoppingRequestStatus.ACCEPTED);
        request.setUpdatedAt(Instant.now());

        ShoppingRequest savedRequest = shoppingRequestRepository.save(request);

        notificationService.notifyShoppingRequestAccepted(savedRequest);

        log.info("Shopping request {} accepted by shopper {}", requestId, shopperEmail);
        return convertToResponse(savedRequest);
    }

    @Transactional
    public ShoppingRequestResponse startShopping(Long requestId, String shopperEmail)
            throws InvalidShoppingRequestActionException, ShopperNotFoundException, ShoppingRequestNotFoundException {
        log.info("Shopper {} starting shopping for request: {}", shopperEmail, requestId);

        ShoppingRequest request = shoppingRequestRepository.findByIdAndShopperId(requestId, getShopperIdByEmail(shopperEmail))
                .orElseThrow(() -> new ShoppingRequestNotFoundException("Shopping request not found or not assigned to this shopper"));

        if (request.getStatus() != ShoppingRequestStatus.ACCEPTED) {
            throw new InvalidShoppingRequestActionException("Shopping request must be ACCEPTED to start shopping");
        }

        request.setStatus(ShoppingRequestStatus.IN_PROGRESS);
        request.setUpdatedAt(Instant.now());

        ShoppingRequest savedRequest = shoppingRequestRepository.save(request);

        notificationService.notifyShoppingStarted(savedRequest);

        log.info("Shopping started for request: {}", requestId);
        return convertToResponse(savedRequest);
    }

    @Transactional
    public ShoppingRequestResponse completeShopping(Long requestId, String shopperEmail)
            throws InvalidShoppingRequestActionException, ShopperNotFoundException, ShoppingRequestNotFoundException {
        log.info("Completing shopping for request: {} by shopper: {}", shopperEmail, requestId);
        
        ShoppingRequest request = shoppingRequestRepository.findByIdAndShopperId(requestId, getShopperIdByEmail(shopperEmail))
                .orElseThrow(() -> new ShoppingRequestNotFoundException("Shopping request not found or not assigned to this shopper"));

        if (request.getStatus() != ShoppingRequestStatus.IN_PROGRESS) {
            throw new InvalidShoppingRequestActionException("Can only complete IN_PROGRESS requests");
        }

        request.setStatus(ShoppingRequestStatus.COMPLETED);
        request.setUpdatedAt(Instant.now());

        ShoppingRequest savedRequest = shoppingRequestRepository.save(request);

        try {
            Payment payment = paymentService.capturePayment(requestId);
            savedRequest.setPaymentStatus(payment.getStatus());
            shoppingRequestRepository.save(savedRequest);
            log.info("Payment captured successfully for completed shopping request: {}", requestId);
        } catch (Exception e) {
            log.error("Failed to capture payment for shopping request: {}", requestId, e);
            throw new InvalidShoppingRequestActionException("Failed to capture payment for completed shopping request");
        }

        notificationService.notifyShoppingCompleted(savedRequest);

        log.info("Shopping completed for request: {}", requestId);
        return convertToResponse(savedRequest);
    }

    @Transactional
    public ShoppingRequestResponse cancelShoppingRequest(Long requestId, String userEmail)
            throws InvalidShoppingRequestActionException, ShoppingRequestNotFoundException {
        log.info("Cancelling shopping request: {} by user: {}", requestId, userEmail);

        ShoppingRequest request = shoppingRequestRepository.findById(requestId)
                .orElseThrow(() -> new ShoppingRequestNotFoundException("Shopping request not found with ID: " + requestId));

        // Check if user is either the customer or the assigned shopper
        boolean isCustomer = request.getCustomer().getUser().getEmail().equals(userEmail);
        boolean isShopper = request.getShopper() != null && request.getShopper().getUser().getEmail().equals(userEmail);

        if (!isCustomer && !isShopper) {
            throw new InvalidShoppingRequestActionException("User not authorized to cancel this request");
        }

        if (request.getStatus() == ShoppingRequestStatus.COMPLETED || request.getStatus() == ShoppingRequestStatus.CANCELLED) {
            throw new InvalidShoppingRequestActionException("Cannot cancel a " + request.getStatus().name() + " request");
        }

        request.setStatus(ShoppingRequestStatus.CANCELLED);
        request.setUpdatedAt(Instant.now());

        try {
            Payment payment = paymentService.cancelPayment(requestId);
            request.setPaymentStatus(payment.getStatus());
            log.info("Payment successfully cancelled for shopping request: {}", requestId);
        } catch (Exception e) {
            log.error("Failed to cancel payment for shopping request: {}", requestId, e);
            request.setPaymentStatus(PaymentStatus.FAILED);
        }

        ShoppingRequest savedRequest = shoppingRequestRepository.save(request);

        notificationService.notifyShoppingRequestCancelled(savedRequest, userEmail);

        log.info("Shopping request {} cancelled by user: {}", requestId, userEmail);
        return convertToResponse(savedRequest);
    }

    @Transactional
    public ShoppingRequestResponse updateShoppingRequest(Long requestId, String customerEmail, ShoppingRequestUpdateRequest request)
            throws CustomerNotFoundException, ShoppingRequestNotFoundException, InvalidShoppingRequestActionException {
        log.info("Updating shopping request {} for customer: {}", requestId, customerEmail);

        Customer customer = customerRepository.findByUserEmail(customerEmail)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with email: " + customerEmail));

        ShoppingRequest shoppingRequest = shoppingRequestRepository.findById(requestId)
                .orElseThrow(() -> new ShoppingRequestNotFoundException("Shopping request not found with ID: " + requestId));

        if (!shoppingRequest.getCustomer().getId().equals(customer.getId())) {
            throw new InvalidShoppingRequestActionException("Customer not authorized to edit this request");
        }

        if (shoppingRequest.getStatus() != ShoppingRequestStatus.PENDING) {
            throw new InvalidShoppingRequestActionException("Can only edit shopping requests in PENDING status");
        }

        shoppingRequest.setDeliveryAddress(request.getDeliveryAddress());
        shoppingRequest.setEstimatedItemsPrice(request.getEstimatedItemsPrice());
        shoppingRequest.setDeliveryFee(request.getDeliveryFee());
        shoppingRequest.setUpdatedAt(Instant.now());

        com.google.maps.model.LatLng location = geocodingService.getLatLngFromAddress(request.getDeliveryAddress());
        shoppingRequest.setLatitude(location != null ? location.lat : null);
        shoppingRequest.setLongitude(location != null ? location.lng : null);

        ShoppingRequest savedRequest = populateShoppingRequestItems(shoppingRequest, request.getItems());

        notificationService.notifyShoppingRequestUpdated(savedRequest);

        log.info("Successfully updated shopping request with ID: {}", savedRequest.getId());
        return convertToResponse(savedRequest);
    }

    @Transactional
    public void updatePaymentStatus(Long shoppingRequestId, PaymentStatus paymentStatus) throws ShoppingRequestNotFoundException {
        log.info("Updating payment status for shopping request: {} to {}", shoppingRequestId, paymentStatus);

        ShoppingRequest request = shoppingRequestRepository.findById(shoppingRequestId)
                .orElseThrow(() -> new ShoppingRequestNotFoundException("Shopping request not found with ID: " + shoppingRequestId));

        request.setPaymentStatus(paymentStatus);
        request.setUpdatedAt(Instant.now());

        shoppingRequestRepository.save(request);
        log.info("Payment status updated for shopping request: {}", shoppingRequestId);
    }

    private ShoppingRequest populateShoppingRequestItems(ShoppingRequest shoppingRequest, List<ItemRequest> itemRequests) {
        if (shoppingRequest.getItems() != null) {
            shoppingRequest.getItems().clear();
        } else {
            shoppingRequest.setItems(new ArrayList<>());
        }

        List<Item> newItems = itemRequests.stream()
                .map(itemRequest -> {
                    Item item = Item.builder()
                        .name(itemRequest.getName())
                        .description(itemRequest.getDescription())
                        .amount(itemRequest.getAmount())
                        .category(itemRequest.getCategory())
                        .build();
                    item.setShoppingRequest(shoppingRequest);
                    return item;
                })
                .toList();

        shoppingRequest.getItems().addAll(newItems);

        return shoppingRequestRepository.save(shoppingRequest);
    }

    private Long getShopperIdByEmail(String email) throws ShopperNotFoundException {
        return shopperRepository.findByUserEmail(email)
                .orElseThrow(() -> new ShopperNotFoundException("Shopper not found with email: " + email))
                .getId();
    }

    private ShoppingRequestResponse convertToResponse(ShoppingRequest request) {
        List<ItemResponse> itemResponses = request.getItems() != null ?
                request.getItems().stream()
                        .map(item -> ItemResponse.builder()
                                .id(item.getId())
                                .name(item.getName())
                                .description(item.getDescription())
                                .amount(item.getAmount())
                                .category(item.getCategory())
                                .build())
                        .collect(Collectors.toList()) : List.of();

        Payment payment = paymentService.getPaymentByShoppingRequestId(request.getId());

        return ShoppingRequestResponse.builder()
                .id(request.getId())
                .customerId(request.getCustomer().getUser().getId())
                .customerName(request.getCustomer().getUser().getFirstName() + " " + request.getCustomer().getUser().getLastName())
                .shopperId(request.getShopper() != null ? request.getShopper().getUser().getId() : null)
                .shopperName(request.getShopper() != null ? 
                        request.getShopper().getUser().getFirstName() + " " + request.getShopper().getUser().getLastName() : null)
                .status(request.getStatus())
                .items(itemResponses)
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .estimatedItemsPrice(request.getEstimatedItemsPrice())
                .deliveryFee(request.getDeliveryFee())
                .deliveryAddress(request.getDeliveryAddress())
                .paymentStatus(request.getPaymentStatus())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .stripeClientSecret(payment != null ? payment.getStripeClientSecret() : null)
                .stripePaymentIntentId(payment != null ? payment.getStripePaymentIntentId() : null)
                .build();
    }
}
