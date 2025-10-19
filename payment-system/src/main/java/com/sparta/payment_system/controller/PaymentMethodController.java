package com.sparta.payment_system.controller;

import com.sparta.payment_system.entity.PaymentMethod;
import com.sparta.payment_system.repository.PaymentMethodRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/payment-methods")
@CrossOrigin(origins = "*")
public class PaymentMethodController {
    
    private final PaymentMethodRepository paymentMethodRepository;
    
    @Autowired
    public PaymentMethodController(PaymentMethodRepository paymentMethodRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
    }
    
    @PostMapping
    public ResponseEntity<PaymentMethod> createPaymentMethod(@RequestBody PaymentMethod paymentMethod) {
        try {
            PaymentMethod savedPaymentMethod = paymentMethodRepository.save(paymentMethod);
            return ResponseEntity.ok(savedPaymentMethod);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping
    public ResponseEntity<List<PaymentMethod>> getAllPaymentMethods() {
        try {
            List<PaymentMethod> paymentMethods = paymentMethodRepository.findAll();
            return ResponseEntity.ok(paymentMethods);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PaymentMethod> getPaymentMethod(@PathVariable Long id) {
        try {
            Optional<PaymentMethod> paymentMethod = paymentMethodRepository.findById(id);
            return paymentMethod.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<PaymentMethod> updatePaymentMethod(@PathVariable Long id, @RequestBody PaymentMethod paymentMethodDetails) {
        try {
            Optional<PaymentMethod> paymentMethodOptional = paymentMethodRepository.findById(id);
            if (paymentMethodOptional.isPresent()) {
                PaymentMethod paymentMethod = paymentMethodOptional.get();
                paymentMethod.setUserId(paymentMethodDetails.getUserId());
                paymentMethod.setPgBillingKey(paymentMethodDetails.getPgBillingKey());
                paymentMethod.setCardType(paymentMethodDetails.getCardType());
                paymentMethod.setCardLast4(paymentMethodDetails.getCardLast4());
                paymentMethod.setIsDefault(paymentMethodDetails.getIsDefault());
                
                PaymentMethod updatedPaymentMethod = paymentMethodRepository.save(paymentMethod);
                return ResponseEntity.ok(updatedPaymentMethod);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePaymentMethod(@PathVariable Long id) {
        try {
            if (paymentMethodRepository.existsById(id)) {
                paymentMethodRepository.deleteById(id);
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PaymentMethod>> getPaymentMethodsByUser(@PathVariable Long userId) {
        try {
            List<PaymentMethod> paymentMethods = paymentMethodRepository.findByUserId(userId);
            return ResponseEntity.ok(paymentMethods);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/user/{userId}/default")
    public ResponseEntity<PaymentMethod> getDefaultPaymentMethod(@PathVariable Long userId) {
        try {
            Optional<PaymentMethod> paymentMethod = paymentMethodRepository.findByUserIdAndIsDefaultTrue(userId);
            return paymentMethod.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/billing-key/{pgBillingKey}")
    public ResponseEntity<PaymentMethod> getPaymentMethodByBillingKey(@PathVariable String pgBillingKey) {
        try {
            Optional<PaymentMethod> paymentMethod = paymentMethodRepository.findByPgBillingKey(pgBillingKey);
            return paymentMethod.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
