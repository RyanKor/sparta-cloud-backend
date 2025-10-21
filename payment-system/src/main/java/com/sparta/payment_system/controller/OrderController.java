package com.sparta.payment_system.controller;

import com.sparta.payment_system.entity.Order;
import com.sparta.payment_system.entity.OrderItem;
import com.sparta.payment_system.entity.Product;
import com.sparta.payment_system.repository.OrderRepository;
import com.sparta.payment_system.repository.OrderItemRepository;
import com.sparta.payment_system.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {
    
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    
    @Autowired
    public OrderController(OrderRepository orderRepository, OrderItemRepository orderItemRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
    }
    
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        try {
            // 주문 저장
            Order savedOrder = orderRepository.save(order);
            
            // 주문 아이템들 저장
            if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
                for (OrderItem orderItem : order.getOrderItems()) {
                    // 상품 존재 여부 확인
                    Optional<Product> productOptional = productRepository.findById(orderItem.getProductId());
                    if (productOptional.isEmpty()) {
                        System.err.println("상품을 찾을 수 없습니다. Product ID: " + orderItem.getProductId());
                        return ResponseEntity.badRequest().build();
                    }
                    
                    orderItem.setOrderId(savedOrder.getOrderId());
                    orderItemRepository.save(orderItem);
                }
                System.out.println("주문 아이템 " + order.getOrderItems().size() + "개가 저장되었습니다.");
            }
            
            return ResponseEntity.ok(savedOrder);
        } catch (Exception e) {
            System.err.println("주문 생성 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        try {
            List<Order> orders = orderRepository.findAll();
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable String orderId) {
        try {
            Optional<Order> order = orderRepository.findByOrderId(orderId);
            return order.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PutMapping("/{orderId}")
    public ResponseEntity<Order> updateOrder(@PathVariable String orderId, @RequestBody Order orderDetails) {
        try {
            Optional<Order> orderOptional = orderRepository.findByOrderId(orderId);
            if (orderOptional.isPresent()) {
                Order order = orderOptional.get();
                order.setUserId(orderDetails.getUserId());
                order.setTotalAmount(orderDetails.getTotalAmount());
                order.setStatus(orderDetails.getStatus());
                
                Order updatedOrder = orderRepository.save(order);
                return ResponseEntity.ok(updatedOrder);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOrder(@PathVariable String orderId) {
        try {
            Optional<Order> order = orderRepository.findByOrderId(orderId);
            if (order.isPresent()) {
                orderRepository.delete(order.get());
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getOrdersByUser(@PathVariable Long userId) {
        try {
            List<Order> orders = orderRepository.findByUserId(userId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Order>> getOrdersByStatus(@PathVariable Order.OrderStatus status) {
        try {
            List<Order> orders = orderRepository.findByStatus(status);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/user/{userId}/status/{status}")
    public ResponseEntity<List<Order>> getOrdersByUserAndStatus(
            @PathVariable Long userId, 
            @PathVariable Order.OrderStatus status) {
        try {
            List<Order> orders = orderRepository.findByUserIdAndStatus(userId, status);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
