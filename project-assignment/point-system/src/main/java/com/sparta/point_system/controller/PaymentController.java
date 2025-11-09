package com.sparta.point_system.controller;

import com.sparta.point_system.dto.PaymentRequestDto;
import com.sparta.point_system.entity.Order;
import com.sparta.point_system.entity.OrderItem;
import com.sparta.point_system.entity.Payment;
import com.sparta.point_system.entity.Product;
import com.sparta.point_system.repository.OrderRepository;
import com.sparta.point_system.repository.OrderItemRepository;
import com.sparta.point_system.repository.PaymentRepository;
import com.sparta.point_system.repository.ProductRepository;
import com.sparta.point_system.service.PaymentService;
import com.sparta.point_system.service.PointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private PointService pointService;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private OrderItemRepository orderItemRepository;
    
    @Autowired
    private ProductRepository productRepository;

    @PostMapping("/payment")
    public Payment createPayment(@RequestParam String orderId,
                                @RequestParam(required = false) Long methodId,
                                @RequestParam(required = false) String impUid,
                                @RequestParam BigDecimal amount,
                                @RequestParam Payment.PaymentStatus status,
                                @RequestParam(required = false) String paymentMethod,
                                @RequestParam(required = false) LocalDateTime paidAt) {
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setMethodId(methodId);
        payment.setImpUid(impUid);
        payment.setAmount(amount);
        payment.setStatus(status);
        payment.setPaymentMethod(paymentMethod);
        payment.setPaidAt(paidAt);
        return paymentRepository.save(payment);
    }

    @GetMapping("/payments")
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    @GetMapping("/payment/{paymentId}")
    public Optional<Payment> getPaymentById(@PathVariable Long paymentId) {
        return paymentRepository.findById(paymentId);
    }

    @GetMapping("/payment/order/{orderId}")
    public Optional<Payment> getPaymentByOrderId(@PathVariable String orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    @GetMapping("/payments/status/{status}")
    public List<Payment> getPaymentsByStatus(@PathVariable Payment.PaymentStatus status) {
        return paymentRepository.findByStatus(status);
    }

    @PutMapping("/payment/{paymentId}")
    public Payment updatePayment(@PathVariable Long paymentId,
                                @RequestParam(required = false) BigDecimal amount,
                                @RequestParam(required = false) Payment.PaymentStatus status,
                                @RequestParam(required = false) String paymentMethod,
                                @RequestParam(required = false) LocalDateTime paidAt) {
        Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            if (amount != null) payment.setAmount(amount);
            if (status != null) payment.setStatus(status);
            if (paymentMethod != null) payment.setPaymentMethod(paymentMethod);
            if (paidAt != null) payment.setPaidAt(paidAt);
            return paymentRepository.save(payment);
        }
        throw new RuntimeException("Payment not found with id: " + paymentId);
    }

    @DeleteMapping("/payment/{paymentId}")
    public String deletePayment(@PathVariable Long paymentId) {
        paymentRepository.deleteById(paymentId);
        return "Payment deleted successfully";
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
    
    // 통합 결제 요청 API (주문 + 결제 정보를 함께 처리, 포인트 사용 포함)
    @PostMapping("/request")
    public ResponseEntity<String> requestPayment(@RequestBody PaymentRequestDto paymentRequest) {
        try {
            // 1. 포인트 사용 처리
            if (paymentRequest.getPointsUsed() != null && paymentRequest.getPointsUsed() > 0) {
                try {
                    pointService.usePoints(
                        paymentRequest.getUserId(),
                        paymentRequest.getPointsUsed(),
                        paymentRequest.getOrderId(),
                        "주문 결제 시 포인트 사용"
                    );
                } catch (RuntimeException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("포인트 사용 실패: " + e.getMessage());
                }
            }
            
            // 2. 주문 생성
            Order order = new Order();
            order.setOrderId(paymentRequest.getOrderId());
            order.setUserId(paymentRequest.getUserId());
            order.setTotalAmount(paymentRequest.getTotalAmount());
            order.setPointsUsed(paymentRequest.getPointsUsed() != null ? paymentRequest.getPointsUsed() : 0);
            order.setPointsDiscountAmount(paymentRequest.getPointsDiscountAmount() != null ? 
                    paymentRequest.getPointsDiscountAmount() : BigDecimal.ZERO);
            order.setStatus(Order.OrderStatus.PENDING_PAYMENT);
            
            Order savedOrder = orderRepository.save(order);
            System.out.println("주문이 생성되었습니다. Order ID: " + savedOrder.getOrderId());
            
            // 3. 주문 아이템들 저장
            if (paymentRequest.getOrderItems() != null && !paymentRequest.getOrderItems().isEmpty()) {
                for (PaymentRequestDto.OrderItemDto itemDto : paymentRequest.getOrderItems()) {
                    Optional<Product> productOptional = productRepository.findById(itemDto.getProductId());
                    if (productOptional.isEmpty()) {
                        System.err.println("상품을 찾을 수 없습니다. Product ID: " + itemDto.getProductId());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body("상품을 찾을 수 없습니다. Product ID: " + itemDto.getProductId());
                    }
                    
                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrderId(savedOrder.getOrderId());
                    orderItem.setProductId(itemDto.getProductId());
                    orderItem.setQuantity(itemDto.getQuantity());
                    orderItem.setPrice(itemDto.getPrice());
                    
                    orderItemRepository.save(orderItem);
                }
                System.out.println("주문 아이템 " + paymentRequest.getOrderItems().size() + "개가 저장되었습니다.");
            }
            
            return ResponseEntity.ok("Payment request processed successfully. Order ID: " + savedOrder.getOrderId());
            
        } catch (Exception e) {
            System.err.println("결제 요청 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Payment request processing failed: " + e.getMessage());
        }
    }
    
    // PAID 상태의 결제 목록 조회 (환불 가능한 결제들)
    @GetMapping("/paid")
    public ResponseEntity<List<Payment>> getPaidPayments() {
        try {
            List<Payment> paidPayments = paymentRepository.findByStatus(Payment.PaymentStatus.PAID);
            return ResponseEntity.ok(paidPayments);
        } catch (Exception e) {
            System.err.println("PAID 결제 목록 조회 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // 결제 취소 API (PortOne imp_uid 사용, 포인트 환불 포함)
    @PostMapping("/cancel")
    public Mono<ResponseEntity<Map<String, Object>>> cancelPayment(@RequestBody Map<String, String> request) {
        String paymentId = request.get("paymentId"); // PortOne의 imp_uid (문자열)
        String reason = request.getOrDefault("reason", "고객 요청에 의한 취소");
        
        System.out.println("결제 취소 요청 받음 - Payment ID (imp_uid): " + paymentId);
        
        if (paymentId == null || paymentId.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "paymentId는 필수입니다.");
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
        }
        
        // DB에서 Payment 조회 (imp_uid로)
        Optional<Payment> paymentOptional = paymentRepository.findByImpUid(paymentId);
        if (paymentOptional.isEmpty()) {
            // imp_uid로 찾지 못하면 orderId로 시도 (결제 ID가 orderId와 같은 경우)
            Optional<Payment> paymentByOrderId = paymentRepository.findByOrderId(paymentId);
            if (paymentByOrderId.isPresent()) {
                paymentOptional = paymentByOrderId;
            }
        }
        
        if (paymentOptional.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "결제 정보를 찾을 수 없습니다. Payment ID: " + paymentId);
            return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
        }
        
        Payment payment = paymentOptional.get();
        
        // 환불 가능 상태 확인
        if (payment.getStatus() != Payment.PaymentStatus.PAID && 
            payment.getStatus() != Payment.PaymentStatus.PARTIALLY_REFUNDED) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "환불할 수 없는 결제 상태입니다. 현재 상태: " + payment.getStatus());
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
        }
        
        // PortOne API로 환불 요청 (포인트 환불 로직은 PaymentService.cancelPayment 내부에서 처리됨)
        return paymentService.cancelPayment(paymentId, reason)
                .map(isSuccess -> {
                    Map<String, Object> response = new HashMap<>();
                    if (isSuccess) {
                        response.put("message", "결제 취소가 성공적으로 처리되었습니다. 사용한 포인트도 복구되었습니다.");
                        response.put("paymentId", paymentId);
                        response.put("impUid", payment.getImpUid());
                        response.put("orderId", payment.getOrderId());
                        response.put("refundAmount", payment.getAmount());
                        return ResponseEntity.ok(response);
                    } else {
                        response.put("error", "PortOne 결제 취소 요청이 실패했습니다.");
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                    }
                })
                .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "결제 취소 처리 중 오류가 발생했습니다.")));
    }
}

