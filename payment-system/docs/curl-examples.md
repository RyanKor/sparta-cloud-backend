# User API 테스트용 curl 명령어 예시

## 기본 설정
- 기본 URL: `http://localhost:8080/api/test`
- Content-Type: `application/x-www-form-urlencoded` (POST/PUT 요청용)

## 1. 사용자 생성 (POST)

### 기본 사용자 생성
```bash
curl -X POST "http://localhost:8080/api/test/user" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "email=john.doe@example.com&passwordHash=hashedpassword123&name=John Doe"
```

### 이름 없이 사용자 생성
```bash
curl -X POST "http://localhost:8080/api/test/user" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "email=jane.smith@example.com&passwordHash=anotherhashedpassword456"
```

### 여러 사용자 생성 예시
```bash
# 사용자 1
curl -X POST "http://localhost:8080/api/test/user" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "email=alice@example.com&passwordHash=alice123&name=Alice Johnson"

# 사용자 2
curl -X POST "http://localhost:8080/api/test/user" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "email=bob@example.com&passwordHash=bob456&name=Bob Wilson"

# 사용자 3
curl -X POST "http://localhost:8080/api/test/user" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "email=charlie@example.com&passwordHash=charlie789&name=Charlie Brown"
```

## 2. 모든 사용자 조회 (GET)

```bash
curl -X GET "http://localhost:8080/api/test/users"
```

## 3. 특정 사용자 조회 (GET)

### ID로 사용자 조회
```bash
curl -X GET "http://localhost:8080/api/test/user/1"
```

### 이메일로 사용자 조회
```bash
curl -X GET "http://localhost:8080/api/test/user/email/john.doe@example.com"
```

## 4. 사용자 정보 수정 (PUT)

### 이메일 수정
```bash
curl -X PUT "http://localhost:8080/api/test/user/1" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "email=newemail@example.com"
```

### 비밀번호 해시 수정
```bash
curl -X PUT "http://localhost:8080/api/test/user/1" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "passwordHash=newhashedpassword789"
```

### 이름 수정
```bash
curl -X PUT "http://localhost:8080/api/test/user/1" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "name=New Name"
```

### 여러 필드 동시 수정
```bash
curl -X PUT "http://localhost:8080/api/test/user/1" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "email=updated@example.com&name=Updated Name&passwordHash=newhash123"
```

## 5. 사용자 삭제 (DELETE)

```bash
curl -X DELETE "http://localhost:8080/api/test/user/1"
```

## 6. 전체 테스트 시나리오

### 1단계: 사용자 생성
```bash
curl -X POST "http://localhost:8080/api/test/user" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "email=test@example.com&passwordHash=test123&name=Test User"
```

### 2단계: 생성된 사용자 확인
```bash
curl -X GET "http://localhost:8080/api/test/users"
```

### 3단계: 특정 사용자 조회
```bash
curl -X GET "http://localhost:8080/api/test/user/email/test@example.com"
```

### 4단계: 사용자 정보 수정
```bash
curl -X PUT "http://localhost:8080/api/test/user/1" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "name=Updated Test User"
```

### 5단계: 수정된 정보 확인
```bash
curl -X GET "http://localhost:8080/api/test/user/1"
```

### 6단계: 사용자 삭제
```bash
curl -X DELETE "http://localhost:8080/api/test/user/1"
```

## 7. JSON 응답 확인을 위한 jq 사용 예시

### JSON 포맷팅으로 응답 확인
```bash
curl -X GET "http://localhost:8080/api/test/users" | jq '.'
```

### 특정 필드만 추출
```bash
curl -X GET "http://localhost:8080/api/test/users" | jq '.[].email'
```

## 8. 에러 테스트

### 중복 이메일로 사용자 생성 시도
```bash
curl -X POST "http://localhost:8080/api/test/user" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "email=test@example.com&passwordHash=test123&name=Test User"

# 같은 이메일로 다시 생성 시도
curl -X POST "http://localhost:8080/api/test/user" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "email=test@example.com&passwordHash=test456&name=Another User"
```

### 존재하지 않는 사용자 조회
```bash
curl -X GET "http://localhost:8080/api/test/user/999"
```

### 존재하지 않는 사용자 수정
```bash
curl -X PUT "http://localhost:8080/api/test/user/999" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "name=Updated Name"
```

## 참고사항

1. **포트 번호**: 기본적으로 8080 포트를 사용합니다. 다른 포트를 사용하는 경우 URL을 수정하세요.

2. **데이터베이스**: 실제 운영 환경에서는 password_hash를 평문으로 저장하지 말고 적절한 해싱 알고리즘을 사용하세요.

3. **에러 처리**: 현재 구현에서는 간단한 RuntimeException을 사용하고 있습니다. 실제 서비스에서는 적절한 예외 처리를 구현하세요.

4. **보안**: 테스트용 컨트롤러이므로 실제 운영 환경에서는 인증/인가 로직을 추가하세요.
