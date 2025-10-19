package com.sparta.payment_system.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@RestController
@CrossOrigin(origins = "*")
public class WebhookController {
    
    @Value("${portone.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/portone-webhook")
    public ResponseEntity<String> handlePortoneWebhook(
            @RequestBody String payload,
            @RequestHeader("PortOne-Signature") String signature,
            @RequestHeader("PortOne-Timestamp") String timestamp) {
        
        System.out.println("웹훅 수신 성공!");
        System.out.println("Payload: " + payload);
        System.out.println("Signature: " + signature);
        System.out.println("Timestamp: " + timestamp);

        // 서명 검증
        if (!verifySignature(payload, signature, timestamp)) {
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

    private boolean verifySignature(String payload, String signature, String timestamp) {
        try {
            // 1. 타임스탬프 검증 (5분 이내인지 확인)
            long currentTime = System.currentTimeMillis() / 1000;
            long webhookTime = Long.parseLong(timestamp);
            if (Math.abs(currentTime - webhookTime) > 300) { // 5분 = 300초
                System.err.println("웹훅 타임스탬프가 너무 오래됨");
                return false;
            }

            // 2. HMAC-SHA256 서명 생성
            String expectedSignature = generateSignature(payload, timestamp);
            
            // 3. 서명 비교
            return signature.equals(expectedSignature);
        } catch (Exception e) {
            System.err.println("서명 검증 중 오류 발생: " + e.getMessage());
            return false;
        }
    }

    private String generateSignature(String payload, String timestamp) throws NoSuchAlgorithmException, InvalidKeyException {
        String message = timestamp + "." + payload;
        
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        
        byte[] signature = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature);
    }

    private void processWebhookEvent(String payload) {
        // TODO: 웹훅 이벤트 타입에 따른 처리 로직 구현
        // 예시:
        // - PAYMENT_STATUS_CHANGED: 결제 상태 변경 처리
        // - VIRTUAL_ACCOUNT_DEPOSITED: 가상계좌 입금 확인 처리
        // - PAYMENT_CANCELLED: 결제 취소 처리
        
        System.out.println("웹훅 이벤트 처리 완료");
    }
}
