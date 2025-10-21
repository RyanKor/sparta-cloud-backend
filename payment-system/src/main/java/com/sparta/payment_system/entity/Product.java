package com.sparta.payment_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;
    
    @Column(name = "name", nullable = false, length = 255)
    private String name;
    
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(name = "stock", nullable = false)
    private Integer stock = 0;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProductStatus status = ProductStatus.ACTIVE;
    
    @Column(name = "min_stock_alert", nullable = false)
    private Integer minStockAlert = 5;
    
    @Column(name = "category", length = 100)
    private String category;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public enum ProductStatus {
        ACTIVE("판매중"),
        OUT_OF_STOCK("품절"),
        DISCONTINUED("단종"),
        INACTIVE("비활성화");
        
        private final String description;
        
        ProductStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 재고 부족 여부 확인 메서드
    public boolean isLowStock() {
        return this.stock <= this.minStockAlert;
    }
    
    // 재고 차감 메서드
    public boolean decreaseStock(int quantity) {
        System.out.println("Product.decreaseStock 호출 - 현재 재고: " + this.stock + ", 차감 수량: " + quantity);
        if (this.stock >= quantity) {
            this.stock -= quantity;
            System.out.println("재고 차감 성공 - 새로운 재고: " + this.stock);
            return true;
        }
        System.out.println("재고 차감 실패 - 재고 부족");
        return false;
    }
    
    // 재고 증가 메서드
    public void increaseStock(int quantity) {
        this.stock += quantity;
    }
}
