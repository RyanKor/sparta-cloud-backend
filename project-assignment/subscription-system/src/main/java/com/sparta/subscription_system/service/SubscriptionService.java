package com.sparta.subscription_system.service;

import com.sparta.subscription_system.client.PortOneClient;
import com.sparta.subscription_system.entity.*;
import com.sparta.subscription_system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final SubscriptionInvoiceRepository invoiceRepository;
    private final SubscriptionRefundRepository refundRepository;
    private final UserRepository userRepository;
    private final PortOneClient portOneClient;

    @Autowired
    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                              PlanRepository planRepository,
                              PaymentMethodRepository paymentMethodRepository,
                              SubscriptionInvoiceRepository invoiceRepository,
                              SubscriptionRefundRepository refundRepository,
                              UserRepository userRepository,
                              PortOneClient portOneClient) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.invoiceRepository = invoiceRepository;
        this.refundRepository = refundRepository;
        this.userRepository = userRepository;
        this.portOneClient = portOneClient;
    }

    @Transactional
    public Subscription createSubscription(Long userId, Long planId, Long paymentMethodId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found: " + planId));

        if (plan.getStatus() != Plan.PlanStatus.ACTIVE) {
            throw new RuntimeException("Plan is not active: " + planId);
        }

        PaymentMethod paymentMethod = null;
        if (paymentMethodId != null) {
            paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                    .orElseThrow(() -> new RuntimeException("Payment method not found: " + paymentMethodId));

            if (!paymentMethod.getUser().getUserId().equals(userId)) {
                throw new RuntimeException("Payment method does not belong to user: " + userId);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentPeriodStart = now;
        LocalDateTime currentPeriodEnd = calculatePeriodEnd(now, plan.getBillingInterval());
        LocalDateTime trialEnd = null;

        Subscription.SubscriptionStatus status = Subscription.SubscriptionStatus.TRIALING;
        if (plan.getTrialPeriodDays() > 0) {
            trialEnd = now.plusDays(plan.getTrialPeriodDays());
            status = Subscription.SubscriptionStatus.TRIALING;
        } else {
            status = Subscription.SubscriptionStatus.ACTIVE;
        }

        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setPlan(plan);
        subscription.setPaymentMethod(paymentMethod);
        subscription.setStatus(status);
        subscription.setCurrentPeriodStart(currentPeriodStart);
        subscription.setCurrentPeriodEnd(currentPeriodEnd);
        subscription.setTrialEnd(trialEnd);

        Subscription savedSubscription = subscriptionRepository.save(subscription);

        // 구독 생성 시 초기 결제 내역을 청구서로 기록
        // 빌링키 발급 시 결제가 완료된 경우를 고려하여 청구서 생성
        if (paymentMethod != null) {
            // 빌링키 발급 시 결제가 완료된 경우를 대비하여 초기 청구서 생성
            // 실제로는 빌링키 발급 시 이미 결제가 완료되었으므로, 여기서는 구독 생성 시점의 청구서만 생성
            // 체험 기간이 없으면 첫 결제 청구서 생성
            if (status == Subscription.SubscriptionStatus.ACTIVE && plan.getTrialPeriodDays() == 0) {
                SubscriptionInvoice initialInvoice = new SubscriptionInvoice();
                initialInvoice.setSubscription(savedSubscription);
                initialInvoice.setAmount(plan.getPrice());
                initialInvoice.setStatus(SubscriptionInvoice.InvoiceStatus.PAID);
                initialInvoice.setPaidAt(LocalDateTime.now());
                initialInvoice.setDueDate(now);
                initialInvoice.setAttemptCount(1);
                initialInvoice.setImpUid("initial_payment_" + savedSubscription.getSubscriptionId());
                invoiceRepository.save(initialInvoice);
            }
        }

        // 결제 수단이 있고 활성 상태인 경우 예약결제 스케줄 생성
        if (paymentMethod != null && (status == Subscription.SubscriptionStatus.ACTIVE || status == Subscription.SubscriptionStatus.TRIALING)) {
            createBillingSchedule(savedSubscription).subscribe(
                    result -> {
                        // 스케줄 생성 성공 (로그만 남김)
                        System.out.println("예약결제 스케줄 생성 성공: subscriptionId=" + savedSubscription.getSubscriptionId());
                    },
                    error -> {
                        // 스케줄 생성 실패 시 로그만 남김 (구독은 이미 생성됨)
                        System.err.println("예약결제 스케줄 생성 실패: " + error.getMessage());
                    }
            );
        }

        return savedSubscription;
    }

    /**
     * 빌링키 발급 시 결제 내역을 청구서로 기록
     * @param userId 사용자 ID
     * @param planId 플랜 ID (선택적)
     * @param amount 결제 금액
     * @param impUid 포트원 결제 ID
     * @param paymentId 결제 ID
     * @return 생성된 청구서 (구독이 있으면 연결, 없으면 null)
     */
    @Transactional
    public SubscriptionInvoice createBillingKeyPaymentInvoice(Long userId, Long planId, BigDecimal amount, String impUid, String paymentId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // 해당 사용자의 활성 구독 찾기 (플랜 ID가 있으면 해당 플랜의 구독)
        Subscription subscription = null;
        List<Subscription> userSubscriptions = subscriptionRepository.findByUserUserId(userId);
        
        if (planId != null) {
            subscription = userSubscriptions.stream()
                    .filter(sub -> sub.getPlan().getPlanId().equals(planId) 
                            && (sub.getStatus() == Subscription.SubscriptionStatus.ACTIVE 
                                || sub.getStatus() == Subscription.SubscriptionStatus.TRIALING))
                    .findFirst()
                    .orElse(null);
        } else {
            // 플랜 ID가 없으면 가장 최근 활성 구독 사용
            subscription = userSubscriptions.stream()
                    .filter(sub -> sub.getStatus() == Subscription.SubscriptionStatus.ACTIVE 
                            || sub.getStatus() == Subscription.SubscriptionStatus.TRIALING)
                    .findFirst()
                    .orElse(null);
        }

        // 구독이 없으면 청구서를 생성하지 않음 (나중에 구독 생성 시 연결)
        if (subscription == null) {
            return null;
        }

        SubscriptionInvoice invoice = new SubscriptionInvoice();
        invoice.setSubscription(subscription);
        invoice.setAmount(amount);
        invoice.setStatus(SubscriptionInvoice.InvoiceStatus.PAID);
        invoice.setImpUid(impUid);
        invoice.setPaidAt(LocalDateTime.now());
        invoice.setDueDate(LocalDateTime.now());
        invoice.setAttemptCount(1);

        return invoiceRepository.save(invoice);
    }

    /**
     * 등록된 결제 수단으로 결제한 내역을 청구서로 기록 (구독 없이도 가능)
     * @param userId 사용자 ID
     * @param amount 결제 금액
     * @param impUid 포트원 결제 ID
     * @param merchantUid 주문 ID
     * @param orderName 주문명
     * @return 생성된 청구서 (구독이 있으면 연결, 없으면 null)
     */
    @Transactional
    public SubscriptionInvoice createPaymentInvoice(Long userId, BigDecimal amount, String impUid, String merchantUid, String orderName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // 해당 사용자의 활성 구독 찾기 (가장 최근 활성 구독 사용)
        List<Subscription> userSubscriptions = subscriptionRepository.findByUserUserId(userId);
        Subscription subscription = userSubscriptions.stream()
                .filter(sub -> sub.getStatus() == Subscription.SubscriptionStatus.ACTIVE 
                        || sub.getStatus() == Subscription.SubscriptionStatus.TRIALING)
                .findFirst()
                .orElse(null);

        // 구독이 없으면 청구서를 생성하지 않음 (구독이 없어도 결제는 가능하지만 청구서는 구독이 있을 때만 생성)
        if (subscription == null) {
            return null;
        }

        SubscriptionInvoice invoice = new SubscriptionInvoice();
        invoice.setSubscription(subscription);
        invoice.setAmount(amount);
        invoice.setStatus(SubscriptionInvoice.InvoiceStatus.PAID);
        invoice.setImpUid(impUid);
        invoice.setPaidAt(LocalDateTime.now());
        invoice.setDueDate(LocalDateTime.now());
        invoice.setAttemptCount(1);

        return invoiceRepository.save(invoice);
    }

    private LocalDateTime calculatePeriodEnd(LocalDateTime start, String billingInterval) {
        if ("yearly".equalsIgnoreCase(billingInterval) || "annual".equalsIgnoreCase(billingInterval)) {
            return start.plusYears(1);
        } else {
            return start.plusMonths(1); // default to monthly
        }
    }

    public List<Subscription> getSubscriptionsByUserId(Long userId) {
        return subscriptionRepository.findByUserUserId(userId);
    }

    public Optional<Subscription> getSubscriptionById(Long subscriptionId) {
        return subscriptionRepository.findById(subscriptionId);
    }

    @Transactional
    public Subscription cancelSubscription(Long userId, Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));

        if (!subscription.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Subscription does not belong to user: " + userId);
        }

        if (subscription.getStatus() == Subscription.SubscriptionStatus.CANCELED ||
            subscription.getStatus() == Subscription.SubscriptionStatus.ENDED) {
            throw new RuntimeException("Subscription is already canceled or ended");
        }

        // PortOne API를 통해 예약결제 스케줄 삭제
        deleteBillingSchedule(subscription).subscribe(
                result -> {
                    System.out.println("예약결제 스케줄 삭제 성공: subscriptionId=" + subscriptionId);
                },
                error -> {
                    System.err.println("예약결제 스케줄 삭제 실패: " + error.getMessage());
                    // 스케줄 삭제 실패해도 구독 취소는 진행
                }
        );

        // 구독 상태를 취소로 변경
        subscription.setStatus(Subscription.SubscriptionStatus.CANCELED);
        subscription.setCanceledAt(LocalDateTime.now());
        Subscription savedSubscription = subscriptionRepository.save(subscription);

        // 구독 취소 시 관련된 모든 청구서의 상태를 CANCELED로 변경
        List<SubscriptionInvoice> invoices = invoiceRepository.findBySubscriptionSubscriptionId(subscriptionId);
        for (SubscriptionInvoice invoice : invoices) {
            // 모든 청구서를 CANCELED 상태로 변경 (결제 완료된 청구서도 포함)
            invoice.setStatus(SubscriptionInvoice.InvoiceStatus.CANCELED);
            invoice.setErrorMessage("구독 취소");
            invoiceRepository.save(invoice);
        }

        return savedSubscription;
    }

    @Transactional
    public SubscriptionInvoice createInvoice(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found: " + subscriptionId));

        if (subscription.getStatus() != Subscription.SubscriptionStatus.ACTIVE &&
            subscription.getStatus() != Subscription.SubscriptionStatus.PAST_DUE) {
            throw new RuntimeException("Cannot create invoice for subscription with status: " + subscription.getStatus());
        }

        SubscriptionInvoice invoice = new SubscriptionInvoice();
        invoice.setSubscription(subscription);
        invoice.setAmount(subscription.getPlan().getPrice());
        invoice.setStatus(SubscriptionInvoice.InvoiceStatus.PENDING);
        invoice.setDueDate(subscription.getCurrentPeriodEnd());
        invoice.setAttemptCount(0);

        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Mono<Boolean> processInvoicePayment(Long invoiceId) {
        SubscriptionInvoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));

        if (invoice.getStatus() != SubscriptionInvoice.InvoiceStatus.PENDING) {
            return Mono.just(false);
        }

        Subscription subscription = invoice.getSubscription();
        PaymentMethod paymentMethod = subscription.getPaymentMethod();

        if (paymentMethod == null) {
            invoice.setStatus(SubscriptionInvoice.InvoiceStatus.FAILED);
            invoice.setErrorMessage("No payment method associated with subscription");
            invoiceRepository.save(invoice);
            return Mono.just(false);
        }

        // PortOne 정기결제 실행
        return portOneClient.getAccessToken()
                .flatMap(accessToken -> {
                    Map<String, Object> billingRequest = Map.of(
                            "amount", invoice.getAmount().intValue(),
                            "merchantUid", "invoice_" + invoiceId,
                            "name", subscription.getPlan().getName() + " 구독료"
                    );

                    return portOneClient.executeBilling(paymentMethod.getCustomerUid(), billingRequest, accessToken)
                            .map(billingResult -> {
                                String impUid = (String) billingResult.get("imp_uid");
                                if (impUid != null) {
                                    invoice.setStatus(SubscriptionInvoice.InvoiceStatus.PAID);
                                    invoice.setImpUid(impUid);
                                    invoice.setPaidAt(LocalDateTime.now());
                                    invoice.setAttemptCount(invoice.getAttemptCount() + 1);

                                    // 구독 기간 연장
                                    LocalDateTime newPeriodStart = subscription.getCurrentPeriodEnd();
                                    LocalDateTime newPeriodEnd = calculatePeriodEnd(newPeriodStart, subscription.getPlan().getBillingInterval());
                                    subscription.setCurrentPeriodStart(newPeriodStart);
                                    subscription.setCurrentPeriodEnd(newPeriodEnd);
                                    subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);

                                    invoiceRepository.save(invoice);
                                    subscriptionRepository.save(subscription);

                                    return true;
                                } else {
                                    invoice.setStatus(SubscriptionInvoice.InvoiceStatus.FAILED);
                                    invoice.setAttemptCount(invoice.getAttemptCount() + 1);
                                    invoice.setErrorMessage("Payment failed: No imp_uid returned");
                                    invoiceRepository.save(invoice);
                                    return false;
                                }
                            })
                            .onErrorResume(error -> {
                                invoice.setStatus(SubscriptionInvoice.InvoiceStatus.FAILED);
                                invoice.setAttemptCount(invoice.getAttemptCount() + 1);
                                invoice.setErrorMessage("Payment failed: " + error.getMessage());
                                invoiceRepository.save(invoice);

                                // 결제 실패 시 구독 상태를 PAST_DUE로 변경
                                subscription.setStatus(Subscription.SubscriptionStatus.PAST_DUE);
                                subscriptionRepository.save(subscription);

                                return Mono.just(false);
                            });
                })
                .onErrorReturn(false);
    }

    public List<SubscriptionInvoice> getInvoicesBySubscriptionId(Long subscriptionId) {
        return invoiceRepository.findBySubscriptionSubscriptionId(subscriptionId);
    }

    public List<SubscriptionInvoice> getInvoicesByUserId(Long userId) {
        return invoiceRepository.findBySubscriptionUserUserId(userId);
    }

    @Transactional
    public Mono<Boolean> refundInvoice(Long invoiceId, BigDecimal amount, String reason) {
        SubscriptionInvoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));

        if (invoice.getStatus() != SubscriptionInvoice.InvoiceStatus.PAID) {
            throw new RuntimeException("Only paid invoices can be refunded");
        }

        if (invoice.getImpUid() == null) {
            throw new RuntimeException("Invoice does not have imp_uid");
        }

        return portOneClient.getAccessToken()
                .flatMap(accessToken -> portOneClient.cancelPayment(invoice.getImpUid(), accessToken, reason))
                .map(cancelResult -> {
                    SubscriptionRefund refund = new SubscriptionRefund();
                    refund.setInvoice(invoice);
                    refund.setAmount(amount);
                    refund.setReason(reason);
                    refund.setStatus(SubscriptionRefund.RefundStatus.COMPLETED);
                    refundRepository.save(refund);

                    invoice.setStatus(SubscriptionInvoice.InvoiceStatus.REFUNDED);
                    invoiceRepository.save(invoice);

                    return true;
                })
                .onErrorReturn(false);
    }

    /**
     * 예약결제 스케줄 생성
     * 구독의 billingInterval에 따라 반복 결제 스케줄을 생성합니다.
     */
    private Mono<Map> createBillingSchedule(Subscription subscription) {
        PaymentMethod paymentMethod = subscription.getPaymentMethod();
        if (paymentMethod == null || paymentMethod.getCustomerUid() == null) {
            return Mono.error(new RuntimeException("Payment method or customerUid is missing"));
        }

        Plan plan = subscription.getPlan();
        String billingInterval = plan.getBillingInterval();
        
        // billingInterval에 따라 스케줄 간격 설정
        // "monthly" -> 매월, "yearly" -> 매년
        String scheduleInterval = "monthly".equalsIgnoreCase(billingInterval) ? "month" : "year";
        
        // 첫 결제 예정일 설정 (체험 기간이 있으면 체험 종료일, 없으면 다음 기간 시작일)
        LocalDateTime scheduledAt = subscription.getTrialEnd() != null 
                ? subscription.getTrialEnd() 
                : subscription.getCurrentPeriodEnd();

        // 저장된 billingKey가 있으면 직접 사용, 없으면 customerUid로 조회
        String billingKey = paymentMethod.getBillingKey();
        
        if (billingKey != null && !billingKey.trim().isEmpty()) {
            // 저장된 billingKey를 직접 사용
            return portOneClient.getAccessToken()
                    .flatMap(accessToken -> {
                        // PortOne V2 API 형식에 맞춰 스케줄 생성
                        // 참고: https://developers.portone.io/opi/ko/integration/start/v2/billing/schedule?v=v2
                        
                        // 고유한 payment_id 생성
                        String paymentId = "schedule_" + subscription.getSubscriptionId() + "_" + System.currentTimeMillis();
                        
                        // ISO 8601 형식으로 변환 (예: 2023-08-24T14:15:22Z)
                        String timeToPay = scheduledAt.toString().replace(" ", "T") + "Z";
                        
                        // payment 객체 생성
                        Map<String, Object> payment = new java.util.HashMap<>();
                        payment.put("billingKey", billingKey);
                        payment.put("orderName", plan.getName() + " 구독료");
                        
                        Map<String, Object> customer = new java.util.HashMap<>();
                        customer.put("id", String.valueOf(subscription.getUser().getUserId()));
                        payment.put("customer", customer);
                        
                        Map<String, Object> amount = new java.util.HashMap<>();
                        amount.put("total", plan.getPrice().intValue());
                        payment.put("amount", amount);
                        payment.put("currency", "KRW");
                        
                        // scheduleRequest 생성
                        Map<String, Object> scheduleRequest = new java.util.HashMap<>();
                        scheduleRequest.put("payment", payment);
                        scheduleRequest.put("timeToPay", timeToPay);

                        return portOneClient.createSchedule(paymentId, scheduleRequest, accessToken);
                    })
                    .onErrorMap(error -> new RuntimeException("예약결제 스케줄 생성 실패: " + error.getMessage(), error));
        } else {
            // billingKey가 없으면 customerUid로 조회 (기존 방식, 재시도 로직 포함)
            return portOneClient.getAccessToken()
                    .flatMap(accessToken -> {
                        // 먼저 빌링키가 존재하는지 확인 (재시도 로직 포함)
                        // 빌링키 발급 후 PortOne에 등록되는 데 시간이 걸릴 수 있으므로 재시도
                        return portOneClient.getBillingKey(paymentMethod.getCustomerUid(), accessToken)
                                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2))
                                        .filter(error -> {
                                            // 404 에러인 경우에만 재시도
                                            String errorMessage = error.getMessage();
                                            return errorMessage != null && 
                                                   (errorMessage.contains("404") || 
                                                    errorMessage.contains("not found") ||
                                                    errorMessage.contains("등록되어 있지 않습니다"));
                                        })
                                        .doBeforeRetry(retrySignal -> 
                                            System.out.println("빌링키 조회 재시도 중... customerUid: " + 
                                                paymentMethod.getCustomerUid() + 
                                                " (시도: " + retrySignal.totalRetries() + "/3)")))
                                .onErrorResume(error -> {
                                    // 재시도 후에도 실패하면 에러 반환
                                    String errorMessage = error.getMessage();
                                    if (errorMessage != null && (errorMessage.contains("404") || errorMessage.contains("not found"))) {
                                        return Mono.error(new RuntimeException(
                                                "빌링키가 PortOne에 등록되어 있지 않습니다. customerUid: " + 
                                                paymentMethod.getCustomerUid() + 
                                                ". 빌링키를 먼저 발급해주세요."));
                                    }
                                    return Mono.error(error);
                                })
                                .flatMap(billingKeyInfo -> {
                                    // PortOne V2 API 형식에 맞춰 스케줄 생성
                                    // 참고: https://developers.portone.io/opi/ko/integration/start/v2/billing/schedule?v=v2
                                    
                                    // 고유한 payment_id 생성
                                    String paymentId = "schedule_" + subscription.getSubscriptionId() + "_" + System.currentTimeMillis();
                                    
                                    // ISO 8601 형식으로 변환 (예: 2023-08-24T14:15:22Z)
                                    String timeToPay = scheduledAt.toString().replace(" ", "T") + "Z";
                                    
                                    // payment 객체 생성
                                    Map<String, Object> payment = new java.util.HashMap<>();
                                    
                                    // billingKeyInfo에서 billingKey 추출
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> billingKeyInfoMap = (Map<String, Object>) billingKeyInfo.get("billingKeyInfo");
                                    String extractedBillingKey = null;
                                    if (billingKeyInfoMap != null && billingKeyInfoMap.containsKey("billingKey")) {
                                        extractedBillingKey = (String) billingKeyInfoMap.get("billingKey");
                                    } else if (billingKeyInfo.containsKey("billingKey")) {
                                        extractedBillingKey = (String) billingKeyInfo.get("billingKey");
                                    }
                                    
                                    if (extractedBillingKey == null) {
                                        return Mono.error(new RuntimeException("빌링키 정보를 찾을 수 없습니다."));
                                    }
                                    
                                    payment.put("billingKey", extractedBillingKey);
                                    payment.put("orderName", plan.getName() + " 구독료");
                                    
                                    Map<String, Object> customer = new java.util.HashMap<>();
                                    customer.put("id", String.valueOf(subscription.getUser().getUserId()));
                                    payment.put("customer", customer);
                                    
                                    Map<String, Object> amount = new java.util.HashMap<>();
                                    amount.put("total", plan.getPrice().intValue());
                                    payment.put("amount", amount);
                                    payment.put("currency", "KRW");
                                    
                                    // scheduleRequest 생성
                                    Map<String, Object> scheduleRequest = new java.util.HashMap<>();
                                    scheduleRequest.put("payment", payment);
                                    scheduleRequest.put("timeToPay", timeToPay);

                                    return portOneClient.createSchedule(paymentId, scheduleRequest, accessToken);
                                });
                    })
                    .onErrorMap(error -> new RuntimeException("예약결제 스케줄 생성 실패: " + error.getMessage(), error));
        }
    }

    /**
     * 예약결제 스케줄 삭제
     */
    public Mono<Boolean> deleteBillingSchedule(Subscription subscription) {
        PaymentMethod paymentMethod = subscription.getPaymentMethod();
        if (paymentMethod == null || paymentMethod.getCustomerUid() == null) {
            return Mono.just(false);
        }

        return portOneClient.getAccessToken()
                .flatMap(accessToken -> 
                    portOneClient.getSchedules(paymentMethod.getCustomerUid(), accessToken)
                        .flatMap(schedules -> {
                            // 스케줄 목록에서 해당 구독의 스케줄을 찾아 삭제
                            // PortOne API 응답 구조: { "schedules": [{ "id": "...", "metadata": {...} }] }
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> scheduleList = (List<Map<String, Object>>) schedules.get("schedules");
                            
                            if (scheduleList == null || scheduleList.isEmpty()) {
                                return Mono.just(true); // 스케줄이 없으면 성공으로 처리
                            }

                            // 해당 구독의 스케줄 찾기 (metadata에 subscriptionId가 있는 경우)
                            List<Mono<Map>> deleteOperations = new java.util.ArrayList<>();
                            
                            for (Map<String, Object> schedule : scheduleList) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> metadata = (Map<String, Object>) schedule.get("metadata");
                                
                                if (metadata != null) {
                                    Object subId = metadata.get("subscriptionId");
                                    if (subId != null && subId.equals(subscription.getSubscriptionId())) {
                                        String scheduleId = (String) schedule.get("id");
                                        if (scheduleId != null) {
                                            deleteOperations.add(
                                                portOneClient.deleteSchedule(paymentMethod.getCustomerUid(), scheduleId, accessToken)
                                            );
                                        }
                                    }
                                } else {
                                    // metadata가 없으면 모든 스케줄 삭제 (안전하지 않을 수 있음)
                                    String scheduleId = (String) schedule.get("id");
                                    if (scheduleId != null) {
                                        deleteOperations.add(
                                            portOneClient.deleteSchedule(paymentMethod.getCustomerUid(), scheduleId, accessToken)
                                        );
                                    }
                                }
                            }

                            if (deleteOperations.isEmpty()) {
                                return Mono.just(true);
                            }

                            // 모든 삭제 작업을 병렬로 실행
                            return Mono.when(deleteOperations)
                                    .then(Mono.just(true));
                        })
                )
                .onErrorReturn(false);
    }
}


