package com.sparta.payment_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;  // 이 줄 추가

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_alerts")
@Getter
@Setter
@NoArgsConstructor
public class StockAlert {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    private Long alertId;
    
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    @Column(name = "current_stock", nullable = false)
    private Integer currentStock;
    
    @Column(name = "min_stock_threshold", nullable = false)
    private Integer minStockThreshold;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertType alertType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AlertStatus status = AlertStatus.PENDING;
    
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;
    
    public enum AlertType {
        LOW_STOCK("재고 부족"),
        OUT_OF_STOCK("품절"),
        STOCK_RESTORED("재고 복구");
        
        private final String description;
        
        AlertType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public enum AlertStatus {
        PENDING("대기중"),
        SENT("발송완료"),
        RESOLVED("해결됨"),
        IGNORED("무시됨");
        
        private final String description;
        
        AlertStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 생성자
    public StockAlert(Long productId, Integer currentStock, Integer minStockThreshold, 
                     AlertType alertType, String message) {
        this.productId = productId;
        this.currentStock = currentStock;
        this.minStockThreshold = minStockThreshold;
        this.alertType = alertType;
        this.message = message;
    }
    
    // 알림 해결 처리
    public void resolve() {
        this.status = AlertStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
    }
    
    // 알림 발송 완료 처리
    public void markAsSent() {
        this.status = AlertStatus.SENT;
    }
}
