package com.sparta.payment_system.controller;

import com.sparta.payment_system.entity.Refund;
import com.sparta.payment_system.repository.RefundRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/refunds")
@CrossOrigin(origins = "*")
public class RefundController {
    
    private final RefundRepository refundRepository;
    
    @Autowired
    public RefundController(RefundRepository refundRepository) {
        this.refundRepository = refundRepository;
    }
    
    @PostMapping
    public ResponseEntity<Refund> createRefund(@RequestBody Refund refund) {
        try {
            Refund savedRefund = refundRepository.save(refund);
            return ResponseEntity.ok(savedRefund);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping
    public ResponseEntity<List<Refund>> getAllRefunds() {
        try {
            List<Refund> refunds = refundRepository.findAll();
            return ResponseEntity.ok(refunds);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Refund> getRefund(@PathVariable Long id) {
        try {
            Optional<Refund> refund = refundRepository.findById(id);
            return refund.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Refund> updateRefund(@PathVariable Long id, @RequestBody Refund refundDetails) {
        try {
            Optional<Refund> refundOptional = refundRepository.findById(id);
            if (refundOptional.isPresent()) {
                Refund refund = refundOptional.get();
                refund.setPaymentId(refundDetails.getPaymentId());
                refund.setAmount(refundDetails.getAmount());
                refund.setReason(refundDetails.getReason());
                refund.setStatus(refundDetails.getStatus());
                
                Refund updatedRefund = refundRepository.save(refund);
                return ResponseEntity.ok(updatedRefund);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRefund(@PathVariable Long id) {
        try {
            if (refundRepository.existsById(id)) {
                refundRepository.deleteById(id);
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<List<Refund>> getRefundsByPayment(@PathVariable Long paymentId) {
        try {
            List<Refund> refunds = refundRepository.findByPaymentId(paymentId);
            return ResponseEntity.ok(refunds);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Refund>> getRefundsByStatus(@PathVariable Refund.RefundStatus status) {
        try {
            List<Refund> refunds = refundRepository.findByStatus(status);
            return ResponseEntity.ok(refunds);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
