package com.highlight.highlight_backend.domain;

import com.highlight.highlight_backend.product.domian.Product;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 사용자 상품 조회 이력 엔티티
 * 
 * 사용자가 상품을 조회한 기록을 저장하여
 * "함께 본 상품" 추천 기능의 기반 데이터로 활용합니다.
 * 
 * @author 전우선
 * @since 2025.08.18
 */
@Entity
@Table(name = "user_product_views")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class UserProductView {

    /**
     * 조회 이력 ID (Primary Key)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "view_id")
    private Long id;

    /**
     * 조회한 사용자 ID (비회원은 null)
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * 세션 ID (비회원 사용자 추적용)
     */
    @Column(name = "session_id", length = 255)
    private String sessionId;

    /**
     * 조회한 상품
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * 조회 시간
     */
    @CreatedDate
    @Column(name = "viewed_at", updatable = false)
    private LocalDateTime viewedAt;

    /**
     * 사용자 IP 주소
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User-Agent 정보
     */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    /**
     * 조회 지속 시간 (초 단위, 선택적)
     */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /**
     * 생성자 - 기본 조회 이력 생성
     */
    public UserProductView(Long userId, String sessionId, Product product, String ipAddress, String userAgent) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.product = product;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

}