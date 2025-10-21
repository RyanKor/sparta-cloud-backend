package com.sparta.payment_system.controller;

import com.sparta.payment_system.service.PaymentService;
import com.sparta.payment_system.dto.PaymentRequestDto;
import com.sparta.payment_system.entity.Order;
import com.sparta.payment_system.entity.OrderItem;
import com.sparta.payment_system.entity.Payment;
import com.sparta.payment_system.entity.Product;
import com.sparta.payment_system.repository.OrderRepository;
import com.sparta.payment_system.repository.OrderItemRepository;
import com.sparta.payment_system.repository.PaymentRepository;
import com.sparta.payment_system.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {
    
    private final PaymentService paymentService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    
    @Autowired
    public PaymentController(PaymentService paymentService, OrderRepository orderRepository, OrderItemRepository orderItemRepository, PaymentRepository paymentRepository, ProductRepository productRepository) {
        this.paymentService = paymentService;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.productRepository = productRepository;
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
    
    // 특정 사용자의 PAID 결제 목록 조회
    @GetMapping("/paid/user/{userId}")
    public ResponseEntity<List<Payment>> getPaidPaymentsByUser(@PathVariable Long userId) {
        try {
            // 주문을 통해 사용자별 결제 조회
            List<Order> userOrders = orderRepository.findByUserId(userId);
            List<String> orderIds = userOrders.stream()
                    .map(Order::getOrderId)
                    .toList();
            
            List<Payment> paidPayments = paymentRepository.findByOrderIdInAndStatus(orderIds, Payment.PaymentStatus.PAID);
            return ResponseEntity.ok(paidPayments);
        } catch (Exception e) {
            System.err.println("사용자별 PAID 결제 목록 조회 오류: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // 통합 결제 요청 API (주문 + 결제 정보를 함께 처리)
    @PostMapping("/request")
    public ResponseEntity<String> requestPayment(@RequestBody PaymentRequestDto paymentRequest) {
        try {
            // 1. 주문 생성
            Order order = new Order();
            order.setOrderId(paymentRequest.getOrderId());
            order.setUserId(paymentRequest.getUserId());
            order.setTotalAmount(paymentRequest.getTotalAmount());
            order.setStatus(Order.OrderStatus.PENDING_PAYMENT);
            
            Order savedOrder = orderRepository.save(order);
            System.out.println("주문이 생성되었습니다. Order ID: " + savedOrder.getOrderId());
            
            // 2. 주문 아이템들 저장
            if (paymentRequest.getOrderItems() != null && !paymentRequest.getOrderItems().isEmpty()) {
                for (PaymentRequestDto.OrderItemDto itemDto : paymentRequest.getOrderItems()) {
                    // 상품 존재 여부 확인
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
            
            // 3. 결제 정보 저장 (임시로 PAID 상태로 저장 - 실제로는 포트원 결제 완료 후 업데이트)
            // 이 부분은 프론트엔드에서 포트원 결제 완료 후 /api/payments/complete를 호출하여 처리
            
            return ResponseEntity.ok("Payment request processed successfully. Order ID: " + savedOrder.getOrderId());
            
        } catch (Exception e) {
            System.err.println("결제 요청 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Payment request processing failed: " + e.getMessage());
        }
    }
}
