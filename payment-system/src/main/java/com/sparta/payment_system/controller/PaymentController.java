package com.sparta.payment_system.controller;

import com.sparta.payment_system.dto.PortOnePaymentRequest;
import com.sparta.payment_system.dto.PortOnePaymentResponse;
import com.sparta.payment_system.service.PortOneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {
    
    private final PortOneService portOneService;
    
    @Autowired
    public PaymentController(PortOneService portOneService) {
        this.portOneService = portOneService;
    }
    
    @PostMapping("/request")
    public Mono<ResponseEntity<PortOnePaymentResponse>> requestPayment(@RequestBody PortOnePaymentRequest request) {
        return portOneService.createPayment(request)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.badRequest().build());
    }
    
    @GetMapping("/{impUid}")
    public Mono<ResponseEntity<PortOnePaymentResponse>> getPayment(@PathVariable String impUid) {
        return portOneService.getPayment(impUid)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/{impUid}/cancel")
    public Mono<ResponseEntity<PortOnePaymentResponse>> cancelPayment(
            @PathVariable String impUid,
            @RequestParam String reason) {
        return portOneService.cancelPayment(impUid, reason)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.badRequest().build());
    }
}
