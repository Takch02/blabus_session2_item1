# 1단계: Build stage
FROM gradle:8.4.0-jdk17 AS builder
WORKDIR /app

# Gradle 캐시를 재사용하기 위해 gradle 관련 파일 먼저 복사
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
RUN gradle build --no-daemon || return 0  # 의존성 캐시 생성용

# 전체 프로젝트 복사 후 빌드
COPY . .
RUN gradle clean build -x test --no-daemon

# 2단계: Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN mkdir -p /app/logs

# build/libs 디렉토리에서 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]