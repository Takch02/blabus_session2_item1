package com.highlight.highlight_backend.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 로그인 응답 DTO
 * 
 * 로그인 성공시 JWT 토큰과 관리자 정보를 반환하는 클래스입니다.
 * 
 * @author 전우선
 * @since 2025.08.13
 */
@Getter
@AllArgsConstructor
public class LoginResponseDto {
    
    /**
     * JWT Access Token
     */
    private String accessToken;
    
    /**
     * JWT Refresh Token
     */
    private String refreshToken;
    
    /**
     * 관리자 ID
     */
    private String adminId;
    
    /**
     * 관리자 이름
     */
    private String adminName;
    
    /**
     * 로그인 성공 메시지
     */
    private String message;
}