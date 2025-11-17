package com.highlight.highlight_backend.common.util;

import org.springframework.security.core.Authentication;

/**
 * 인증 관련 유틸리티 클래스
 * 
 * Spring Security Authentication 객체에서 공통적으로 사용되는 작업들을 처리합니다.
 * 
 * @author 전우선
 * @since 2025.08.16
 */
public class AuthenticationUtils {
    
    /**
     * Authentication 객체에서 사용자 ID를 추출합니다.
     * 
     * @param authentication Spring Security Authentication 객체
     * @return 사용자 ID (Long)
     */
    public static Long extractUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
    
    /**
     * Authentication 객체에서 관리자 ID를 추출합니다.
     * 
     * @param authentication Spring Security Authentication 객체
     * @return 관리자 ID (Long)
     */
    public static Long extractAdminId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}