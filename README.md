# KT 위치 문자 서비스 - 백엔드

## 프로젝트 구조
```
backend/
├── src/main/java/com/kt/campaign/
│   ├── controller/             # REST API 컨트롤러
│   ├── service/                # 비즈니스 로직
│   ├── entity/                 # JPA 엔티티
│   ├── repository/             # 데이터 접근 계층
│   └── security/               # JWT 인증/보안
└── src/main/resources/
    ├── application.yml         # 설정 파일
    ├── postgresql-compose.yml  # PostgreSQL 설정 파일
    └── *.sql                   # 데이터베이스 초기화
```

## 기술 스택
- **Java 17**
- **Spring Boot 3.x**
- **Spring Security** (JWT 인증)
- **Spring Data JPA**
- **Gradle** (빌드 도구)

## 실행 방법
```bash
./gradlew bootRun
```

서버는 http://localhost:8080 에서 실행됩니다.

## 주요 파일 역할

### Controller
- `AuthController.java` - 로그인/회원가입 API
- `CampaignController.java` - 캠페인 생성/조회/발송 API
- `WalletController.java` - 포인트 충전/조회 API
- `AdminController.java` - 관리자 기능 API

### Service
- `AuthService.java` - 인증/사용자 관리 비즈니스 로직
- `CampaignService.java` - 캠페인 관리 및 통계 계산
- `WalletService.java` - 포인트 거래 처리

### Entity
- `AppUser.java` - 사용자 정보
- `Campaign.java` - 캠페인 정보
- `Customer.java` - 고객 정보
- `WalletTransaction.java` - 포인트 거래 내역

### Security
- `JwtUtil.java` - JWT 토큰 생성/검증
- `SecurityConfig.java` - Spring Security 설정
