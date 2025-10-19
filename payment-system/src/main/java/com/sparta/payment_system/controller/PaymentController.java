package com.sparta.payment_system.controller;

import com.sparta.payment_system.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {
    
    private final PaymentService paymentService;
    
    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    
    // 결제 완료 검증 API
    @PostMapping("/complete")
    public Mono<ResponseEntity<String>> completePayment(@RequestBody Map<String, String> request) {
        String paymentId = request.get("paymentId");
        System.out.println("결제 완료 검증 요청 받음 - Payment ID: " + paymentId);
        
        return paymentService.verifyPayment(paymentId)
                .map(isSuccess -> {
                    if (isSuccess) {
                        return ResponseEntity.ok("Payment verification successful.");
                    } else {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment verification failed.");
                    }
                });
    }
    
    // 결제 취소 API
    @PostMapping("/cancel")
    public Mono<ResponseEntity<String>> cancelPaymentByPaymentId(@RequestBody Map<String, String> request) {
        String paymentId = request.get("paymentId");
        String reason = request.getOrDefault("reason", "사용자 요청에 의한 취소");
        
        return paymentService.cancelPayment(paymentId, reason)
                .map(isSuccess -> {
                    if (isSuccess) {
                        return ResponseEntity.ok("Payment cancellation successful.");
                    } else {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment cancellation failed.");
                    }
                });
    }
}
