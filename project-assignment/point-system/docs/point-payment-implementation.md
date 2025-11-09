# 포인트 기반 결제 기능 구현

## 목적

- Spring Boot를 활용하여 포인트 기반 결제 시스템의 백엔드 API를 구현합니다.
- 프론트엔드에서 호출할 수 있는 REST API 엔드포인트 구현를 구현합니다.

---

## 구현 내용

### 1. 포인트 잔액 조회 API

**엔드포인트**: `GET /api/points/balance/{userId}`

**주요 처리**:
- 사용자의 포인트 잔액 조회
- `PointService`를 통해 포인트 거래 내역 집계

**구현 힌트**:
```java
@RestController
@RequestMapping("/api/points")
public class PointController {
    
    @Autowired
    private PointService pointService;
    
    @GetMapping("/balance/{userId}")
    public ResponseEntity<Map<String, Object>> getPointBalance(@PathVariable Long userId) {
        Integer balance = pointService.getPointBalance(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("balance", balance);
        return ResponseEntity.ok(response);
    }
}
```

---

### 2. 포인트 충전 API

**엔드포인트**: `POST /api/points/charge/{userId}`

**주요 처리**:
- 포인트 충전 처리
- 포인트 거래 내역 기록
- 충전 후 새로운 잔액 반환

**구현 힌트**:
```java
@PostMapping("/charge/{userId}")
public ResponseEntity<Map<String, Object>> chargePoints(
        @PathVariable Long userId,
        @RequestParam(required = false, defaultValue = "100000") Integer points,
        @RequestParam(required = false) String description) {
    
    pointService.chargePoints(userId, points, description);
    Integer newBalance = pointService.getPointBalance(userId);
    
    Map<String, Object> response = new HashMap<>();
    response.put("userId", userId);
    response.put("chargedPoints", points);
    response.put("newBalance", newBalance);
    return ResponseEntity.ok(response);
}
```

---

### 3. 멤버십 정보 조회 API

**엔드포인트**: `GET /api/membership/user/{userId}/info`

**주요 처리**:
- 사용자의 멤버십 정보 조회
- 멤버십 등급 정보 조회
- 총 결제 금액 계산 및 반환

**구현 힌트**:
```java
@RestController
@RequestMapping("/api")
public class MembershipController {
    
    @Autowired
    private MembershipService membershipService;
    
    @GetMapping("/membership/user/{userId}/info")
    public ResponseEntity<Map<String, Object>> getMembershipInfo(@PathVariable Long userId) {
        MembershipService.MembershipWithLevel membershipWithLevel = 
            membershipService.getMembershipWithLevel(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("membership", membershipWithLevel.getMembership());
        response.put("level", membershipWithLevel.getLevel());
        response.put("totalPaymentAmount", membershipWithLevel.getTotalPaymentAmount());
        
        return ResponseEntity.ok(response);
    }
}
```

---

### 4. 결제 내역 조회 API

**엔드포인트**: `GET /api/membership/user/{userId}/payments`

**주요 처리**:
- 완료된 주문 목록 조회
- 결제 완료된 결제 목록 조회
- 취소된 주문 목록 조회
- 총 결제 금액 계산

**구현 힌트**:
```java
@GetMapping("/membership/user/{userId}/payments")
public ResponseEntity<Map<String, Object>> getUserPaymentHistory(@PathVariable Long userId) {
    // 완료된 주문 조회
    List<Order> completedOrders = orderRepository.findByUserIdAndStatus(
        userId, Order.OrderStatus.COMPLETED);
    
    // 취소된 주문 조회
    List<Order> cancelledOrders = orderRepository.findByUserIdAndStatus(
        userId, Order.OrderStatus.CANCELLED);
    
    // 총 결제 금액 계산
    BigDecimal totalPaidAmount = membershipService.calculateTotalPaidAmount(userId);
    
    Map<String, Object> response = new HashMap<>();
    response.put("completedOrders", completedOrders);
    response.put("cancelledOrders", cancelledOrders);
    response.put("totalPaidAmount", totalPaidAmount);
    
    return ResponseEntity.ok(response);
}
```

---

### 5. 통합 결제 요청 API

**엔드포인트**: `POST /api/payments/request`

**주요 처리**:
1. 포인트 사용 처리 (선택사항)
2. 주문 생성 및 저장
3. 주문 아이템 저장
4. 주문 상태를 `PENDING_PAYMENT`로 설정

**구현 힌트**:
```java
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    
    @Autowired
    private PointService pointService;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @PostMapping("/request")
    @Transactional
    public ResponseEntity<String> requestPayment(@RequestBody PaymentRequestDto request) {
        // 1. 포인트 사용 처리
        if (request.getPointsUsed() != null && request.getPointsUsed() > 0) {
            pointService.usePoints(
                request.getUserId(),
                request.getPointsUsed(),
                request.getOrderId(),
                "주문 결제 시 포인트 사용"
            );
        }
        
        // 2. 주문 생성
        Order order = new Order();
        order.setOrderId(request.getOrderId());
        order.setUserId(request.getUserId());
        order.setTotalAmount(request.getTotalAmount());
        order.setPointsUsed(request.getPointsUsed());
        order.setStatus(Order.OrderStatus.PENDING_PAYMENT);
        orderRepository.save(order);
        
        // 3. 주문 아이템 저장
        // ...
        
        return ResponseEntity.ok("Payment request processed successfully");
    }
}
```

---

### 6. 포인트 전액 결제 완료 API

**엔드포인트**: `POST /api/payments/complete-point-payment`

**주요 처리**:
1. 주문 상태를 `COMPLETED`로 변경
2. Payment 레코드 생성 (결제 방법: POINT)
3. 멤버십 등급에 따른 포인트 적립
4. 멤버십 등급 자동 업데이트

**구현 힌트**:
```java
@PostMapping("/complete-point-payment")
@Transactional
public ResponseEntity<Map<String, Object>> completePointPayment(
        @RequestBody Map<String, String> request) {
    
    String orderId = request.get("orderId");
    Order order = orderRepository.findByOrderId(orderId)
        .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다"));
    
    // 1. 주문 상태 변경
    order.setStatus(Order.OrderStatus.COMPLETED);
    orderRepository.save(order);
    
    // 2. Payment 레코드 생성
    Payment payment = new Payment();
    payment.setOrderId(orderId);
    payment.setAmount(order.getTotalAmount());
    payment.setStatus(Payment.PaymentStatus.PAID);
    payment.setPaymentMethod("POINT");
    paymentRepository.save(payment);
    
    // 3. 포인트 적립 (멤버십 등급 반영)
    Long userId = order.getUserId();
    Integer pointsEarned = membershipService.calculateEarnedPoints(
        userId, order.getTotalAmount());
    pointService.earnPoints(userId, pointsEarned, orderId, "포인트 결제 완료", null);
    
    // 4. 멤버십 등급 업데이트
    membershipService.updateMembershipLevel(userId);
    
    Map<String, Object> response = new HashMap<>();
    response.put("pointsEarned", pointsEarned);
    return ResponseEntity.ok(response);
}
```

---

### 7. PortOne 결제 완료 검증 API

**엔드포인트**: `POST /api/payments/complete`

**주요 처리**:
1. PortOne API로 결제 정보 조회 및 검증
2. 결제 상태 확인 (PAID 여부)
3. Payment 레코드 생성 및 저장
4. 주문 상태를 `COMPLETED`로 변경
5. 멤버십 등급에 따른 포인트 적립
6. 멤버십 등급 자동 업데이트

**구현 힌트**:
```java
@PostMapping("/complete")
public Mono<ResponseEntity<String>> completePayment(@RequestBody Map<String, String> request) {
    String paymentId = request.get("paymentId");
    
    return paymentService.verifyPayment(paymentId)
        .map(isSuccess -> {
            if (isSuccess) {
                return ResponseEntity.ok("Payment verified and saved successfully");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Payment verification failed");
            }
        });
}

// PaymentService 내부
@Transactional
public void savePaymentToDatabase(String paymentId, String orderId, 
                                  Integer amount, Map<String, Object> paymentDetails) {
    // Payment 레코드 생성
    Payment payment = new Payment();
    payment.setImpUid(paymentId);
    payment.setOrderId(orderId);
    payment.setAmount(BigDecimal.valueOf(amount));
    payment.setStatus(Payment.PaymentStatus.PAID);
    paymentRepository.save(payment);
    
    // 주문 상태 변경
    Order order = orderRepository.findByOrderId(orderId).orElseThrow();
    order.setStatus(Order.OrderStatus.COMPLETED);
    orderRepository.save(order);
    
    // 포인트 적립 및 멤버십 등급 업데이트
    Long userId = order.getUserId();
    Integer pointsEarned = membershipService.calculateEarnedPoints(
        userId, BigDecimal.valueOf(amount));
    pointService.earnPoints(userId, pointsEarned, orderId, "결제 완료", null);
    membershipService.updateMembershipLevel(userId);
}
```

---

### 8. 주문 조회 API

**엔드포인트**: `GET /api/order/{orderId}`

**주요 처리**:
- 주문 ID로 주문 정보 조회
- 주문 상태, 결제 정보 반환

**구현 힌트**:
```java
@RestController
@RequestMapping("/api")
public class OrderController {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @GetMapping("/order/{orderId}")
    public ResponseEntity<Order> getOrderById(@PathVariable String orderId) {
        Optional<Order> order = orderRepository.findByOrderId(orderId);
        return order.map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
```

---

### 9. 상품 정보 조회 API

**엔드포인트**: `GET /api/product/{productId}`

**주요 처리**:
- 상품 ID로 상품 정보 조회

**구현 힌트**:
```java
@RestController
@RequestMapping("/api")
public class ProductController {
    
    @Autowired
    private ProductRepository productRepository;
    
    @GetMapping("/product/{productId}")
    public ResponseEntity<Product> getProduct(@PathVariable Long productId) {
        Optional<Product> product = productRepository.findById(productId);
        return product.map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
```

---

## 주요 고려사항

### 1. 트랜잭션 관리
- `@Transactional` 어노테이션을 활용하여 주문 생성, 포인트 차감, 결제 처리를 하나의 트랜잭션으로 관리
- 결제 실패 시 모든 변경사항 롤백

### 2. 포인트 처리
- 포인트 사용 시 잔액 확인 및 차감
- 포인트 적립 시 멤버십 등급에 따른 차등 적립률 적용
- 포인트 거래 내역은 `PointTransaction` 엔티티에 기록

### 3. 멤버십 등급 관리
- 결제 완료 시 총 결제 금액 재계산
- 총 결제 금액에 따라 멤버십 등급 자동 업데이트
- `MembershipService`를 통해 등급 관리 로직 분리

### 4. 에러 처리
- 포인트 잔액 부족 시 적절한 에러 메시지 반환
- 주문/결제 정보 조회 실패 시 404 응답
- 예외 발생 시 상세한 에러 메시지 제공

### 5. PortOne API 연동
- `PortOneClient`를 통해 PortOne API 호출
- Reactor Core의 `Mono`를 활용한 비동기 처리
- 결제 검증 및 취소 처리

---

## 참고사항

- **API 엔드포인트 상세 정보**: `api-endpoints.md` 파일 참고
- **결제 플로우 설계**: `point-payment-flow-design.md` 파일 참고
- **실제 구현 예시**: `src/main/java/com/sparta/point_system/controller/` 패키지 참고
