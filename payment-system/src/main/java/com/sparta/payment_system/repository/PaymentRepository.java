package com.sparta.payment_system.repository;

import com.sparta.payment_system.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    Optional<Payment> findByOrderId(String orderId);
    
    Optional<Payment> findByImpUid(String impUid);
    
    List<Payment> findByStatus(Payment.PaymentStatus status);
    
    List<Payment> findByMethodId(Long methodId);
}
