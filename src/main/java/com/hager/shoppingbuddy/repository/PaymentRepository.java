package com.hager.shoppingbuddy.repository;

import com.hager.shoppingbuddy.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByShoppingRequestId(Long shoppingRequestId);
}
