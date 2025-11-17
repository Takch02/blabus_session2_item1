package com.highlight.highlight_backend.common.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 유틸리티 클래스 (웹 개발 최적화)
 * 
 * JWT 토큰의 생성, 검증, 파싱 기능을 제공합니다.
 * 웹 환경에 최적화된 토큰 관리 시스템을 구현합니다.
 *
 * - Access Token: 30분 (API 요청용, 짧은 주기로 보안 강화)
 * - Refresh Token: 7일 (재로그인 방지, 적당한 세션 유지)
 * - HttpOnly Cookie와 함께 사용하여 XSS 공격 방지 가능
 * 
 * @author 전우선
 * @since 2025.08.08
 */
@Slf4j
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long accessTokenExpireTime;
    private final long refreshTokenExpireTime;

    /**
     * JwtUtil 생성자
     * 
     * @param secret JWT 서명에 사용할 비밀 키
     * @param accessTokenExpireTime Access Token 만료 시간 (밀리초)
     * @param refreshTokenExpireTime Refresh Token 만료 시간 (밀리초)
     */
    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expire-time}") long accessTokenExpireTime,
            @Value("${jwt.refresh-token-expire-time}") long refreshTokenExpireTime) {
        
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpireTime = accessTokenExpireTime;
        this.refreshTokenExpireTime = refreshTokenExpireTime;
    }

    /**
     * Access Token 생성
     * 
     * @param userId 사용자 ID
     * @param email 사용자 이메일
     * @param role 사용자 권한
     * @return 생성된 Access Token
     */
    public String generateAccessToken(Long userId, String email, String role) {
        return generateToken(userId, email, role, accessTokenExpireTime, "ACCESS");
    }

    /**
     * Refresh Token 생성
     *
     * @param userId 사용자 ID
     * @param email 사용자 이메일
     * @param role 사용자 권한
     * @return 생성된 Refresh Token
     */
    public String generateRefreshToken(Long userId, String email, String role) {
        return generateToken(userId, email, role, refreshTokenExpireTime, "REFRESH");
    }

    /**
     * JWT 토큰 생성 (내부 메서드)
     *
     * @param userId 사용자 ID
     * @param email 사용자 이메일
     * @param role 사용자 권한
     * @param expireTime 만료 시간
     * @param tokenType 토큰 타입
     * @return 생성된 JWT 토큰
     */
    private String generateToken(Long userId, String email, String role, long expireTime, String tokenType) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expireTime);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role)
                .claim("type", tokenType)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * JWT 토큰에서 사용자 권한 추출
     *
     * @param token JWT 토큰
     * @return 사용자 권한
     */
    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    /**
     * JWT 토큰에서 사용자 ID 추출
     * 
     * @param token JWT 토큰
     * @return 사용자 ID
     */
    public Long getUserId(String token) {
        return Long.valueOf(getClaims(token).getSubject());
    }

    /**
     * JWT 토큰에서 사용자 이메일 추출
     * 
     * @param token JWT 토큰
     * @return 사용자 이메일
     */
    public String getEmail(String token) {
        return getClaims(token).get("email", String.class);
    }

    /**
     * JWT 토큰 유효성 검증
     * 
     * @param token 검증할 JWT 토큰
     * @return 유효성 여부
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
        } catch (SecurityException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
        }
        return false;
    }

    /**
     * JWT 토큰이 만료되었는지 확인
     * 
     * @param token JWT 토큰
     * @return 만료 여부
     */
    public boolean isTokenExpired(String token) {
        try {
            return getClaims(token).getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * JWT 토큰에서 Claims 추출
     * 
     * @param token JWT 토큰
     * @return Claims 객체
     */
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     * 
     * @param bearerToken "Bearer {token}" 형식의 문자열
     * @return 추출된 토큰 (Bearer 접두사 제거)
     */
    public String extractTokenFromBearer(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}