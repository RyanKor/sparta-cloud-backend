package com.sparta.payment_system.controller;

import com.sparta.payment_system.entity.OrderItem;
import com.sparta.payment_system.repository.OrderItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/order-items")
@CrossOrigin(origins = "*")
public class OrderItemController {
    
    private final OrderItemRepository orderItemRepository;
    
    @Autowired
    public OrderItemController(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }
    
    @PostMapping
    public ResponseEntity<OrderItem> createOrderItem(@RequestBody OrderItem orderItem) {
        try {
            OrderItem savedOrderItem = orderItemRepository.save(orderItem);
            return ResponseEntity.ok(savedOrderItem);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping
    public ResponseEntity<List<OrderItem>> getAllOrderItems() {
        try {
            List<OrderItem> orderItems = orderItemRepository.findAll();
            return ResponseEntity.ok(orderItems);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderItem> getOrderItem(@PathVariable Long id) {
        try {
            Optional<OrderItem> orderItem = orderItemRepository.findById(id);
            return orderItem.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<OrderItem> updateOrderItem(@PathVariable Long id, @RequestBody OrderItem orderItemDetails) {
        try {
            Optional<OrderItem> orderItemOptional = orderItemRepository.findById(id);
            if (orderItemOptional.isPresent()) {
                OrderItem orderItem = orderItemOptional.get();
                orderItem.setOrderId(orderItemDetails.getOrderId());
                orderItem.setProductId(orderItemDetails.getProductId());
                orderItem.setQuantity(orderItemDetails.getQuantity());
                orderItem.setPrice(orderItemDetails.getPrice());
                
                OrderItem updatedOrderItem = orderItemRepository.save(orderItem);
                return ResponseEntity.ok(updatedOrderItem);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrderItem(@PathVariable Long id) {
        try {
            if (orderItemRepository.existsById(id)) {
                orderItemRepository.deleteById(id);
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<OrderItem>> getOrderItemsByOrder(@PathVariable String orderId) {
        try {
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
            return ResponseEntity.ok(orderItems);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<OrderItem>> getOrderItemsByProduct(@PathVariable Long productId) {
        try {
            List<OrderItem> orderItems = orderItemRepository.findByProductId(productId);
            return ResponseEntity.ok(orderItems);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
