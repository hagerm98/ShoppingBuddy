package com.hager.shoppingbuddy.repository;

import com.hager.shoppingbuddy.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByShoppingRequestIdOrderByTimestampAsc(Long shoppingRequestId);

    long countByShoppingRequestId(Long shoppingRequestId);
}
