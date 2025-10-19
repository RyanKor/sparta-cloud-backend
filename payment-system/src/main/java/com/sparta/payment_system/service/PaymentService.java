package com.sparta.payment_system.service;

import com.sparta.payment_system.client.PortOneClient;
import com.sparta.payment_system.entity.Payment;
import com.sparta.payment_system.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class PaymentService {

    private final PortOneClient portoneClient;
    private final PaymentRepository paymentRepository;

    @Autowired
    public PaymentService(PortOneClient portoneClient, PaymentRepository paymentRepository) {
        this.portoneClient = portoneClient;
        this.paymentRepository = paymentRepository;
    }

    public Mono<Boolean> verifyPayment(String paymentId) {
        return portoneClient.getAccessToken()
                .flatMap(accessToken -> portoneClient.getPaymentDetails(paymentId, accessToken))
                .map(paymentDetails -> {
                    System.out.println("결제 정보 조회 결과: " + paymentDetails);
                    
                    // 1. 포트원에서 조회한 결제 상태가 "PAID"인지 확인
                    String status = (String) paymentDetails.get("status");
                    if (!"PAID".equals(status)) {
                        System.out.println("결제 상태 오류: " + status);
                        return false;
                    }

                    // 2. 결제 금액 정보 추출
                    Map<String, Object> amountInfo = (Map<String, Object>) paymentDetails.get("amount");
                    Integer paidAmount = (Integer) amountInfo.get("total");
                    
                    // 3. 주문 정보 추출
                    String orderName = (String) paymentDetails.get("orderName");
                    String merchantUid = (String) paymentDetails.get("merchantUid");
                    
                    System.out.println("결제 검증 성공!");
                    System.out.println("결제 ID: " + paymentId);
                    System.out.println("주문명: " + orderName);
                    System.out.println("주문 ID: " + merchantUid);
                    System.out.println("결제 금액: " + paidAmount);
                    
                    // 4. DB에 결제 정보 저장
                    savePaymentToDatabase(paymentId, merchantUid, paidAmount, paymentDetails);
                    
                    return true;
                })
                .onErrorReturn(false);
    }

    private void savePaymentToDatabase(String paymentId, String orderId, Integer amount, Map<String, Object> paymentDetails) {
        try {
            Payment payment = new Payment();
            payment.setOrderId(orderId);
            payment.setImpUid(paymentId);
            payment.setAmount(java.math.BigDecimal.valueOf(amount));
            payment.setStatus(Payment.PaymentStatus.PAID);
            payment.setPaymentMethod((String) paymentDetails.get("payMethod"));
            
            // 결제 완료 시간 설정
            String paidAt = (String) paymentDetails.get("paidAt");
            if (paidAt != null) {
                payment.setPaidAt(java.time.LocalDateTime.parse(paidAt.replace("Z", "")));
            }
            
            paymentRepository.save(payment);
            System.out.println("결제 정보가 데이터베이스에 저장되었습니다.");
        } catch (Exception e) {
            System.err.println("결제 정보 저장 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Mono<Boolean> cancelPayment(String paymentId, String reason) {
        return portoneClient.getAccessToken()
                .flatMap(accessToken -> portoneClient.cancelPayment(paymentId, accessToken, reason))
                .map(cancelResult -> {
                    System.out.println("결제 취소 결과: " + cancelResult);
                    return true;
                })
                .onErrorReturn(false);
    }
}
