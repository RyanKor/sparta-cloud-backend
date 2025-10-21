package com.sparta.payment_system.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;
    
    @Column(name = "order_id", nullable = false, length = 255)
    private String orderId;
    
    @Column(name = "method_id")
    private Long methodId;
    
    @Column(name = "imp_uid", unique = true, length = 255)
    private String impUid;
    
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private PaymentStatus status;
    
    @Column(name = "payment_method", length = 100)
    private String paymentMethod;
    
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
    
    // 외래키 제약조건 문제를 방지하기 위해 일시적으로 주석 처리
    // @OneToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "order_id", referencedColumnName = "order_id", insertable = false, updatable = false)
    // @JsonBackReference
    // private Order order;

    
    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Refund> refunds;
    
    public enum PaymentStatus {
        PAID, FAILED, REFUNDED, PARTIALLY_REFUNDED
    }
}
