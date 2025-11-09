# 포인트 기반 결제 시스템 API 엔드포인트 목록

> **참고**: 이 문서는 `point-payment.html`에서 실제로 사용하는 API만 포함합니다.

## 📋 목차
1. [인증 관련 API](#인증-관련-api) 🔐 필수
2. [포인트 관련 API](#포인트-관련-api)
3. [멤버십 관련 API](#멤버십-관련-api)
4. [결제 관련 API](#결제-관련-api) ⭐ 핵심
5. [주문 관련 API](#주문-관련-api)
6. [상품 관련 API](#상품-관련-api)

---

## 1. 인증 관련 API 🔐 필수

### 회원가입 및 로그인
- `POST /api/auth/register` - **회원가입** (JWT 토큰 발급) 🔐
  - Request Body: `{ "email": "string", "password": "string", "name": "string" }`
  - Response: `{ "token": "string", "email": "string", "userId": number, "name": "string", "message": "string" }`
- `POST /api/auth/login` - **로그인** (JWT 토큰 발급) 🔐
  - Request Body: `{ "email": "string", "password": "string" }`
  - Response: `{ "token": "string", "email": "string", "userId": number, "name": "string", "message": "string" }`
- `GET /api/auth/me` - **현재 사용자 정보 조회** 🔐
  - Headers: `Authorization: Bearer {token}`
  - Response: `{ "userId": number, "email": "string", "name": "string" }`

**참고**: 
- 모든 보호된 API는 `Authorization: Bearer {token}` 헤더가 필요합니다.
- 인증 가이드는 `point-payment-implementation.md`의 "7. 인증 기능 추가하기" 섹션을 참고하세요.

---

## 2. 포인트 관련 API

### 포인트 조회 및 충전
- `GET /api/points/balance/{userId}` - 포인트 잔액 조회 ⭐
- `POST /api/points/charge/{userId}` - 포인트 충전 ⭐
  - Query Parameters: `points` (충전할 포인트), `description` (설명)

---

## 3. 멤버십 관련 API

### 멤버십 정보 조회 (핵심)
- `GET /api/membership/user/{userId}/info` - **멤버십 정보 및 등급 조회** (총 결제 금액 포함) ⭐⭐⭐
- `GET /api/membership/user/{userId}/payments` - **사용자 결제 내역 조회** (완료/취소 주문 포함) ⭐⭐

---

## 4. 결제 관련 API ⭐ 핵심

### 결제 처리 (핵심)
- `POST /api/payments/request` - **통합 결제 요청** (주문 생성 + 포인트 사용 + 결제 처리) ⭐⭐⭐
- `POST /api/payments/complete` - 결제 완료 검증 (PortOne) ⭐
- `POST /api/payments/complete-point-payment` - 포인트 전액 결제 완료 처리 ⭐

---

## 5. 주문 관련 API

### 주문 조회
- `GET /api/order/{orderId}` - 주문 상세 조회 ⭐

---

## 6. 상품 관련 API

### 상품 조회 및 생성
- `GET /api/product/{productId}` - 상품 정보 조회 ⭐
- `POST /api/product` - 상품 생성 (테스트용)

---

## 🔥 핵심 API 요약 (가장 중요)

### 인증 관련 핵심 API (필수)
0. **`POST /api/auth/register`** - 회원가입 (JWT 토큰 발급)
1. **`POST /api/auth/login`** - 로그인 (JWT 토큰 발급)
2. **`GET /api/auth/me`** - 현재 사용자 정보 조회

### 결제 플로우 핵심 API
3. **`POST /api/payments/request`** - 통합 결제 요청 (주문 생성 + 포인트 사용 + 결제 처리)
4. **`POST /api/payments/complete`** - 결제 완료 검증 (PortOne)
5. **`POST /api/payments/complete-point-payment`** - 포인트 전액 결제 완료

### 멤버십 관련 핵심 API
6. **`GET /api/membership/user/{userId}/info`** - 멤버십 정보 및 등급 조회 (총 결제 금액 포함)
7. **`GET /api/membership/user/{userId}/payments`** - 사용자 결제 내역 조회

### 포인트 관련 핵심 API
8. **`GET /api/points/balance/{userId}`** - 포인트 잔액 조회
9. **`POST /api/points/charge/{userId}`** - 포인트 충전

### 기타 조회 API
10. **`GET /api/order/{orderId}`** - 주문 상세 조회
11. **`GET /api/product/{productId}`** - 상품 정보 조회

---

## 📝 참고사항

- **인증 필수**: 대부분의 API는 JWT 토큰이 필요합니다. `Authorization: Bearer {token}` 헤더를 포함해야 합니다.
- 모든 API는 `/api` 경로를 기본으로 사용합니다
- 멤버십 등급은 총 결제 금액에 따라 자동으로 업데이트됩니다:
  - 5만원 이하: Normal (1% 적립)
  - 10만원 이하: VIP (5% 적립)
  - 15만원 이상: VVIP (10% 적립)
- 결제 완료 및 취소 시 멤버십 등급이 자동으로 갱신됩니다
- `point-payment.html`에서 실제로 호출하는 API만 포함되어 있습니다
- **인증 구현 가이드**: `point-payment-implementation.md`의 "7. 인증 기능 추가하기" 섹션 참고
