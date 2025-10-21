package com.sparta.payment_system.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class RefundRequestDto {
    
    private Long paymentId;
    private BigDecimal amount; // 부분 환불 시 금액 (전체 환불 시 null)
    private String reason;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
}
