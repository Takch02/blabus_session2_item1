package com.highlight.highlight_backend.seller.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 판매자 엔티티
 * 
 * 경매 플랫폼의 실제 상품 판매자 정보를 저장하는 엔티티입니다.
 * 
 * @author 전우선
 * @since 2025.08.16
 */
@Entity
@Table(name = "seller")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Seller {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 판매자 이름/상호명
     */
    @Column(nullable = false, length = 100)
    private String sellerName;
    
    /**
     * 판매자 소개글
     */
    @Column(columnDefinition = "TEXT")
    private String description;
    
    /**
     * 판매자 프로필 이미지 URL
     */
    @Column(length = 500)
    private String profileImageUrl;
    
    /**
     * 연락처
     */
    @Column(length = 20)
    private String phoneNumber;
    
    /**
     * 이메일
     */
    @Column(length = 100)
    private String email;
    
    /**
     * 주소
     */
    @Column(length = 200)
    private String address;
    
    /**
     * 판매자 평점 (0.0 ~ 5.0)
     */
    @Column(precision = 3, scale = 2)
    private BigDecimal rating = BigDecimal.ZERO;
    
    /**
     * 총 리뷰 수
     */
    @Column(nullable = false)
    private Long reviewCount = 0L;
    
    /**
     * 총 판매 건수
     */
    @Column(nullable = false)
    private Long salesCount = 0L;
    
    /**
     * 판매자 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SellerStatus status = SellerStatus.ACTIVE;
    
    /**
     * 사업자 등록번호 (선택)
     */
    @Column(length = 20)
    private String businessNumber;
    
    /**
     * 계정 생성 시간
     */
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 마지막 수정 시간
     */
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    /**
     * 마지막 활동 시간
     */
    private LocalDateTime lastActiveAt;
    
    /**
     * 판매자 상태 열거형
     */
    public enum SellerStatus {
        ACTIVE("활성"),
        INACTIVE("비활성"),
        SUSPENDED("정지"),
        PENDING("승인대기");
        
        private final String description;
        
        SellerStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 평점 업데이트
     * 
     * @param newRating 새로운 평점
     */
    public void updateRating(BigDecimal newRating) {
        this.rating = newRating;
        this.reviewCount++;
    }
    
    /**
     * 판매 건수 증가
     */
    public void incrementSalesCount() {
        this.salesCount++;
    }
    
    /**
     * 마지막 활동 시간 업데이트
     */
    public void updateLastActiveAt() {
        this.lastActiveAt = LocalDateTime.now();
    }
}