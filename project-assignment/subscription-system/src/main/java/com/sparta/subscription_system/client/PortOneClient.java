package com.sparta.subscription_system.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class PortOneClient {

    private final WebClient webClient;
    private final String apiSecret;

    public PortOneClient(@Value("${portone.api.url}") String apiUrl,
                         @Value("${portone.api.secret}") String apiSecret) {
        this.webClient = WebClient.create(apiUrl);
        this.apiSecret = apiSecret;
    }

    // API Secret으로 인증 토큰 요청
    public Mono<String> getAccessToken() {
        return webClient.post()
                .uri("/login/api-secret")
                .bodyValue(Map.of("apiSecret", apiSecret))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (String) response.get("accessToken"));
    }

    // 결제 ID로 결제 정보 조회
    public Mono<Map> getPaymentDetails(String paymentId, String accessToken) {
        return webClient.get()
                .uri("/payments/{paymentId}", paymentId)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class);
    }

    // 정기결제 빌링키 발급 (PortOne V2 API)
    // 참고: https://developers.portone.io/opi/ko/integration/start/v2/billing/issue?v=v2
    // POST /billing-keys 엔드포인트 사용 (customerUid는 body에 포함)
    public Mono<Map> issueBillingKey(Map<String, Object> billingKeyRequest, String accessToken) {
        return webClient.post()
                .uri("/billing-keys")
                .header("Authorization", "PortOne " + apiSecret)
                .header("Content-Type", "application/json")
                .bodyValue(billingKeyRequest)
                .retrieve()
                .bodyToMono(Map.class);
    }

    // 정기결제 실행 (빌링키로 결제)
    public Mono<Map> executeBilling(String customerUid, Map<String, Object> billingRequest, String accessToken) {
        return webClient.post()
                .uri("/billing-keys/{customerUid}/payments", customerUid)
                .header("Authorization", "Bearer " + accessToken)
                .bodyValue(billingRequest)
                .retrieve()
                .bodyToMono(Map.class);
    }

    // 결제 취소
    public Mono<Map> cancelPayment(String paymentId, String accessToken, String reason) {
        return webClient.post()
                .uri("/payments/{paymentId}/cancel", paymentId)
                .header("Authorization", "Bearer " + accessToken)
                .bodyValue(Map.of("reason", reason))
                .retrieve()
                .bodyToMono(Map.class);
    }

    // 예약결제 스케줄 생성 (PortOne V2 API)
    // 참고: https://developers.portone.io/opi/ko/integration/start/v2/billing/schedule?v=v2
    // POST /payments/{payment_id}/schedule 엔드포인트 사용
    // payment_id는 고유한 결제 ID여야 함
    public Mono<Map> createSchedule(String paymentId, Map<String, Object> scheduleRequest, String accessToken) {
        return webClient.post()
                .uri("/payments/{paymentId}/schedule", paymentId)
                .header("Authorization", "PortOne " + apiSecret)
                .header("Content-Type", "application/json")
                .bodyValue(scheduleRequest)
                .retrieve()
                .bodyToMono(Map.class);
    }

    // 예약결제 스케줄 조회
    public Mono<Map> getSchedule(String customerUid, String scheduleId, String accessToken) {
        return webClient.get()
                .uri("/billing-keys/{customerUid}/schedules/{scheduleId}", customerUid, scheduleId)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class);
    }

    // 예약결제 스케줄 목록 조회
    public Mono<Map> getSchedules(String customerUid, String accessToken) {
        return webClient.get()
                .uri("/billing-keys/{customerUid}/schedules", customerUid)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class);
    }

    // 예약결제 스케줄 삭제
    public Mono<Map> deleteSchedule(String customerUid, String scheduleId, String accessToken) {
        return webClient.delete()
                .uri("/billing-keys/{customerUid}/schedules/{scheduleId}", customerUid, scheduleId)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class);
    }

    // 빌링키 정보 조회
    public Mono<Map> getBillingKey(String customerUid, String accessToken) {
        return webClient.get()
                .uri("/billing-keys/{customerUid}", customerUid)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class);
    }
}


