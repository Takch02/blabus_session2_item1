# 🏷️ Highlight Backend

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.5-6DB33F?style=flat&logo=spring-boot)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white)](https://www.docker.com/)
[![AWS](https://img.shields.io/badge/AWS-232F3E?style=flat&logo=amazon-aws&logoColor=white)](https://aws.amazon.com/)

블레이버스 해커톤 2025 - Highlight 팀의 **실시간 경매 플랫폼** 백엔드 API 서버

## 📖 프로젝트 개요

실시간 경매 시스템을 통해 사용자가 상품에 입찰하고, 즉시 구매할 수 있는 온라인 경매 플랫폼의 백엔드 서비스입니다.
WebSocket을 활용한 실시간 입찰, JWT 기반 인증, AWS S3 이미지 관리, SMS 인증 등의 기능을 제공합니다.

## 🛠️ 기술 스택

### Backend Framework
- **Spring Boot** 3.4.5 (Java 17)
- **Spring Data JPA** - 데이터 액세스 레이어
- **Spring Security** - 인증 및 보안
- **Spring WebSocket** - 실시간 통신 (STOMP)

### Database & Storage
- **MySQL** 8.0 - 메인 데이터베이스 (AWS RDS)
- **AWS S3** - 이미지 파일 저장소
- **H2** - 테스트용 인메모리 데이터베이스

### Authentication & Security
- **JWT** (JSON Web Token) - Access/Refresh Token 기반 인증
- **CoolSMS** - SMS 휴대폰 인증

### Infrastructure & Deployment
- **AWS EC2** - 서버 호스팅 (프리티어)
- **Docker & Docker Compose** - 컨테이너 환경
- **Gradle** 8.4 - 빌드 도구

### Documentation & Monitoring
- **Swagger/OpenAPI** 3.0 - API 문서화
- **Spring Actuator** - 헬스체크 및 모니터링

## 🏗️ 프로젝트 구조

```
src/main/java/com/highlight/highlight_backend/
├── config/               # 설정 클래스들
│   ├── SecurityConfig.java
│   ├── WebSocketConfig.java
│   └── SwaggerConfig.java
├── controller/           # REST API 컨트롤러 (18개 엔드포인트)
│   ├── user/            # 사용자 관련 컨트롤러
│   └── dashboard/       # 대시보드 관련 컨트롤러
├── domain/              # JPA 엔티티 (11개 도메인)
├── dto/                 # 데이터 전송 객체
├── service/             # 비즈니스 로직 (14개 서비스)
├── repository/          # 데이터 액세스 레이어
├── security/            # JWT 인증 필터
├── exception/           # 전역 예외 처리
└── util/               # 유틸리티 클래스들
```

## ⚡ 주요 기능

### 👤 사용자 관리
- 휴대폰 번호를 통한 SMS 인증 회원가입/로그인
- JWT 기반 Access/Refresh Token 인증
- 마이페이지 및 사용자 정보 관리
- 사용자 랭킹 시스템

### 🏺 상품 및 경매 관리
- 상품 등록, 수정, 삭제 (이미지 업로드 포함)
- 경매 일정 관리 및 자동 시작/종료
- 실시간 경매 상태 관리
- 상품 찜하기 및 알림 설정
- 연관 상품 추천 시스템

### 💰 입찰 및 결제
- 실시간 입찰 시스템 (WebSocket)
- 즉시구매 기능
- 입찰 히스토리 및 알림
- 결제 미리보기 및 처리

### 🔧 관리자 기능
- 상품 및 경매 관리
- 사용자 관리
- 대시보드 통계
- 판매자 관리

### 📊 실시간 기능
- WebSocket을 통한 실시간 입찰 알림
- 경매 상태 실시간 업데이트
- 입찰 경합 실시간 알림

## 🚀 실행 방법

### 환경 요구사항
- Java 17+
- Docker & Docker Compose
- MySQL 8.0 (또는 Docker 컨테이너 사용)

### 1. 프로젝트 클론
```bash
git clone https://github.com/blaybus-highlight/Highlight_Backend.git
cd Highlight_Backend
```

### 2. 환경변수 설정
`.env` 파일을 생성하고 다음 변수들을 설정하세요:

```bash
# Database
DB_NAME=highlight_db
DB_USERNAME=highlight_user
DB_PASSWORD=your_password
DB_PORT=3306
MYSQL_ROOT_PASSWORD=root_password
DB_DDL_AUTO=update
DB_SHOW_SQL=false

# Server
SERVER_PORT=8085

# JWT
JWT_SECRET=your_jwt_secret_key
JWT_ACCESS_TOKEN_EXPIRE_TIME=1800000
JWT_REFRESH_TOKEN_EXPIRE_TIME=604800000

# AWS S3
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
AWS_REGION=ap-northeast-2
S3_BUCKET_NAME=your_bucket_name

# CoolSMS
COOLSMS_API_KEY=your_api_key
COOLSMS_API_SECRET=your_api_secret
COOLSMS_FROM_NUMBER=your_phone_number
```

### 3. Docker로 실행
```bash
# 전체 스택 실행 (MySQL + Backend)
docker-compose up -d

# 로그 확인
docker-compose logs -f highlight-backend
```

### 4. 로컬 개발 실행
```bash
# 데이터베이스만 Docker로 실행
docker-compose up -d highlight-db

# 애플리케이션 로컬 실행
./gradlew bootRun
```

### 5. 테스트 실행
```bash
./gradlew test
```

## 📚 API 문서

### Production
- **Swagger UI**: http://ec2-52-78-128-131.ap-northeast-2.compute.amazonaws.com:8085/swagger-ui/index.html
- **OpenAPI JSON**: http://ec2-52-78-128-131.ap-northeast-2.compute.amazonaws.com:8085/api-docs

### Local Development
- **Swagger UI**: http://localhost:8085/swagger-ui/index.html
- **OpenAPI JSON**: http://localhost:8085/api-docs

### 주요 API 엔드포인트

#### 🔐 인증 관련
- `POST /api/auth/signup` - 회원가입
- `POST /api/auth/login` - 로그인
- `POST /api/auth/phone/send` - SMS 인증 코드 발송
- `POST /api/auth/phone/verify` - SMS 인증 코드 검증

#### 🏺 상품 관련
- `GET /api/products` - 상품 목록 조회
- `POST /api/products` - 상품 등록
- `GET /api/products/{id}` - 상품 상세 조회
- `PUT /api/products/{id}` - 상품 수정

#### 🏃‍♂️ 경매 관련
- `GET /api/auctions` - 경매 목록 조회
- `POST /api/auctions/{id}/start` - 경매 시작
- `POST /api/auctions/{id}/end` - 경매 종료
- `GET /api/auctions/{id}/status` - 경매 상태 조회

#### 💰 입찰 관련
- `POST /api/bids` - 입찰하기
- `GET /api/bids/auction/{auctionId}` - 경매별 입찰 내역
- `POST /api/buy-now` - 즉시구매

## 🏗️ 인프라 구성

### AWS 아키텍처
```
Internet Gateway
    ↓
Application Load Balancer
    ↓
EC2 Instance (t2.micro)
├── Spring Boot Application (Port 8085)
├── MySQL Container (Port 3306)
└── Docker Network
    ↓
AWS S3 Bucket (이미지 저장)
```

### 데이터베이스 스키마
- **User** - 사용자 정보
- **Product** - 상품 정보
- **Auction** - 경매 정보
- **Bid** - 입찰 정보
- **ProductImage** - 상품 이미지
- **ProductWishlist** - 상품 찜
- **ProductNotification** - 상품 알림
- **Seller** - 판매자 정보
- **Admin** - 관리자 정보

## 🧪 테스트

```bash
# 전체 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "com.highlight.highlight_backend.*"

# 테스트 커버리지 확인
./gradlew jacocoTestReport
```

## 📊 모니터링 및 헬스체크

```bash
# 애플리케이션 상태 확인
curl http://localhost:8085/actuator/health

# 메트릭 확인
curl http://localhost:8085/actuator/metrics
```

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 라이선스

이 프로젝트는 MIT 라이선스 하에 있습니다. 자세한 내용은 `LICENSE` 파일을 참조하세요.

## 👥 팀원

| 이름 | 역할 | GitHub                                 |
|------|------|----------------------------------------|
| 전우선 | 백엔드 개발 | [@wooxexn](https://github.com/wooxexn) |
| 탁찬홍 | 백엔드 개발 | [@Takch02](https://github.com/Takch02)                   |

---

**📧 문의사항**: 프로젝트 관련 문의사항이 있으시면 GitHub Issues를 통해 연락 부탁드립니다.