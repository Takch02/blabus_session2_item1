# 📢 NAFAL (나팔) - 대규모 트래픽을 고려한 업사이클링 역경매 플랫폼

> **블레이버스 MVP 해커톤 시즌 2 최우수상 수상작 🏆**
> 
> "초기 MVP 개발에 그치지 않고, 실제 대규모 운영 환경을 가정하여 **데이터 정합성 보장(Exactly-Once)**과 **조회 성능 및 동시성 최적화**를 집요하게 파고든 백엔드 아키텍처 고도화 프로젝트입니다."

<br>

## 📌 Project Overview
- **Phase 1 (해커톤):** 2025.08.12 ~ 2025.08.25 / 5인 팀 (백오피스 및 경매 조회 로직 개발, 프론트 인력 이탈 대응으로 UI/API 연동 전담") 
- **Phase 2 (아키텍처 리팩토링):** 2025.11 ~ 2026.03 / 1인 개인 (백엔드 아키텍처 고도화)
- **기술 블로그 (Trouble Shooting):** [벨로그 리팩토링 시리즈](https://velog.io/@takch02/series/%EB%A6%AC%ED%8E%99%ED%86%A0%EB%A7%81)

<br>

## 🛠 Tech Stack
- **Backend:** Java, Spring Boot, Spring Data JPA, QueryDSL
- **Database / Cache:** MySQL, Redis (Redisson) 
- **Infra/Test:** AWS (EC2, RDS, S3), GitHub Actions, K6 (부하 테스트), JUnit5, Mockito

<br>

## 🏆 Hackathon Achievements (Phase 1)
**블레이버스 MVP 해커톤 시즌 2 최우수상 수상**
- 프론트엔드 인력 이탈 상황에서 UI/API 연동을 자발적으로 전담하여 팀의 개발 공백을 메움
- React 기반 UI 설계부터 백오피스 환경 구축, API 연동까지 전체 사이클을 기한 내 성공적으로 완수하여 프로젝트 수상을 견인.

<br>

## 🚀 Key Refactoring Achievements (Phase 2)

### 1. Payload-State 역할 분리 기반의 부분 실패 복구 및 Exactly-Once 보장
- **🚨 Problem:** 입찰 트랜잭션과 User 정보 수정 간 교차 Lock 획득으로 **Deadlock** 발생. 비동기 분리 시 일부 로직만 실패하는 **부분 실패** 위험 및 무거운 이벤트 데이터 중복 저장으로 인한 DB 부하 우려.
- **💡 Solution:**
  - **비동기 분리 및 Claim Check 패턴:** 로직을 비동기로 분리해 Lock 충돌을 없애고, 이벤트 본체(Payload)는 `Outbox` 테이블에 단일 저장.
  - **Consumer Log 도입:** 실행 상태만 추적하는 가벼운 `Consumer Log` 티켓을 동시 발행하고, 수신 측에서 상태를 사전 검증하여 **멱등성 확보**.
  - **독립적 재처리 파이프라인:** FAILED, Pending상태의 Log만 스케줄러가 추적하여 원본 이벤트를 꺼내 재실행.
    
  <img width="932" height="519" alt="스크린샷 2026-03-17 오후 10 25 31" src="https://github.com/user-attachments/assets/8062e628-4599-493f-b0e1-af77c67aa00c" />

- **📊 Result:**
  - **Deadlock 원천 차단:** **100개 스레드 규모의 동시성 부하 테스트**를 통해 트랜잭션 분리에 따른 **DeadLock 미발생 검증**
  - **결과적 일관성 100% 증명:** Mockito를 활용해 **비동기 이벤트의 의도적 부분 실패 상황을 재현** 후 Consumer Log 스케줄러의 개입을 통한 **자동 복구 및 멱등성 유지 로직 검증**
- **블로그:** : [Consumer Log 도입](https://velog.io/@takch02/리팩토링-프로젝트를-리팩토링-하자-8Consumer-Log-패턴)

### 2. Redisson 분산 락 도입을 통한 DB Connection Pool 고갈 해결
- **🚨 Problem:** 비관적 락(Pessimistic Lock) 기반 입찰 로직이 대기 시간 동안 DB Connection을 계속 점유하여, 단순 경매 조회 등 타 로직까지 마비되는 병목 현상 발생 (조회 1,003ms 지연).
- **💡 Solution:**
  - **Redisson Pub/Sub 분산 락:** Lock 관리를 애플리케이션(Redis) 계층으로 분리하여 무의미한 DB Connection 점유 제거.
  - 스핀 락 방식의 렛투스(Lettuce) 대신 Pub/Sub 기반의 Redisson을 선택하여 레디스 서버의 CPU 부하 최소화.
    
  <img width="718" height="347" alt="스크린샷 2026-03-15 오후 11 39 08" src="https://github.com/user-attachments/assets/1c64f903-8a65-4b06-828d-0bbb13394ecc" />

- **📊 Result:**
  - 트랜잭션과 Lock 획득 순서를 분리하여 단 1개의 Connection으로 입찰 처리.
  - 통합 부하 테스트(K6) 결과, 조회 응답 **1,003ms ➡️ 44ms (95.6% 개선)** 및 전체 TPS 22% 향상. (Connection Pool 30개, 비동기 Task Pool 15개로 제한하여 운영 환경 자원 제약을 재현한 로컬 환경 기준)
- **블로그:** [Redisson Distrubution Lock 도입](https://velog.io/@takch02/리팩토링-프로젝트를-리팩토링-하자-9)

### 3. 300만 건 대용량 데이터 조회 성능 개선 (Covering Index & Deferred Join)
- **🚨 Problem:** 기존 `findAll()` 및 `OFFSET` 기반 Paging에서 COUNT 쿼리 병목과 Full Scan으로 인한 응답 지연.
- **💡 Solution:** No-Offset(Page Slice) 전환으로 COUNT 쿼리 제거. 식별자만 선조회하는 **Covering Index**와 실제 필요한 데이터만 원본에서 빼오는 **Deferred Join** 적용.
- **📊 Result:** 300만 건 더미 데이터 부하 테스트(K6) 기준 조회 응답 **5,115ms ➡️ 31ms (168배 개선)**. (로컬 환경 기준)
- **블로그:** [Covering Index & Deferred Join으로 성능 개선](https://velog.io/@takch02/리팩토링-프로젝트를-리팩토링-하자-5)
