package com.sparta.payment_system.service;

import com.sparta.payment_system.client.PortOneClient;
import com.sparta.payment_system.entity.Payment;
import com.sparta.payment_system.entity.Refund;
import com.sparta.payment_system.entity.Order;
import com.sparta.payment_system.repository.PaymentRepository;
import com.sparta.payment_system.repository.RefundRepository;
import com.sparta.payment_system.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Service
public class PaymentService {

    private final PortOneClient portoneClient;
    private final PaymentRepository paymentRepository;
	private final RefundRepository refundRepository;
    private final OrderRepository orderRepository;

    @Autowired
	public PaymentService(PortOneClient portoneClient, PaymentRepository paymentRepository, RefundRepository refundRepository, OrderRepository orderRepository) {
		this.portoneClient = portoneClient;
		this.paymentRepository = paymentRepository;
		this.refundRepository = refundRepository;
		this.orderRepository = orderRepository;
	}

    public Mono<Boolean> verifyPayment(String paymentId) {
        return portoneClient.getAccessToken()
                .flatMap(accessToken -> portoneClient.getPaymentDetails(paymentId, accessToken))
                .map(paymentDetails -> {
                    System.out.println("결제 정보 조회 결과: " + paymentDetails);
                    
                    // 1. 포트원에서 조회한 결제 상태가 결제 완료인지 확인 (대소문자/표기 변형 허용)
                    String status = (String) paymentDetails.get("status");
                    if (status == null || !("PAID".equalsIgnoreCase(status) || "Paid".equalsIgnoreCase(status))) {
                        System.out.println("결제 상태 오류: " + status);
                        return false;
                    }

                    // 2. 결제 금액 정보 추출
                    Map<String, Object> amountInfo = (Map<String, Object>) paymentDetails.get("amount");
                    Integer paidAmount = 0;
                    if (amountInfo != null) {
                        Object totalObj = amountInfo.get("total");
                        if (totalObj instanceof Number) {
                            paidAmount = ((Number) totalObj).intValue();
                        } else if (totalObj instanceof String) {
                            try {
                                paidAmount = Integer.parseInt((String) totalObj);
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                    
                    // 3. 주문 정보 추출
                    String orderName = (String) paymentDetails.get("orderName");
                    String resolvedOrderId = resolveOrderId(paymentDetails, paymentId);
                    
                    System.out.println("결제 검증 성공!");
                    System.out.println("결제 ID: " + paymentId);
                    System.out.println("주문명: " + orderName);
                    System.out.println("주문 ID: " + resolvedOrderId);
                    System.out.println("결제 금액: " + paidAmount);
                    
                    // 4. DB에 결제 정보 저장
                    System.out.println("=== 결제 정보 DB 저장 시작 ===");
                    System.out.println("Payment ID: " + paymentId);
                    System.out.println("Resolved Order ID: " + resolvedOrderId);
                    System.out.println("Paid Amount: " + paidAmount);
                    
                    try {
                        savePaymentToDatabase(paymentId, resolvedOrderId, paidAmount, paymentDetails);
                        System.out.println("=== 결제 정보 DB 저장 완료 ===");
                    } catch (Exception e) {
                        System.err.println("결제 정보 DB 저장 중 오류 발생: " + e.getMessage());
                        e.printStackTrace();
                        // DB 저장 실패해도 결제 검증은 성공으로 처리
                    }
                    
                    return true;
                })
                .onErrorReturn(false);
    }

    /**
     * 결제 상세 응답에서 주문 ID를 최대한 안정적으로 추출한다.
     * 우선순위: merchantUid → merchantPaymentId → orderId → customData.orderId → payment.id (fallback)
     */
    private String resolveOrderId(Map<String, Object> paymentDetails, String paymentId) {
        // 1) 흔히 쓰는 키들 시도
        String[] candidateKeys = new String[]{"merchantUid", "merchantPaymentId", "orderId"};
        for (String key : candidateKeys) {
            Object value = paymentDetails.get(key);
            if (value instanceof String && !((String) value).isBlank()) {
                return (String) value;
            }
        }

        // 2) customData에서 orderId 추출 (문자열 또는 맵 형태 모두 지원)
        Object customDataObj = paymentDetails.get("customData");
        if (customDataObj != null) {
            try {
                if (customDataObj instanceof Map) {
                    Object orderIdInMap = ((Map<?, ?>) customDataObj).get("orderId");
                    if (orderIdInMap instanceof String && !((String) orderIdInMap).isBlank()) {
                        return (String) orderIdInMap;
                    }
                } else if (customDataObj instanceof String) {
                    String customDataStr = (String) customDataObj;
                    // 단순 문자열 또는 JSON 문자열 모두 처리 시도
                    if (customDataStr.contains("orderId")) {
                        // 매우 가벼운 파싱 (중괄호 JSON 가정)
                        int idx = customDataStr.indexOf("orderId");
                        int colon = customDataStr.indexOf(":", idx);
                        if (colon > -1) {
                            int startQuote = customDataStr.indexOf('"', colon);
                            int endQuote = customDataStr.indexOf('"', startQuote + 1);
                            if (startQuote > -1 && endQuote > startQuote) {
                                String extracted = customDataStr.substring(startQuote + 1, endQuote);
                                if (!extracted.isBlank()) return extracted;
                            }
                        }
                    } else if (!customDataStr.isBlank()) {
                        // customData가 orderId 그 자체일 수도 있음
                        return customDataStr;
                    }
                }
            } catch (Exception ignored) {
                // 파싱 실패 시 무시하고 다음 단계 진행
            }
        }

        // 3) 최후의 수단: payment.id를 사용 (DB 제약 위반 방지용)
        Object id = paymentDetails.get("id");
        if (id instanceof String && !((String) id).isBlank()) {
            System.out.println("[경고] 주문 ID를 찾을 수 없어 payment.id로 대체합니다: " + id);
            return (String) id;
        }
        System.out.println("[경고] 주문 ID를 찾을 수 없어 결제 ID로 대체합니다.");
        return paymentId; // "unknown-order" 대신 실제 paymentId 사용
    }

    private void savePaymentToDatabase(String paymentId, String orderId, Integer amount, Map<String, Object> paymentDetails) {
        try {
            System.out.println("savePaymentToDatabase 메서드 호출됨");
            System.out.println("입력 파라미터 - paymentId: " + paymentId + ", orderId: " + orderId + ", amount: " + amount);
            
            // 주문이 존재하는지 먼저 확인
            Optional<Order> orderOptional = orderRepository.findByOrderId(orderId);
            if (orderOptional.isEmpty()) {
                System.out.println("주문을 찾을 수 없습니다. 자동으로 주문을 생성합니다. Order ID: " + orderId);
                
                // 주문이 없으면 자동으로 생성
                Order newOrder = createOrderFromPaymentDetails(orderId, amount, paymentDetails);
                if (newOrder != null) {
                    orderRepository.save(newOrder);
                    System.out.println("새 주문이 생성되었습니다. Order ID: " + orderId);
                } else {
                    System.err.println("주문 생성에 실패했습니다. Order ID: " + orderId);
                    return;
                }
            }
            
            // 기존 결제가 있는지 확인 (imp_uid로 먼저, 없으면 order_id로)
            Optional<Payment> existingPayment = paymentRepository.findByImpUid(paymentId);
            if (existingPayment.isEmpty()) {
                existingPayment = paymentRepository.findByOrderId(orderId);
            }
            
            Payment payment;
            if (existingPayment.isPresent()) {
                // 기존 결제 업데이트
                payment = existingPayment.get();
                System.out.println("기존 결제 정보를 업데이트합니다. Payment ID: " + payment.getPaymentId());
            } else {
                // 새 결제 생성
                payment = new Payment();
                System.out.println("새 결제 정보를 생성합니다.");
            }
            
            payment.setOrderId(orderId);
            payment.setImpUid(paymentId);
            payment.setAmount(java.math.BigDecimal.valueOf(amount));
            payment.setStatus(Payment.PaymentStatus.PAID);
            
            // 결제 수단 설정 (키가 환경에 따라 다를 수 있음)
            Object payMethod = paymentDetails.get("payMethod");
            if (payMethod == null) {
                payMethod = paymentDetails.get("method");
            }
            if (payMethod instanceof String) {
                payment.setPaymentMethod((String) payMethod);
            }
            
            // 결제 완료 시간 설정
            try {
                Object paidAtObj = paymentDetails.get("paidAt");
                if (paidAtObj instanceof String) {
                    String paidAt = (String) paidAtObj;
                    // ISO-8601 형식 처리 (Z 포함 가능)
                    java.time.Instant instant = java.time.Instant.parse(paidAt);
                    payment.setPaidAt(java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault()));
                }
            } catch (Exception ignored) {
                // 시간 파싱 실패 시 저장 생략
            }
            
            paymentRepository.save(payment);
            System.out.println("결제 정보가 데이터베이스에 저장되었습니다.");
            
            // 결제 완료 후 주문 상태를 COMPLETED로 업데이트
            updateOrderStatusToCompleted(orderId);
            
        } catch (Exception e) {
            System.err.println("결제 정보 저장 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Mono<Boolean> cancelPayment(String paymentId, String reason) {
        return portoneClient.getAccessToken()
                .flatMap(accessToken ->
                        // 1) 먼저 결제 상세 조회로 PortOne의 공식 결제 ID를 확인한다
                        portoneClient.getPaymentDetails(paymentId, accessToken)
                                .flatMap(paymentDetails -> {
                                    Object officialIdObj = paymentDetails.get("id");
                                    String idToCancel = (officialIdObj instanceof String && !((String) officialIdObj).isBlank())
                                            ? (String) officialIdObj
                                            : paymentId;

                                    System.out.println("취소 대상 식별자 확인 - 제공된 ID: " + paymentId + ", PortOne 공식 ID: " + idToCancel);

                                    // 2) 확인한 ID로 취소 요청 수행
									return portoneClient.cancelPayment(idToCancel, accessToken, reason)
											.map(cancelResult -> {
												System.out.println("결제 취소 결과: " + cancelResult);
												// 취소 성공 시 DB 상태 갱신 및 환불 레코드 생성
												try {
													updateDatabaseAfterCancel(paymentDetails, idToCancel, reason, cancelResult);
												} catch (Exception e) {
													System.err.println("취소 후 DB 업데이트 중 오류: " + e.getMessage());
													e.printStackTrace();
												}
												return true;
											});
                                })
                                // 상세 조회가 실패하면(예: 잘못된 ID 유형) 마지막 수단으로 전달된 ID로 바로 취소 시도
                                .onErrorResume(detailError -> {
                                    System.err.println("결제 상세 조회 실패로 직접 취소 시도: " + detailError.getMessage());
                                    return portoneClient.cancelPayment(paymentId, accessToken, reason)
											.map(cancelResult -> {
												System.out.println("결제 취소 결과(직접 시도): " + cancelResult);
												// 상세 조회 실패 케이스에서는 paymentDetails가 없어 보수적 처리만 수행
												try {
													updateDatabaseAfterCancel(null, paymentId, reason, cancelResult);
												} catch (Exception e) {
													System.err.println("취소 후 DB 업데이트(직접 시도) 중 오류: " + e.getMessage());
													e.printStackTrace();
												}
												return true;
											});
                                })
                )
                .doOnError(e -> System.err.println("결제 취소 중 오류: " + e.getMessage()))
                .onErrorReturn(false);
    }

	private void updateDatabaseAfterCancel(Map<String, Object> paymentDetails,
				String idToCancel,
				String reason,
				Map<String, Object> cancelResult) {
		// 결제 식별을 위해 우선 impUid(PortOne id)로 조회, 실패 시 orderId로 보조 조회
		Optional<Payment> paymentOptional = paymentRepository.findByImpUid(idToCancel);
		if (paymentOptional.isEmpty() && paymentDetails != null) {
			String resolvedOrderId = resolveOrderId(paymentDetails, idToCancel);
			if (resolvedOrderId != null && !resolvedOrderId.isBlank()) {
				paymentOptional = paymentRepository.findByOrderId(resolvedOrderId);
			}
		}

		if (paymentOptional.isEmpty()) {
			System.err.println("[경고] 취소 후 DB 업데이트 실패: 결제 레코드를 찾을 수 없습니다. 기준값=" + idToCancel);
			return;
		}

		Payment payment = paymentOptional.get();

		BigDecimal refundAmount = extractRefundAmount(cancelResult);
		if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
			// 환불 금액 정보를 못 찾으면 전체 금액 환불로 가정
			refundAmount = payment.getAmount();
		}

		Payment.PaymentStatus newStatus = refundAmount.compareTo(payment.getAmount()) >= 0
				? Payment.PaymentStatus.REFUNDED
				: Payment.PaymentStatus.PARTIALLY_REFUNDED;
		payment.setStatus(newStatus);
		paymentRepository.save(payment);

		Refund refund = new Refund();
		refund.setPaymentId(payment.getPaymentId());
		refund.setAmount(refundAmount);
		refund.setReason(reason);
		refund.setStatus(Refund.RefundStatus.COMPLETED);
		refundRepository.save(refund);

		System.out.println("취소 후 DB 업데이트 완료 - paymentId=" + payment.getPaymentId() + 
				", status=" + newStatus + ", refundAmount=" + refundAmount);
	}

	private BigDecimal extractRefundAmount(Map<String, Object> cancelResult) {
		if (cancelResult == null) return null;
		try {
			// 우선 'canceledAmount' 또는 'cancelAmount' 키 확인
			Object canceledAmount = cancelResult.get("canceledAmount");
			if (canceledAmount == null) {
				canceledAmount = cancelResult.get("cancelAmount");
			}
			if (canceledAmount instanceof Number) {
				return BigDecimal.valueOf(((Number) canceledAmount).doubleValue());
			}
			if (canceledAmount instanceof String && !((String) canceledAmount).isBlank()) {
				return new BigDecimal((String) canceledAmount);
			}

			// amount 객체 내부에서 취소 금액 유추: amount.cancelled 또는 amount.canceled
			Object amountObj = cancelResult.get("amount");
			if (amountObj instanceof Map<?, ?> amountMap) {
				Object cancelled = amountMap.get("cancelled");
				if (cancelled == null) {
					cancelled = amountMap.get("canceled");
				}
				if (cancelled instanceof Number) {
					return BigDecimal.valueOf(((Number) cancelled).doubleValue());
				}
				if (cancelled instanceof String && !((String) cancelled).isBlank()) {
					return new BigDecimal((String) cancelled);
				}
			}
		} catch (Exception ignored) {
		}
		return null;
	}

    /**
     * 결제 상세 정보에서 주문을 자동 생성
     */
    private Order createOrderFromPaymentDetails(String orderId, Integer amount, Map<String, Object> paymentDetails) {
        try {
            Order order = new Order();
            order.setOrderId(orderId);
            order.setTotalAmount(java.math.BigDecimal.valueOf(amount));
            order.setStatus(Order.OrderStatus.PENDING_PAYMENT);
            
            // 고객 정보에서 user_id 추출 시도
            Object customerObj = paymentDetails.get("customer");
            if (customerObj instanceof Map) {
                Map<String, Object> customer = (Map<String, Object>) customerObj;
                Object customerId = customer.get("id");
                if (customerId instanceof Number) {
                    order.setUserId(((Number) customerId).longValue());
                } else if (customerId instanceof String) {
                    try {
                        order.setUserId(Long.parseLong((String) customerId));
                    } catch (NumberFormatException ignored) {
                        order.setUserId(1L); // 기본값
                    }
                } else {
                    order.setUserId(1L); // 기본값
                }
            } else {
                order.setUserId(1L); // 기본값
            }
            
            System.out.println("자동 생성된 주문 정보:");
            System.out.println("- Order ID: " + order.getOrderId());
            System.out.println("- User ID: " + order.getUserId());
            System.out.println("- Total Amount: " + order.getTotalAmount());
            System.out.println("- Status: " + order.getStatus());
            
            return order;
        } catch (Exception e) {
            System.err.println("주문 자동 생성 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 결제 완료 후 주문 상태를 COMPLETED로 업데이트
     */
    private void updateOrderStatusToCompleted(String orderId) {
        try {
            Optional<Order> orderOptional = orderRepository.findByOrderId(orderId);
            if (orderOptional.isPresent()) {
                Order order = orderOptional.get();
                order.setStatus(Order.OrderStatus.COMPLETED);
                orderRepository.save(order);
                System.out.println("주문 상태가 COMPLETED로 업데이트되었습니다. Order ID: " + orderId);
            } else {
                System.err.println("주문을 찾을 수 없습니다. Order ID: " + orderId);
            }
        } catch (Exception e) {
            System.err.println("주문 상태 업데이트 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
