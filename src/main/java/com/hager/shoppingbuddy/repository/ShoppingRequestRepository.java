package com.hager.shoppingbuddy.repository;

import com.hager.shoppingbuddy.entity.PaymentStatus;
import com.hager.shoppingbuddy.entity.ShoppingRequest;
import com.hager.shoppingbuddy.entity.ShoppingRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShoppingRequestRepository extends JpaRepository<ShoppingRequest, Long> {

    Optional<ShoppingRequest> findByIdAndShopperId(Long id, Long shopperId);

    List<ShoppingRequest> findByStatusAndPaymentStatusOrderByCreatedAtDesc(ShoppingRequestStatus status, PaymentStatus paymentStatus);

    List<ShoppingRequest> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<ShoppingRequest> findByShopperIdOrderByCreatedAtDesc(Long shopperId);
}
