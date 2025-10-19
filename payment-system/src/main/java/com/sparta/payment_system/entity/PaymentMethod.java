package com.sparta.payment_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_methods")
@Getter
@Setter
@NoArgsConstructor
public class PaymentMethod {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "method_id")
    private Long methodId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "pg_billing_key", nullable = false, unique = true, length = 255)
    private String pgBillingKey;
    
    @Column(name = "card_type", length = 50)
    private String cardType;
    
    @Column(name = "card_last4", length = 4)
    private String cardLast4;
    
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
