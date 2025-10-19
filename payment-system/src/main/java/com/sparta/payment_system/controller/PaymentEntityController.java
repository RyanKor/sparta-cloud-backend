package com.sparta.payment_system.controller;

import com.sparta.payment_system.entity.Payment;
import com.sparta.payment_system.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/payment-entities")
@CrossOrigin(origins = "*")
public class PaymentEntityController {
    
    private final PaymentRepository paymentRepository;
    
    @Autowired
    public PaymentEntityController(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }
    
    @PostMapping
    public ResponseEntity<Payment> createPayment(@RequestBody Payment payment) {
        try {
            Payment savedPayment = paymentRepository.save(payment);
            return ResponseEntity.ok(savedPayment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {
        try {
            List<Payment> payments = paymentRepository.findAll();
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPayment(@PathVariable Long id) {
        try {
            Optional<Payment> payment = paymentRepository.findById(id);
            return payment.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Payment> updatePayment(@PathVariable Long id, @RequestBody Payment paymentDetails) {
        try {
            Optional<Payment> paymentOptional = paymentRepository.findById(id);
            if (paymentOptional.isPresent()) {
                Payment payment = paymentOptional.get();
                payment.setOrderId(paymentDetails.getOrderId());
                payment.setMethodId(paymentDetails.getMethodId());
                payment.setImpUid(paymentDetails.getImpUid());
                payment.setAmount(paymentDetails.getAmount());
                payment.setStatus(paymentDetails.getStatus());
                payment.setPaymentMethod(paymentDetails.getPaymentMethod());
                payment.setPaidAt(paymentDetails.getPaidAt());
                
                Payment updatedPayment = paymentRepository.save(payment);
                return ResponseEntity.ok(updatedPayment);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable Long id) {
        try {
            if (paymentRepository.existsById(id)) {
                paymentRepository.deleteById(id);
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/order/{orderId}")
    public ResponseEntity<Payment> getPaymentByOrder(@PathVariable String orderId) {
        try {
            Optional<Payment> payment = paymentRepository.findByOrderId(orderId);
            return payment.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/imp-uid/{impUid}")
    public ResponseEntity<Payment> getPaymentByImpUid(@PathVariable String impUid) {
        try {
            Optional<Payment> payment = paymentRepository.findByImpUid(impUid);
            return payment.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Payment>> getPaymentsByStatus(@PathVariable Payment.PaymentStatus status) {
        try {
            List<Payment> payments = paymentRepository.findByStatus(status);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/method/{methodId}")
    public ResponseEntity<List<Payment>> getPaymentsByMethod(@PathVariable Long methodId) {
        try {
            List<Payment> payments = paymentRepository.findByMethodId(methodId);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
