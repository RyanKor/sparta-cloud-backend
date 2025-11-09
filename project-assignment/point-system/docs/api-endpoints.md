# 포인트 기반 결제 시스템 API 엔드포인트 목록

## 📋 목차
1. [주문 관련 API](#주문-관련-api)
2. [결제 관련 API](#결제-관련-api) ⭐ 핵심
3. [포인트 관련 API](#포인트-관련-api)
4. [멤버십 관련 API](#멤버십-관련-api)
5. [환불 관련 API](#환불-관련-api)

---

## 주문 관련 API

### 기본 CRUD
- `POST /api/order` - 주문 생성
- `GET /api/orders` - 전체 주문 목록 조회
- `GET /api/order/{orderId}` - 주문 상세 조회 ⭐
- `GET /api/orders/user/{userId}` - 사용자별 주문 목록 조회
- `GET /api/orders/status/{status}` - 상태별 주문 목록 조회
- `PUT /api/order/{orderId}` - 주문 정보 수정
- `DELETE /api/order/{orderId}` - 주문 삭제

---

## 결제 관련 API ⭐ 핵심

### 결제 처리 (핵심)
- `POST /api/payments/request` - **통합 결제 요청** (주문 생성 + 포인트 사용 + 결제 처리) ⭐⭐⭐
- `POST /api/payments/complete` - 결제 완료 검증 (PortOne)
- `POST /api/payments/complete-point-payment` - 포인트 전액 결제 완료 처리 ⭐

### 결제 취소
- `POST /api/payments/cancel` - 결제 취소 (PortOne, 포인트 환불 포함) ⭐

### 결제 조회
- `GET /api/payments/payments` - 전체 결제 목록 조회
- `GET /api/payments/payment/{paymentId}` - 결제 상세 조회
- `GET /api/payments/payment/order/{orderId}` - 주문별 결제 조회
- `GET /api/payments/payments/status/{status}` - 상태별 결제 목록 조회
- `GET /api/payments/paid` - PAID 상태 결제 목록 조회 (환불 가능한 결제)

### 결제 관리
- `POST /api/payments/payment` - 결제 생성
- `PUT /api/payments/payment/{paymentId}` - 결제 정보 수정
- `DELETE /api/payments/payment/{paymentId}` - 결제 삭제

---

## 포인트 관련 API

### 포인트 조회 및 충전
- `GET /api/points/balance/{userId}` - 포인트 잔액 조회 ⭐
- `POST /api/points/charge/{userId}` - 포인트 충전 ⭐
- `GET /api/points/transactions/{userId}` - 포인트 거래 내역 조회

---

## 멤버십 관련 API

### 멤버십 정보 조회 (핵심)
- `GET /api/membership/user/{userId}/info` - **멤버십 정보 및 등급 조회** (총 결제 금액 포함) ⭐⭐⭐
- `GET /api/membership/user/{userId}/payments` - **사용자 결제 내역 조회** (완료/취소 주문 포함) ⭐⭐

### 멤버십 관리
- `POST /api/membership` - 멤버십 생성
- `GET /api/memberships` - 전체 멤버십 목록 조회
- `GET /api/membership/{membershipId}` - 멤버십 상세 조회
- `GET /api/membership/user/{userId}` - 사용자별 멤버십 조회
- `PUT /api/membership/{membershipId}` - 멤버십 정보 수정
- `DELETE /api/membership/{membershipId}` - 멤버십 삭제

### 멤버십 등급 관리
- `POST /api/membership-level` - 멤버십 등급 생성
- `GET /api/membership-levels` - 전체 멤버십 등급 목록 조회
- `GET /api/membership-level/{levelId}` - 멤버십 등급 상세 조회
- `GET /api/membership-level/name/{name}` - 등급명으로 조회
- `PUT /api/membership-level/{levelId}` - 멤버십 등급 수정
- `DELETE /api/membership-level/{levelId}` - 멤버십 등급 삭제

---

## 환불 관련 API

### 환불 처리 (핵심)
- `POST /api/refunds/request` - **환불 요청** (포인트 환불 포함, 멤버십 등급 갱신) ⭐⭐⭐

### 환불 조회
- `GET /api/refunds/refunds` - 전체 환불 목록 조회
- `GET /api/refunds/refund/{refundId}` - 환불 상세 조회
- `GET /api/refunds/refunds/payment/{paymentId}` - 결제별 환불 목록 조회

### 환불 관리
- `POST /api/refunds/refund` - 환불 생성
- `PUT /api/refunds/refund/{refundId}` - 환불 정보 수정
- `DELETE /api/refunds/refund/{refundId}` - 환불 삭제