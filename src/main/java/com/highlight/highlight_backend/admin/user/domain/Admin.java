package com.highlight.highlight_backend.admin.user.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 관리자 엔티티
 * 
 * nafal 백오피스 시스템의 관리자 정보를 저장하는 엔티티입니다.
 * 
 * @author 전우선
 * @since 2025.08.13
 */
@Entity
@Table(name = "admin")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Admin {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 관리자 로그인 ID (고유값)
     */
    @Column(nullable = false, unique = true, length = 50)
    private String adminId;
    
    /**
     * 관리자 비밀번호 (암호화 저장)
     */
    @Column(nullable = false)
    private String password;
    
    /**
     * 관리자 이름
     */
    @Column(nullable = false, length = 30)
    private String adminName;
    
    /**
     * 관리자 이메일
     */
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    /**
     * 관리자 권한 (SUPER_ADMIN, ADMIN)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdminRole role = AdminRole.ADMIN;
    
    
    /**
     * 계정 활성화 상태
     */
    @Column(nullable = false)
    private boolean isActive = true;
    
    /**
     * 상품 관리 권한
     */
    @Column(nullable = false)
    private boolean canManageProducts = false;
    
    /**
     * 경매 관리 권한
     */
    @Column(nullable = false)
    private boolean canManageAuctions = false;
    
    /**
     * 결제 관리 권한
     */
    @Column(nullable = false)
    private boolean canManagePayments = false;
    
    /**
     * 배송 관리 권한
     */
    @Column(nullable = false)
    private boolean canManageShipping = false;
    
    /**
     * 경매 결과 관리 권한
     */
    @Column(nullable = false)
    private boolean canManageAuctionResults = false;
    
    /**
     * 문의 관리 권한
     */
    @Column(nullable = false)
    private boolean canManageInquiries = false;

    /**
     * 마지막 로그인 시간
     */
    private LocalDateTime lastLoginAt;
    
    /**
     * 생성 시간
     */
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 수정 시간
     */
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    /**
     * 관리자 권한 열거형
     */
    public enum AdminRole {
        SUPER_ADMIN("슈퍼 관리자"),
        ADMIN("일반 관리자");
        
        private final String description;
        
        AdminRole(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}