package com.sparta.payment_system.repository;

import com.sparta.payment_system.entity.StockAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {
    
    // 특정 상품의 알림 조회
    List<StockAlert> findByProductIdOrderByCreatedAtDesc(Long productId);
    
    // 대기중인 알림 조회
    List<StockAlert> findByStatusOrderByCreatedAtAsc(StockAlert.AlertStatus status);
    
    // 특정 상품의 대기중인 알림 조회
    List<StockAlert> findByProductIdAndStatus(Long productId, StockAlert.AlertStatus status);
    
    // 특정 기간 내 알림 조회
    @Query("SELECT sa FROM StockAlert sa WHERE sa.createdAt BETWEEN :startDate AND :endDate ORDER BY sa.createdAt DESC")
    List<StockAlert> findAlertsByDateRange(@Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);
    
    // 재고 부족 알림만 조회
    @Query("SELECT sa FROM StockAlert sa WHERE sa.alertType = 'LOW_STOCK' AND sa.status = 'PENDING' ORDER BY sa.createdAt ASC")
    List<StockAlert> findPendingLowStockAlerts();
    
    // 품절 알림만 조회
    @Query("SELECT sa FROM StockAlert sa WHERE sa.alertType = 'OUT_OF_STOCK' AND sa.status = 'PENDING' ORDER BY sa.createdAt ASC")
    List<StockAlert> findPendingOutOfStockAlerts();
    
    // 특정 상품의 최근 알림 조회 (중복 알림 방지용)
    @Query("SELECT sa FROM StockAlert sa WHERE sa.productId = :productId AND sa.alertType = :alertType AND sa.status = 'PENDING' ORDER BY sa.createdAt DESC")
    List<StockAlert> findRecentPendingAlertsByProductAndType(@Param("productId") Long productId, 
                                                            @Param("alertType") StockAlert.AlertType alertType);
}
