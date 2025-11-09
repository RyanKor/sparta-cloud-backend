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

### 10. 포인트 기반 결제 환불 구현

**목적**:
사용자가 결제를 취소하거나 환불을 요청할 경우, 사용된 포인트를 복구하고 적립된 포인트를 취소하며, 총 결제 금액 변동에 따라 멤버십 등급을 자동으로 갱신하는 기능을 구현합니다. 이는 결제 시스템의 완전성과 사용자 경험을 보장하는 데 필수적입니다.

**엔드포인트**: `POST /api/refunds/request`, `POST /api/payments/cancel`

**주요 처리**:
1. 결제 상태 확인 (PAID 상태만 환불 가능)
2. 사용한 포인트 복구 (`point_transactions`에서 SPENT 타입 거래 확인)
3. 적립된 포인트 취소 (`point_transactions`에서 EARNED 타입 거래 확인)
4. 주문 상태를 `CANCELLED`로 변경
5. 결제 상태를 `REFUNDED`로 변경
6. 환불 레코드 생성
7. 멤버십 등급 자동 업데이트

**구현 힌트**:
```java
@RestController
@RequestMapping("/api/refunds")
public class RefundController {
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private PointService pointService;
    
    @Autowired
    private MembershipService membershipService;
    
    @PostMapping("/request")
    public Mono<ResponseEntity<Map<String, Object>>> requestRefund(
            @RequestBody Map<String, Object> refundRequest) {
        
        Long paymentId = Long.parseLong(refundRequest.get("paymentId").toString());
        Optional<Payment> paymentOptional = paymentRepository.findById(paymentId);
        
        if (paymentOptional.isEmpty()) {
            return Mono.just(ResponseEntity.notFound().build());
        }
        
        Payment payment = paymentOptional.get();
        
        // 환불 가능 상태 확인
        if (payment.getStatus() != Payment.PaymentStatus.PAID) {
            return Mono.just(ResponseEntity.badRequest()
                .body(Map.of("error", "환불할 수 없는 결제 상태입니다")));
        }
        
        // PortOne 결제 취소 또는 포인트 전액 결제 환불 처리
        if (payment.getImpUid() != null && !payment.getImpUid().isEmpty()) {
            // PortOne 결제 취소
            return paymentService.cancelPayment(payment.getImpUid(), reason)
                .map(isSuccess -> {
                    if (isSuccess) {
                        return ResponseEntity.ok(Map.of("message", "환불 완료"));
                    } else {
                        return ResponseEntity.badRequest()
                            .body(Map.of("error", "환불 처리 실패"));
                    }
                });
        } else {
            // 포인트 전액 결제 환불 처리
            return processPointRefund(payment);
        }
    }
}

// PaymentService 내부
@Transactional
public void updateDatabaseAfterCancel(String orderId, String reason) {
    Optional<Payment> paymentOptional = paymentRepository.findByOrderId(orderId);
    if (paymentOptional.isEmpty()) return;
    
    Payment payment = paymentOptional.get();
    Optional<Order> orderOptional = orderRepository.findByOrderId(orderId);
    
    if (orderOptional.isPresent()) {
        Order order = orderOptional.get();
        Long userId = order.getUserId();
        
        // 1. 사용한 포인트 복구
        List<PointTransaction> orderTransactions = 
            pointService.getPointTransactionsByOrderId(orderId);
        
        Integer spentPointsForOrder = orderTransactions.stream()
            .filter(t -> t.getType() == TransactionType.SPENT && t.getPoints() < 0)
            .mapToInt(t -> Math.abs(t.getPoints()))
            .sum();
        
        if (spentPointsForOrder > 0) {
            pointService.refundPoints(userId, spentPointsForOrder, orderId, 
                "주문 취소로 인한 포인트 환불");
        }
        
        // 2. 적립된 포인트 취소
        Integer pointsEarned = membershipService.calculateEarnedPoints(
            userId, order.getTotalAmount());
        
        if (pointsEarned > 0) {
            List<PointTransaction> transactions = pointService.getPointTransactions(userId);
            Integer earnedPointsForOrder = transactions.stream()
                .filter(t -> orderId.equals(t.getOrderId()) 
                        && t.getType() == TransactionType.EARNED
                        && t.getPoints() > 0)
                .mapToInt(PointTransaction::getPoints)
                .sum();
            
            if (earnedPointsForOrder > 0) {
                pointService.cancelEarnedPoints(userId, earnedPointsForOrder, orderId,
                    "주문 취소로 인한 포인트 적립 취소");
            }
        }
        
        // 3. 주문 상태 변경
        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);
        
        // 4. 결제 상태 변경
        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        paymentRepository.save(payment);
        
        // 5. 멤버십 등급 자동 업데이트
        membershipService.updateMembershipLevel(userId);
    }
}
```

---

### 11. 포인트 기반 멤버십 등급 관리 구현

**목적**:
사용자의 총 결제 금액에 따라 멤버십 등급(Normal, VIP, VVIP)을 자동으로 부여하고 관리하며, 각 등급에 따라 차등화된 포인트 적립률(1%, 5%, 10%)을 적용하는 시스템을 구축합니다. 이는 사용자 충성도를 높이고 차별화된 혜택을 제공하기 위함입니다.

**주요 처리**:
1. 멤버십 등급 정의 및 초기화
2. 총 결제 금액 계산
3. 등급 결정 및 업데이트
4. 포인트 적립률 적용
5. API 제공

**구현 힌트**:

#### 1. 멤버십 등급 엔티티 정의
```java
@Entity
@Table(name = "membership_levels")
public class MembershipLevel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long levelId;
    
    @Column(nullable = false, unique = true)
    private String name; // Normal, VIP, VVIP
    
    @Column(nullable = false)
    private BigDecimal pointAccrualRate; // 0.01, 0.05, 0.10
    
    private String benefitsDescription;
}
```

#### 2. 총 결제 금액 계산
```java
@Service
public class MembershipService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    /**
     * 사용자의 총 결제 금액 계산 (COMPLETED 주문만 집계)
     */
    public BigDecimal calculateTotalPaidAmount(Long userId) {
        List<Order> completedOrders = orderRepository.findByUserIdAndStatus(
            userId, Order.OrderStatus.COMPLETED);
        
        List<String> orderIds = completedOrders.stream()
            .map(Order::getOrderId)
            .collect(Collectors.toList());
        
        List<Payment> paidPayments = paymentRepository.findByOrderIdInAndStatus(
            orderIds, Payment.PaymentStatus.PAID);
        
        BigDecimal totalAmount = paidPayments.stream()
            .map(Payment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return totalAmount;
    }
}
```

#### 3. 등급 결정 및 업데이트
```java
/**
 * 총 결제 금액에 따른 멤버십 등급 결정
 * - 5만원 이하: Normal (1%)
 * - 10만원 이하: VIP (5%)
 * - 15만원 이상: VVIP (10%)
 */
@Transactional
public Long determineMembershipLevel(BigDecimal totalPaymentAmount) {
    // Normal: 50,000원 이하
    if (totalPaymentAmount.compareTo(new BigDecimal("50000")) <= 0) {
        MembershipLevel normalLevel = createDefaultMembershipLevel("Normal");
        return normalLevel.getLevelId();
    }
    
    // VIP: 100,000원 이하
    if (totalPaymentAmount.compareTo(new BigDecimal("100000")) <= 0) {
        MembershipLevel vipLevel = createDefaultMembershipLevel("VIP");
        return vipLevel.getLevelId();
    }
    
    // VVIP: 150,000원 이상
    MembershipLevel vvipLevel = createDefaultMembershipLevel("VVIP");
    return vvipLevel.getLevelId();
}

/**
 * 사용자의 멤버십 등급 자동 업데이트
 */
@Transactional
public Membership updateMembershipLevel(Long userId) {
    BigDecimal totalPaymentAmount = calculateTotalPaidAmount(userId);
    Long newLevelId = determineMembershipLevel(totalPaymentAmount);
    
    Optional<Membership> membershipOpt = membershipRepository.findByUserId(userId);
    Membership membership;
    
    if (membershipOpt.isPresent()) {
        membership = membershipOpt.get();
        membership.setLevelId(newLevelId);
    } else {
        membership = new Membership();
        membership.setUserId(userId);
        membership.setLevelId(newLevelId);
    }
    
    return membershipRepository.save(membership);
}

/**
 * 기본 멤버십 등급 생성 (없을 경우)
 */
@Transactional
private MembershipLevel createDefaultMembershipLevel(String levelName) {
    Optional<MembershipLevel> existingLevel = membershipLevelRepository.findByName(levelName);
    if (existingLevel.isPresent()) {
        return existingLevel.get();
    }
    
    MembershipLevel newLevel = new MembershipLevel();
    newLevel.setName(levelName);
    
    // 등급별 적립률 설정
    if ("Normal".equals(levelName)) {
        newLevel.setPointAccrualRate(new BigDecimal("0.01"));
        newLevel.setBenefitsDescription("일반 등급 - 기본 1% 포인트 적립");
    } else if ("VIP".equals(levelName)) {
        newLevel.setPointAccrualRate(new BigDecimal("0.05"));
        newLevel.setBenefitsDescription("우수 등급 - 5% 포인트 적립");
    } else if ("VVIP".equals(levelName)) {
        newLevel.setPointAccrualRate(new BigDecimal("0.10"));
        newLevel.setBenefitsDescription("최우수 등급 - 10% 포인트 적립");
    }
    
    return membershipLevelRepository.save(newLevel);
}
```

#### 4. 포인트 적립률 적용
```java
/**
 * 사용자의 멤버십 등급에 따른 포인트 적립률 조회
 */
public BigDecimal getPointAccrualRate(Long userId) {
    Membership membership = getMembership(userId);
    MembershipLevel level = membershipLevelRepository.findById(membership.getLevelId())
        .orElseThrow(() -> new RuntimeException("멤버십 등급 정보를 찾을 수 없습니다"));
    
    return level.getPointAccrualRate();
}

/**
 * 결제 금액에 멤버십 등급 적립률을 적용하여 적립 포인트 계산
 */
public Integer calculateEarnedPoints(Long userId, BigDecimal paymentAmount) {
    BigDecimal accrualRate = getPointAccrualRate(userId);
    BigDecimal earnedPoints = paymentAmount.multiply(accrualRate);
    
    // 소수점 이하 반올림 처리
    return earnedPoints.setScale(0, RoundingMode.HALF_UP).intValue();
}
```

#### 5. 결제 완료 시 멤버십 등급 업데이트 호출
```java
// PaymentService.savePaymentToDatabase 내부
@Transactional
public void savePaymentToDatabase(String paymentId, String orderId, 
                                  Integer amount, Map<String, Object> paymentDetails) {
    // ... 결제 정보 저장 ...
    
    // 포인트 적립 및 멤버십 등급 업데이트
    Long userId = order.getUserId();
    Integer pointsEarned = membershipService.calculateEarnedPoints(
        userId, BigDecimal.valueOf(amount));
    pointService.earnPoints(userId, pointsEarned, orderId, "결제 완료", null);
    
    // 멤버십 등급 자동 업데이트
    membershipService.updateMembershipLevel(userId);
}
```

#### 6. API 제공
```java
@RestController
@RequestMapping("/api")
public class MembershipController {
    
    @Autowired
    private MembershipService membershipService;
    
    /**
     * 멤버십 정보 및 등급 조회
     */
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
    
    /**
     * 사용자 결제 내역 조회
     */
    @GetMapping("/membership/user/{userId}/payments")
    public ResponseEntity<Map<String, Object>> getUserPaymentHistory(@PathVariable Long userId) {
        // 완료된 주문, 취소된 주문, 총 결제 금액 등 반환
        // ...
    }
}
```

**주요 고려사항**:
- 결제 완료 시 `membershipService.updateMembershipLevel(userId)` 자동 호출
- 결제 취소 시에도 `membershipService.updateMembershipLevel(userId)` 자동 호출하여 등급 다운그레이드 처리
- 멤버십 등급이 없을 경우 기본 등급(Normal) 자동 생성
- 포인트 적립 시 현재 멤버십 등급의 적립률 적용

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
