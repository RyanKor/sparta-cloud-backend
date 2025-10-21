package com.sparta.payment_system.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.payment_system.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class WebhookController {
    
    @Value("${portone.webhook.secret}")
    private String webhookSecret;
    
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public WebhookController(PaymentService paymentService, ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/portone-webhook")
    public ResponseEntity<String> handlePortoneWebhook(
            @RequestBody String payload,
            @RequestHeader("PortOne-Signature") String signature,
            @RequestHeader("PortOne-Timestamp") String timestamp,
            @RequestHeader("PortOne-Webhook-Id") String webhookId) {
        
        System.out.println("웹훅 수신 성공!");
        System.out.println("Payload: " + payload);
        System.out.println("Signature: " + signature);
        System.out.println("Timestamp: " + timestamp);
        System.out.println("Webhook ID: " + webhookId);

        // 서명 검증
        if (!verifySignature(payload, signature, timestamp, webhookId)) {
            System.err.println("웹훅 서명 검증 실패");
            return ResponseEntity.status(401).body("Invalid signature");
        }

        // 서명 검증이 성공하면 비즈니스 로직 처리
        try {
            // TODO: 웹훅 이벤트 타입에 따른 처리 로직 추가
            // 예: 가상계좌 입금 확인, 결제 취소 등
            processWebhookEvent(payload);
            
            return ResponseEntity.ok("Webhook received successfully.");
        } catch (Exception e) {
            System.err.println("웹훅 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Webhook processing failed");
        }
    }

    private boolean verifySignature(String payload, String signature, String timestamp, String webhookId) {
        try {
            // 1. 타임스탬프 검증 (5분 이내인지 확인)
            long currentTime = System.currentTimeMillis() / 1000;
            long webhookTime = Long.parseLong(timestamp);
            if (Math.abs(currentTime - webhookTime) > 300) { // 5분 = 300초
                System.err.println("웹훅 타임스탬프가 너무 오래됨");
                return false;
            }

            // 2. HMAC-SHA256 서명 생성 (포트원 공식 방식)
            String expectedSignature = generateSignature(payload, timestamp, webhookId);
            
            // 3. 서명 비교 (보안을 위해 상수 시간 비교 사용)
            return constantTimeEquals(signature, expectedSignature);
        } catch (Exception e) {
            System.err.println("서명 검증 중 오류 발생: " + e.getMessage());
            return false;
        }
    }

    private String generateSignature(String payload, String timestamp, String webhookId) throws NoSuchAlgorithmException, InvalidKeyException {
        // 포트원 공식 서명 생성 방식: timestamp + "." + webhookId + "." + payload
        String message = timestamp + "." + webhookId + "." + payload;
        
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        
        byte[] signature = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature);
    }
    
    /**
     * 상수 시간 비교를 통한 보안 강화
     * 타이밍 공격을 방지하기 위해 문자열 비교 시간이 일정하도록 함
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        
        if (a.length() != b.length()) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        
        return result == 0;
    }

    private void processWebhookEvent(String payload) {
        try {
            // JSON 파싱
            Map<String, Object> webhookData = objectMapper.readValue(payload, Map.class);
            
            System.out.println("=== 웹훅 데이터 파싱 성공 ===");
            System.out.println("Webhook Data: " + webhookData);
            
            // 이벤트 타입별 처리
            String type = (String) webhookData.get("type");
            Map<String, Object> data = (Map<String, Object>) webhookData.get("data");
            
            if (data == null) {
                System.err.println("웹훅 데이터에 'data' 필드가 없습니다.");
                return;
            }
            
            // payment_id 추출
            String paymentId = (String) data.get("payment_id");
            String txId = (String) data.get("tx_id");
            String status = (String) data.get("status");
            
            System.out.println("Payment ID: " + paymentId);
            System.out.println("Transaction ID: " + txId);
            System.out.println("Status: " + status);
            System.out.println("Status 비교 - 'PAID'.equals(status): " + "PAID".equals(status));
            System.out.println("Status 비교 - 'Paid'.equals(status): " + "Paid".equals(status));
            
            // 결제 완료 상태일 때 자동 검증
            if ("PAID".equals(status) || "Paid".equals(status)) {
                System.out.println("결제 완료 상태 감지 - 자동 검증 시작");
                
                paymentService.verifyPayment(paymentId)
                    .subscribe(
                        isSuccess -> {
                            if (isSuccess) {
                                System.out.println("✅ 웹훅을 통한 결제 검증 성공!");
                            } else {
                                System.err.println("❌ 웹훅을 통한 결제 검증 실패");
                            }
                        },
                        error -> System.err.println("웹훅 결제 검증 중 오류: " + error.getMessage())
                    );
            } else if ("Ready".equals(status)) {
                System.out.println("결제 준비 상태 - 결제 대기 중");
            } else if ("Cancelled".equals(status)) {
                System.out.println("결제 취소 상태 감지");
            }
            
            System.out.println("웹훅 이벤트 처리 완료");
            
        } catch (Exception e) {
            System.err.println("웹훅 이벤트 처리 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
