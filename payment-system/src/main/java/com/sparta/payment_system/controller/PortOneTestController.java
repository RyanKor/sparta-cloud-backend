package com.sparta.payment_system.controller;

import com.sparta.payment_system.dto.PortOnePaymentRequest;
import com.sparta.payment_system.dto.PortOnePaymentResponse;
import com.sparta.payment_system.service.PortOneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/test/portone")
@CrossOrigin(origins = "*")
public class PortOneTestController {
    
    private final PortOneService portOneService;
    
    @Autowired
    public PortOneTestController(PortOneService portOneService) {
        this.portOneService = portOneService;
    }
    
    @PostMapping("/payment/test")
    public Mono<ResponseEntity<PortOnePaymentResponse>> testPayment() {
        // 테스트용 결제 요청 데이터 생성
        PortOnePaymentRequest request = new PortOnePaymentRequest();
        request.setOrderId("test_order_" + System.currentTimeMillis());
        request.setAmount(new BigDecimal("1000"));
        request.setCustomerEmail("test@example.com");
        request.setCustomerName("테스트 사용자");
        request.setCustomerPhone("010-1234-5678");
        request.setProductName("테스트 상품");
        request.setProductDescription("PortOne API 테스트용 상품입니다.");
        request.setReturnUrl("http://localhost:8080/api/test/portone/success");
        request.setCancelUrl("http://localhost:8080/api/test/portone/cancel");
        
        return portOneService.createPayment(request)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.badRequest().build());
    }
    
    @GetMapping("/payment/{impUid}")
    public Mono<ResponseEntity<PortOnePaymentResponse>> getPayment(@PathVariable String impUid) {
        return portOneService.getPayment(impUid)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/payment/{impUid}/cancel")
    public Mono<ResponseEntity<PortOnePaymentResponse>> cancelPayment(
            @PathVariable String impUid,
            @RequestParam(defaultValue = "테스트 취소") String reason) {
        return portOneService.cancelPayment(impUid, reason)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.badRequest().build());
    }
    
    @GetMapping("/success")
    public ResponseEntity<String> paymentSuccess() {
        return ResponseEntity.ok("결제가 성공적으로 완료되었습니다.");
    }
    
    @GetMapping("/cancel")
    public ResponseEntity<String> paymentCancel() {
        return ResponseEntity.ok("결제가 취소되었습니다.");
    }
}
