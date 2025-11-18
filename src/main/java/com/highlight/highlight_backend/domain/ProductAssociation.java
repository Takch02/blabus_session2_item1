package com.highlight.highlight_backend.domain;

import com.highlight.highlight_backend.admin.product.domian.Product;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 상품 연관도 엔티티
 * 
 * 두 상품 간의 연관도 점수를 저장하여
 * "함께 본 상품" 추천 기능에서 사용합니다.
 * 
 * 연관도 점수는 다음 요소들을 기반으로 계산됩니다:
 * - 동일 세션에서 함께 조회된 횟수
 * - 동일 사용자가 함께 조회한 횟수  
 * - 시간적 근접성 (같은 시간대에 조회)
 * - 카테고리/브랜드 유사성 가중치
 * 
 * @author 전우선
 * @since 2025.08.18
 */
@Entity
@Table(name = "product_associations", 
       indexes = {
           @Index(name = "idx_source_product_score", columnList = "source_product_id, association_score DESC"),
           @Index(name = "idx_target_product_score", columnList = "target_product_id, association_score DESC")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ProductAssociation {

    /**
     * 연관도 ID (Primary Key)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "association_id")
    private Long id;

    /**
     * 기준 상품 (조회한 상품)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_product_id", nullable = false)
    private Product sourceProduct;

    /**
     * 연관 상품 (함께 조회된 상품)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_product_id", nullable = false)
    private Product targetProduct;

    /**
     * 연관도 점수 (0.0 ~ 100.0)
     * 높을수록 강한 연관성을 의미
     */
    @Column(name = "association_score", precision = 5, scale = 2, nullable = false)
    private BigDecimal associationScore = BigDecimal.ZERO;

    /**
     * 함께 조회된 총 횟수
     */
    @Column(name = "co_view_count", nullable = false)
    private Integer coViewCount = 0;

    /**
     * 동일 세션에서 함께 조회된 횟수
     */
    @Column(name = "same_session_count", nullable = false)
    private Integer sameSessionCount = 0;

    /**
     * 동일 사용자가 함께 조회한 횟수
     */
    @Column(name = "same_user_count", nullable = false)
    private Integer sameUserCount = 0;

    /**
     * 마지막 함께 조회된 시간
     */
    @Column(name = "last_co_viewed_at")
    private LocalDateTime lastCoViewedAt;

    /**
     * 연관도 점수 계산 시간
     */
    @LastModifiedDate
    @Column(name = "score_calculated_at")
    private LocalDateTime scoreCalculatedAt;

    /**
     * 생성 시간
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 생성자 - 새로운 연관도 생성
     */
    public ProductAssociation(Product sourceProduct, Product targetProduct) {
        this.sourceProduct = sourceProduct;
        this.targetProduct = targetProduct;
        this.coViewCount = 1;
        this.lastCoViewedAt = LocalDateTime.now();
    }

    /**
     * 함께 조회 횟수 증가
     */
    public void incrementCoViewCount(boolean isSameSession, boolean isSameUser) {
        this.coViewCount++;
        this.lastCoViewedAt = LocalDateTime.now();
        
        if (isSameSession) {
            this.sameSessionCount++;
        }
        
        if (isSameUser) {
            this.sameUserCount++;
        }
    }

    /**
     * 연관도 점수 업데이트
     * 
     * @param newScore 새로운 연관도 점수
     */
    public void updateAssociationScore(BigDecimal newScore) {
        this.associationScore = newScore;
        this.scoreCalculatedAt = LocalDateTime.now();
    }

    /**
     * 연관도 점수 계산 로직
     * 
     * 점수 = (기본점수 * 가중치) + (세션보너스) + (사용자보너스) + (시간보너스)
     * - 기본점수: 총 함께 조회 횟수
     * - 세션보너스: 같은 세션에서 조회된 횟수 * 2.0
     * - 사용자보너스: 같은 사용자가 조회한 횟수 * 1.5  
     * - 시간보너스: 최근 30일 내 조회 시 추가 점수
     */
    public BigDecimal calculateScore() {
        double baseScore = coViewCount.doubleValue();
        double sessionBonus = sameSessionCount.doubleValue() * 2.0;
        double userBonus = sameUserCount.doubleValue() * 1.5;
        
        // 시간 보너스: 최근 30일 내 조회 시 20% 보너스
        double timeBonus = 0.0;
        if (lastCoViewedAt != null && lastCoViewedAt.isAfter(LocalDateTime.now().minusDays(30))) {
            timeBonus = baseScore * 0.2;
        }
        
        // 카테고리 가중치: 같은 카테고리인 경우 10% 보너스
        double categoryBonus = 0.0;
        if (sourceProduct.getCategory() != null && targetProduct.getCategory() != null &&
            sourceProduct.getCategory().equals(targetProduct.getCategory())) {
            categoryBonus = baseScore * 0.1;
        }
        
        double totalScore = baseScore + sessionBonus + userBonus + timeBonus + categoryBonus;
        
        // 최대 점수 100점으로 제한
        return BigDecimal.valueOf(Math.min(totalScore, 100.0));
    }
}