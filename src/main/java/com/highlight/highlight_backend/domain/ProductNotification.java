package com.highlight.highlight_backend.domain;

import com.highlight.highlight_backend.user.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 상품 알림 설정 엔티티
 * 
 * 사용자가 특정 상품에 대한 알림을 설정한 정보를 저장합니다.
 * 
 * @author 전우선
 * @since 2025.08.16
 */
@Entity
@Table(name = "product_notification", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"}))
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ProductNotification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 알림을 설정한 사용자 ID
     */
    @Column(nullable = false)
    private Long userId;
    
    /**
     * 알림 대상 상품 ID  
     */
    @Column(nullable = false)
    private Long productId;
    
    /**
     * 알림 활성화 여부
     */
    @Column(nullable = false)
    private boolean isActive = true;
    
    /**
     * 알림 유형
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType notificationType = NotificationType.BID_UPDATE;
    
    /**
     * 마지막 알림 발송 시간
     */
    private LocalDateTime lastNotifiedAt;
    
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
     * 상품 정보 (Lazy Loading)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productId", insertable = false, updatable = false)
    private Product product;
    
    /**
     * 사용자 정보 (Lazy Loading)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", insertable = false, updatable = false)
    private User user;
    
    /**
     * 알림 유형 열거형
     */
    public enum NotificationType {
        BID_UPDATE("입찰 현황 알림"),
        AUCTION_END("경매 종료 알림"),
        PRICE_DROP("가격 하락 알림"),
        AUCTION_START("경매 시작 알림");
        
        private final String description;
        
        NotificationType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 알림 설정 생성자
     * 
     * @param userId 사용자 ID
     * @param productId 상품 ID
     * @param notificationType 알림 유형
     */
    public ProductNotification(Long userId, Long productId, NotificationType notificationType) {
        this.userId = userId;
        this.productId = productId;
        this.notificationType = notificationType;
        this.isActive = true;
    }
    
    /**
     * 알림 활성화/비활성화 토글
     */
    public void toggleActive() {
        this.isActive = !this.isActive;
    }
    
    /**
     * 마지막 알림 발송 시간 업데이트
     */
    public void updateLastNotifiedAt() {
        this.lastNotifiedAt = LocalDateTime.now();
    }
}