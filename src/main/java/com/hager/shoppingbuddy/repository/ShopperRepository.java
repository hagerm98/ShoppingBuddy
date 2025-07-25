package com.hager.shoppingbuddy.repository;

import com.hager.shoppingbuddy.entity.Shopper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShopperRepository extends JpaRepository<Shopper, Long> {

    Optional<Shopper> findByUserEmail(String email);

}
