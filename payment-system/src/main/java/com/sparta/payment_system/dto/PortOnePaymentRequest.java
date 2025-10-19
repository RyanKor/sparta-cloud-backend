package com.sparta.payment_system.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class PortOnePaymentRequest {
    
    private String orderId;
    private BigDecimal amount;
    private String customerEmail;
    private String customerName;
    private String customerPhone;
    private String productName;
    private String productDescription;
    private String returnUrl;
    private String cancelUrl;
    private String webhookUrl;
    
    public PortOnePaymentRequest(String orderId, BigDecimal amount, String customerEmail) {
        this.orderId = orderId;
        this.amount = amount;
        this.customerEmail = customerEmail;
    }
}
