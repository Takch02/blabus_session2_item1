# 📢 NAFAL (나팔) - 대규모 트래픽을 고려한 업사이클링 역경매 플랫폼

> **블레이버스 MVP 해커톤 시즌 2 최우수상 수상작 🏆**
> 
> "초기 MVP 개발에 그치지 않고, 실제 대규모 운영 환경을 가정하여 **데이터 정합성 보장**과 **조회 성능 최적화**를 집요하게 파고든 백엔드 리팩토링 프로젝트입니다."

<br>

## 📌 Project Overview
- **Phase 1 (해커톤):** 2025.08.12 ~ 2025.08.25 / 5인 팀 (프론트엔드 UI/API 연동 및 백오피스 개발)
- **Phase 2 (아키텍처 리팩토링):** 2025.11 ~ 2026.01 / 1인 개인 (백엔드 아키텍처 고도화)
- **기술 블로그 (Trouble Shooting):** [https://velog.io/@takch02/series/리펙토링]

<br>

## 🛠 Tech Stack
- **Backend:** Java, Spring Boot, Spring Data JPA, QueryDSL
- **Database:** MySQL
- **Infra/Test:** AWS (EC2, RDS, S3), GitHub Actions, K6 (부하 테스트), JUnit5, Mockito

<br>

## 🚀 Key Refactoring Achievements (아키텍처 고도화)

### 1. 300만 건 대용량 경매 데이터 조회 성능 2,300배 개선 (3900ms ➡️ 16ms)
- **🚨 Problem:** 기존 JPA `findAll()` 및 `OFFSET` 기반 Paging 방식에서 COUNT 쿼리 병목과 Full Scan으로 인해 Connection Pool 고갈 및 높은 응답 지연 발생 (OOM 위험).
- **💡 Solution:**
  - **No-Offset (Page Slice) 전환:** 불필요한 COUNT 쿼리 제거로 DB 부하 감소.
  - **Covering Index 도입:** 인덱스에 포함된 식별자만 선조회하는 Index Only Scan 방식 적용.
  - **Deferred Join 적용:** 선조회된 식별자를 바탕으로 실제 데이터 건수만큼만 원본 테이블과 Join 하여 데이터 추출.
- **📊 Result:** K6 부하 테스트 환경에서 조회 응답 속도 **3900ms ➡️ 16ms**로 대폭 단축, 에러율 **12.5% ➡️ 0%** 달성.

### 2. Outbox Pattern 기반 Deadlock 해결 및 100% 데이터 정합성 보장
- **🚨 Problem:** '입찰 트랜잭션(Auction Lock -> User Lock)'과 '유저 정보 수정 트랜잭션(User Lock -> Auction Lock)'이 서로 다른 순서로 Lock을 획득하면서 **교차 락(Cross Lock)에 의한 Deadlock** 발생.
- **💡 Solution:**
  - **이벤트 기반 아키텍처 분리:** `@TransactionalEventListener`를 활용해 유저 업데이트 로직을 비동기 이벤트로 분리하여 Lock 충돌 원천 차단.
  - **Outbox Pattern 도입:** 메인 트랜잭션 커밋 전, 이벤트를 Outbox 테이블에 먼저 저장하여 메인 로직과 이벤트 발행 간의 원자성(Atomicity) 확보.
  - **TSID 및 Scheduler 적용:** TSID 기반 인덱싱으로 고속 조회 환경을 구축하고, 스케줄러를 통해 미처리 이벤트를 재전송하여 **결과적 일관성(Eventual Consistency)** 보장.
- **📊 Result:** Deadlock 이슈 완벽 제거 및 고의적인 이벤트 유실 테스트 환경(Mockito 기반)에서도 100% 재처리 검증 완료.

### 3. MSA 전환을 대비한 도메인 중심 아키텍처(Package by Feature) 재편
- **🚨 Problem:** 초기 개발 시 계층형 구조(Package by Layer)를 채택하여, 서비스 규모가 커짐에 따라 도메인 간의 결합도가 높아지고(High Coupling) 비즈니스 흐름을 파악하기 어려운 유지보수 한계 직면.
- **💡 Solution:**
  - **도메인 중심 패키지 분리:** 기존 `controller/`, `service/`, `repository/` 구조를 `auction/`, `user/`, `bid/` 등 비즈니스 도메인(Feature) 단위로 구조 전면 리팩토링.
  - **의존성 격리:** 각 도메인이 독립적으로 동작할 수 있도록 패키지 간 순환 참조 및 강결합을 끊어내고 인터페이스 기반으로 통신하도록 개선.
- **📊 Result:** 도메인 내의 응집도를 높이고 타 도메인과의 결합도를 낮추어 코드 가독성 및 유지보수성을 항샹.

<br>

## 🏆 Hackathon Achievements (Phase 1)
**블레이버스 MVP 해커톤 시즌 2 최우수상 수상**
- 프론트엔드 인력 이탈 위기 상황에서 풀스택 개발로 전향.
- React 기반 UI 설계부터 백오피스 환경 구축, API 연동까지 기한 내 성공적으로 완수하여 프로젝트 수상을 견인.

<br>

## 📐 Architecture & Trouble Shooting 상세

- **[Trouble Shooting 1]** [Covering Index와 Deferred Join을 통한 성능 개선기](https://velog.io/@takch02/리팩토링-프로젝트를-리팩토링-하자-5)
  
- **[Trouble Shooting 2]** 이벤트 유실 방지와 Deadlock 해결을 위한 Outbox 패턴 도입기
  <img width="512" height="798" alt="ChatGPT Image 2026년 3월 6일 오후 11_41_59-2" src="https://github.com/user-attachments/assets/31437a81-9f34-4d70-8a27-7f297550e4bd" />

   [Deadlock 해결](https://velog.io/@takch02/리팩토링-프로젝트를-리팩토링-하자-6), [Outbox 도입](https://velog.io/@takch02/리팩토링-프로젝트를-리팩토링-하자-7)

<br>
