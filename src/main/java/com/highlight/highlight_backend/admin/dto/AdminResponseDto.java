package com.highlight.highlight_backend.admin.dto;

import com.highlight.highlight_backend.admin.domain.Admin;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 관리자 정보 응답 DTO
 * 
 * @author 전우선
 * @since 2025.08.13
 */
@Getter
@AllArgsConstructor
public class AdminResponseDto {
    
    /**
     * 관리자 ID (Primary Key)
     */
    private Long id;
    
    /**
     * 관리자 로그인 ID
     */
    private String adminId;
    
    /**
     * 관리자 이름
     */
    private String adminName;
    
    /**
     * 관리자 이메일
     */
    private String email;
    
    /**
     * 관리자 권한
     */
    private Admin.AdminRole role;
    
    /**
     * 계정 활성화 상태
     */
    private boolean isActive;
    
    
    /**
     * 마지막 로그인 시간
     */
    private LocalDateTime lastLoginAt;
    
    /**
     * 생성 시간
     */
    private LocalDateTime createdAt;
    
    /**
     * 수정 시간
     */
    private LocalDateTime updatedAt;
    
    /**
     * Admin 엔티티로부터 DTO 생성
     */
    public static AdminResponseDto from(Admin admin) {
        return new AdminResponseDto(
            admin.getId(),
            admin.getAdminId(),
            admin.getAdminName(),
            admin.getEmail(),
            admin.getRole(),
            admin.isActive(),
            admin.getLastLoginAt(),
            admin.getCreatedAt(),
            admin.getUpdatedAt()
        );
    }
}