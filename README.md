# KT 위치 문자 서비스 - 백엔드

## 프로젝트 구조
```
backend/
├── src/main/java/com/kt/campaign/
│   ├── controller/     # REST API 컨트롤러
│   ├── service/        # 비즈니스 로직 서비스 (인터페이스 적용)
│   ├── repository/     # 데이터 접근 계층
│   ├── entity/         # JPA 엔티티
│   ├── security/       # 인증/보안 설정
│   └── config/         # 설정 파일들
├── src/main/resources/ # 설정 파일 및 리소스
└── build.gradle       # 빌드 설정
```

## 기술 스택
- **Java 17**
- **Spring Boot 3.x**
- **Spring Security** (JWT 인증)
- **Spring Data JPA**
- **H2 Database** (개발용)
- **Gradle** (빌드 도구)

## 주요 기능
- 사용자 인증 및 JWT 토큰 관리
- 캠페인 생성 및 관리 (실시간 통계)
- 위치 기반 고객 필터링
- 포인트 시스템 (지갑)
- 관리자 기능 (고객 관리, ID 순 정렬)
- 메시지 읽음/클릭 추적

## 아키텍처 특징
- **서비스 인터페이스 적용**: 확장성과 테스트 용이성 향상
- **의존성 역전 원칙**: 고수준 모듈이 저수준 모듈에 의존하지 않음
- **트랜잭션 관리**: 데이터 일관성 보장
- **JWT 기반 인증**: Stateless 인증 시스템

## 실행 방법
```bash
./gradlew bootRun
```

서버는 http://localhost:8080 에서 실행됩니다.

## 주요 코드 파일 설명

### Controller 패키지 (REST API 계층)
- **AuthController.java**: 사용자 인증(로그인/회원가입) API 처리
- **CampaignController.java**: 캠페인 관련 API (생성, 조회, 통계, 미리보기) 처리  
- **WalletController.java**: 포인트 충전 및 거래 내역 API 처리
- **AdminController.java**: 관리자 전용 API (고객 관리, ID 순 정렬) 처리
- **CustomerController.java**: 고객 메시지 확인 및 상호작용 API 처리 (JPA 캐시 관리)
- **TrackingController.java**: 메시지 읽음/클릭 추적 API 처리

### Service 패키지 (비즈니스 로직 계층)
**인터페이스 기반 설계로 확장성과 테스트 용이성 향상**
- **AuthServiceInterface.java / AuthService.java**: 인증 관련 비즈니스 로직
- **CampaignServiceInterface.java / CampaignService.java**: 캠페인 관련 비즈니스 로직
- **WalletServiceInterface.java / WalletService.java**: 포인트 관련 비즈니스 로직

### Repository 패키지 (데이터 접근 계층)
- **AppUserRepository.java**: 사용자 데이터 접근
- **CampaignRepository.java**: 캠페인 데이터 접근
- **CustomerRepository.java**: 고객 데이터 접근 및 필터링 쿼리 (ID 순 정렬)
- **CampaignTargetRepository.java**: 캠페인 대상자 및 발송 결과, 시간별 통계 데이터 접근
- **WalletTransactionRepository.java**: 포인트 거래 내역 데이터 접근
- **ChatMessageRepository.java**: 채팅 메시지 데이터 접근

### Entity 패키지 (데이터 모델 계층)
- **AppUser.java**: 사용자 엔티티 (일반 사용자, 관리자)
- **Campaign.java**: 캠페인 엔티티 (제목, 내용, 상태 등)
- **Customer.java**: 고객 엔티티 (개인정보, 위치 정보)
- **CampaignTarget.java**: 캠페인 발송 대상 및 결과 엔티티
- **WalletTransaction.java**: 포인트 거래 내역 엔티티
- **ChatMessage.java**: 채팅 메시지 엔티티

### Security 패키지 (보안 계층)
- **SecurityConfig.java**: Spring Security 설정 (인증/인가 규칙)
- **JwtAuthenticationFilter.java**: JWT 토큰 검증 필터
- **JwtUtil.java**: JWT 토큰 생성 및 검증 유틸리티

### Config 패키지 (설정 계층)
- **CorsConfig.java**: CORS 설정 (프론트엔드와의 통신 허용)
- **JwtConfig.java**: JWT 관련 설정값 관리
- **DataInitializer.java**: 초기 데이터 설정

## 데이터베이스 스키마
- **app_users**: 사용자 정보 (일반/관리자)
- **customers**: 고객 정보 (위치 기반 필터링)
- **campaigns**: 캠페인 정보
- **campaign_targets**: 캠페인 발송 대상 및 결과
- **wallet_transactions**: 포인트 거래 내역
- **chat_messages**: 채팅 메시지