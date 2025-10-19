package com.sparta.payment_system.service;

import com.sparta.payment_system.dto.PortOnePaymentRequest;
import com.sparta.payment_system.dto.PortOnePaymentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class PortOneService {
    
    private final WebClient portOneWebClient;
    
    @Autowired
    public PortOneService(@Qualifier("portOneWebClient") WebClient portOneWebClient) {
        this.portOneWebClient = portOneWebClient;
    }
    
    public Mono<PortOnePaymentResponse> createPayment(PortOnePaymentRequest request) {
        return portOneWebClient
                .post()
                .uri("/payments")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PortOnePaymentResponse.class);
    }
    
    public Mono<PortOnePaymentResponse> getPayment(String impUid) {
        return portOneWebClient
                .get()
                .uri("/payments/" + impUid)
                .retrieve()
                .bodyToMono(PortOnePaymentResponse.class);
    }
    
    public Mono<PortOnePaymentResponse> cancelPayment(String impUid, String reason) {
        return portOneWebClient
                .post()
                .uri("/payments/" + impUid + "/cancel")
                .bodyValue(new CancelRequest(reason))
                .retrieve()
                .bodyToMono(PortOnePaymentResponse.class);
    }
    
    private static class CancelRequest {
        private String reason;
        
        public CancelRequest(String reason) {
            this.reason = reason;
        }
        
        public String getReason() {
            return reason;
        }
        
        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
