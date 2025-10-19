package com.sparta.payment_system.repository;

import com.sparta.payment_system.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    List<OrderItem> findByOrderId(String orderId);
    
    List<OrderItem> findByProductId(Long productId);
}
