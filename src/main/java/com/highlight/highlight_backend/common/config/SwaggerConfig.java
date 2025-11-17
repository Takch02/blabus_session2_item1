package com.highlight.highlight_backend.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(buildApiInfo())
                .addSecurityItem(buildSecurityRequirement())
                .components(buildComponents())
                .servers(buildServers());
    }
    
    /**
     * API 기본 정보 구성
     */
    private Info buildApiInfo() {
        return new Info()
                .title("Highlight 경매 플랫폼 API")
                .description(buildApiDescription())
                .version("v1.0.0")
                .contact(buildContactInfo())
                .license(buildLicenseInfo());
    }
    
    /**
     * API 상세 설명 구성
     */
    private String buildApiDescription() {
        return """
                ## 블레이버스 해커톤 2025 - Highlight 팀
                
                **실시간 경매 플랫폼의 백엔드 API 서비스입니다.**
                
                ### 주요 기능
                - **실시간 경매**: WebSocket을 통한 실시간 입찰
                - **즉시구매**: 빠른 상품 구매
                - **사용자 관리**: 회원가입, 로그인, 마이페이지
                - **관리자 기능**: 상품/경매 관리, 계정 관리
                - **부가 기능**: 찜하기, 알림 설정
                
                ### 인증 방법
                1. **사용자**: `/api/public/login` 
                2. **관리자**: `/api/admin/login`
                3. 응답으로 받은 JWT 토큰을 우측 **Authorize**에 입력
                
                ### 실시간 통신
                - **WebSocket**: `/ws` 엔드포인트로 연결
                - **구독**: `/topic/auction/{auctionId}` (경매별 실시간 정보)
                
                ### 문의사항
                개발팀에게 언제든 연락주세요.
                """;
    }
    
    /**
     * 연락처 정보 구성
     */
    private Contact buildContactInfo() {
        return new Contact()
                .name("Highlight Backend Team")
                .email("wooxexn@gmail.com")
                .url("https://github.com/blaybus-highlight");
    }
    
    /**
     * 라이센스 정보 구성
     */
    private License buildLicenseInfo() {
        return new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");
    }
    
    /**
     * 보안 요구사항 구성
     */
    private SecurityRequirement buildSecurityRequirement() {
        return new SecurityRequirement().addList("bearerAuth");
    }
    
    /**
     * API 컴포넌트 구성 (보안 스키마 등)
     */
    private Components buildComponents() {
        return new Components()
                .addSecuritySchemes("bearerAuth", buildJwtSecurityScheme());
    }
    
    /**
     * JWT 보안 스키마 구성
     */
    private SecurityScheme buildJwtSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization")
                .description("JWT 토큰을 입력하세요. Bearer 접두사는 자동으로 추가됩니다.");
    }
    
    /**
     * API 서버 정보 구성
     */
    private List<Server> buildServers() {
        return Arrays.asList(
                new Server()
                        .url("http://ec2-52-78-128-131.ap-northeast-2.compute.amazonaws.com:8085")
                        .description("Production Server (현재 활성화!!!)"),
                new Server()
                        .url("http://localhost:8085")
                        .description("Local Development Server")
        );
    }
}