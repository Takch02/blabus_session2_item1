package com.highlight.highlight_backend.common.config;


import com.highlight.highlight_backend.common.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정
 * 
 * 애플리케이션의 보안 정책을 정의합니다.
 * JWT 기반 인증, CORS 설정, 엔드포인트 접근 권한을 구성합니다.
 * 
 * @author 전우선
 * @since 2025.08.08
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Spring Security 필터 체인 설정
     * 
     * @param http HttpSecurity 객체
     * @return SecurityFilterChain
     * @throws Exception 설정 중 발생할 수 있는 예외
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF 보호 비활성화 (JWT 사용하므로)
                .csrf(AbstractHttpConfigurer::disable)
                
                // CORS 설정 적용 (별도 CorsConfig에서 관리)
                .cors(cors -> {})
                
                // 보안 헤더 설정
                .headers(headers -> headers
                    .frameOptions(frameOptions -> frameOptions.deny())  // X-Frame-Options: DENY
                    .contentTypeOptions(contentTypeOptions -> {})  // X-Content-Type-Options: nosniff
                    .httpStrictTransportSecurity(hsts -> hsts
                        .maxAgeInSeconds(31536000)  // 1년
                        .includeSubDomains(true)
                    )
                )
                
                // 세션 사용하지 않음 (JWT 사용)
                .sessionManagement(session -> 
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // 엔드포인트 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                    // 공개 엔드포인트 (인증 불필요) - 구체적인 경로를 먼저 처리
                    .requestMatchers(
                        "/api/admin-auth/**",              // 관리자 인증 관련 API
                        "/api/admin/signup",               // 관리자 회원가입
                        "/api/admin/login",               // 관리자 회원가입
                        "/api/public/**",                  // 공개 API (사용자 인증, 경매 목록, 판매자 정보 등)
                        "/api/auctions/*/bids",            // 경매 입찰 내역 조회 (익명)
                        "/api/auctions/*/status",          // 실시간 경매 상태 조회
                        "/api/admin/products/*/recommendations", // 관련 상품 추천
                        "/ws/**", "/topic/**", "/queue/**", "/app/**", // WebSocket 관련 (STOMP 포함)
                        "/swagger-ui/**", "/api-docs/**", "/v3/api-docs/**", // API 문서
                        "/swagger-ui.html", "/webjars/**", // Swagger UI 리소스
                        "/error"                           // 에러 페이지
                    ).permitAll()
                    
                    // 사용자 인증 필요 엔드포인트
                    .requestMatchers(
                        "/api/bids",                       // 입찰 참여
                        "/api/users/**",                   // 사용자 개인 정보 관련 API
                        "/api/auctions/*/bids/with-user",  // 경매 입찰 내역 (본인 강조)
                        "/api/auctions/*/my-result",       // 경매에서 내 결과 조회
                        "/api/user/**"                     // 사용자 마이페이지, 찜하기, 알림, 경매 참여 등
                    ).authenticated()
                    
                    // 관리자 권한 필요 엔드포인트
                    .requestMatchers(
                        "/api/admin/admin-management/**",  // 관리자 계정 관리
                        "/api/admin/products/**",          // 상품 관리
                        "/api/admin/auctions/**"           // 경매 관리
                    ).hasAnyRole("ADMIN", "SUPER_ADMIN")
                    
                    // 그 외 모든 요청은 인증 필요
                    .anyRequest().authenticated()
                )
                
                // 기본 로그인 폼 비활성화
                .formLogin(AbstractHttpConfigurer::disable)
                
                // HTTP Basic 인증 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)
                
                // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 이전에 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                
                .build();
    }

    /**
     * 비밀번호 암호화를 위한 BCrypt 인코더
     * 
     * @return PasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}