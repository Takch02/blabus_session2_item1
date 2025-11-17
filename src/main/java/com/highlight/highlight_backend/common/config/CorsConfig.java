package com.highlight.highlight_backend.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * CORS 설정
 * 
 * Cross-Origin Resource Sharing 정책을 정의합니다.
 * 프론트엔드와 백엔드 간의 교차 도메인 요청을 허용합니다.
 * 
 * @author 전우선
 * @since 2025.08.08
 */
@Configuration
public class CorsConfig {

    /**
     * CORS 설정 구성
     * 
     * @return CorsConfigurationSource
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 허용할 Origin
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:3000",        // React 개발 서버
            "http://localhost:80",          // 로컬 백엔드 (표준 포트)
            "http://127.0.0.1:3000",        // 로컬호스트 별칭
            "http://ec2-52-78-128-131.ap-northeast-2.compute.amazonaws.com:3000",  // frontend 배포 도메인
            "http://ec2-52-78-128-131.ap-northeast-2.compute.amazonaws.com",        // frontend 배포 도메인 (포트 없이)
            "http://ec2-52-78-128-131.ap-northeast-2.compute.amazonaws.com:8085",   // 백엔드 서버 (Swagger UI)
            "http://ec2-52-78-128-131.ap-northeast-2.compute.amazonaws.com:3306",        // DB서버 배포
            "http://ec2-52-78-128-131.ap-northeast-2.compute.amazonaws.com",        // AWS 배포
            "http://*.execute-api.*.amazonaws.com"  // API Gateway (HTTP만)
        ));
        
        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        
        // 허용할 헤더
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "Sec-WebSocket-Key",           // WebSocket 핸드셰이크
            "Sec-WebSocket-Version",       // WebSocket 버전
            "Sec-WebSocket-Protocol",      // WebSocket 프로토콜 (STOMP)
            "Sec-WebSocket-Extensions"     // WebSocket 확장
        ));
        
        // 노출할 헤더
        configuration.setExposedHeaders(Arrays.asList(
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials"
        ));
        
        // 인증 정보 포함 허용
        configuration.setAllowCredentials(true);
        
        // 브라우저 캐시 시간 (초)
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}