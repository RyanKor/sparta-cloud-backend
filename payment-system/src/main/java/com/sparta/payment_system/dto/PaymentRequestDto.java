package com.sparta.payment_system.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PaymentRequestDto {
    
    // 주문 정보
    private String orderId;
    private Long userId;
    private BigDecimal totalAmount;
    private String orderName;
    
    // 주문 아이템들
    private List<OrderItemDto> orderItems;
    
    // 결제 정보
    private String paymentMethod;
    private String impUid;
    
    // 사용자 정보 (선택적)
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    
    @Getter
    @Setter
    @NoArgsConstructor
    public static class OrderItemDto {
        private Long productId;
        private Integer quantity;
        private BigDecimal price;
        private String productName;
    }
}
