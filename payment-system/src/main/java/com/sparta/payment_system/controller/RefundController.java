package com.sparta.payment_system.controller;

import com.sparta.payment_system.entity.Refund;
import com.sparta.payment_system.entity.Payment;
import com.sparta.payment_system.dto.RefundRequestDto;
import com.sparta.payment_system.repository.RefundRepository;
import com.sparta.payment_system.repository.PaymentRepository;
import com.sparta.payment_system.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/refunds")
@CrossOrigin(origins = "*")
public class RefundController {
    
    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    
    @Autowired
    public RefundController(RefundRepository refundRepository, PaymentRepository paymentRepository, PaymentService paymentService) {
        this.refundRepository = refundRepository;
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
    }
    
    
    // 환불 요청 API (PortOne API 호출 + DB 반영)
    @PostMapping("/request")
    public Mono<ResponseEntity<String>> requestRefund(@RequestBody RefundRequestDto refundRequest) {
        try {
            System.out.println("환불 요청 받음: " + refundRequest);
            
            // 1. 결제 정보 조회 및 검증
            Optional<Payment> paymentOptional = paymentRepository.findById(refundRequest.getPaymentId());
            if (paymentOptional.isEmpty()) {
                return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("결제 정보를 찾을 수 없습니다. Payment ID: " + refundRequest.getPaymentId()));
            }
            
            Payment payment = paymentOptional.get();
            
            // 2. 환불 가능 상태 확인
            if (payment.getStatus() != Payment.PaymentStatus.PAID && 
                payment.getStatus() != Payment.PaymentStatus.PARTIALLY_REFUNDED) {
                return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("환불할 수 없는 결제 상태입니다. 현재 상태: " + payment.getStatus()));
            }
            
            // 3. 환불 금액 설정 (부분 환불 또는 전체 환불)
            final BigDecimal refundAmount = refundRequest.getAmount() != null ? 
                    refundRequest.getAmount() : payment.getAmount();
            
            // 4. 환불 금액 검증
            if (refundAmount.compareTo(BigDecimal.ZERO) <= 0 || 
                refundAmount.compareTo(payment.getAmount()) > 0) {
                return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("잘못된 환불 금액입니다. 환불 금액: " + refundAmount + ", 결제 금액: " + payment.getAmount()));
            }
            
            // 5. PortOne API로 환불 요청
            final String reason = refundRequest.getReason() != null ? refundRequest.getReason() : "사용자 요청에 의한 환불";
            
            return paymentService.cancelPayment(payment.getImpUid(), reason)
                    .map(isSuccess -> {
                        if (isSuccess) {
                            // 6. 환불 성공 시 DB에 환불 정보 저장
                            try {
                                Refund refund = new Refund();
                                refund.setPaymentId(payment.getPaymentId());
                                refund.setAmount(refundAmount);
                                refund.setReason(reason);
                                refund.setStatus(Refund.RefundStatus.COMPLETED);
                                
                                refundRepository.save(refund);
                                
                                // 7. 결제 상태 업데이트
                                if (refundAmount.compareTo(payment.getAmount()) >= 0) {
                                    payment.setStatus(Payment.PaymentStatus.REFUNDED);
                                } else {
                                    payment.setStatus(Payment.PaymentStatus.PARTIALLY_REFUNDED);
                                }
                                paymentRepository.save(payment);
                                
                                System.out.println("환불 처리 완료 - Payment ID: " + payment.getPaymentId() + 
                                        ", Refund Amount: " + refundAmount);
                                
                                return ResponseEntity.ok("환불이 성공적으로 처리되었습니다. 환불 금액: " + refundAmount);
                            } catch (Exception e) {
                                System.err.println("환불 DB 저장 중 오류: " + e.getMessage());
                                e.printStackTrace();
                                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body("환불은 성공했으나 DB 저장 중 오류가 발생했습니다: " + e.getMessage());
                            }
                        } else {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body("PortOne 환불 요청이 실패했습니다.");
                        }
                    })
                    .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("환불 처리 중 오류가 발생했습니다."));
                            
        } catch (Exception e) {
            System.err.println("환불 요청 처리 중 오류: " + e.getMessage());
            e.printStackTrace();
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("환불 요청 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
