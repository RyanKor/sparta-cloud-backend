package com.sparta.payment_system.repository;

import com.sparta.payment_system.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {
    
    List<Refund> findByPaymentId(Long paymentId);
    
    List<Refund> findByStatus(Refund.RefundStatus status);
}
