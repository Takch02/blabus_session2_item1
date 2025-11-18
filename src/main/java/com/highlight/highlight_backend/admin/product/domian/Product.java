package com.highlight.highlight_backend.admin.product.domian;

import com.highlight.highlight_backend.domain.Seller;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 상품 엔티티
 * 
 * nafal 경매 시스템의 상품 정보를 저장하는 엔티티입니다.
 * 
 * @author 전우선
 * @since 2025.08.13
 */
@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 상품명
     */
    @Column(nullable = false, length = 100)
    private String productName;
    
    /**
     * 상품 소개 (25자 제한)
     */
    @Column(nullable = false, length = 100)
    private String shortDescription;
    
    /**
     * 상품 히스토리
     */
    @Column(columnDefinition = "TEXT")
    private String history;

    /**
     * 기대효과
     */
    @Column(columnDefinition = "TEXT")
    private String expectedEffects;
    
    /**
     * 상세 정보
     */
    @Column(columnDefinition = "TEXT")
    private String detailedInfo;
    
    
    /**
     * 상품 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status = ProductStatus.DRAFT;

    /**
     * 현재 상품 상태 ex. 최상, 상, 중 (ENUM class 로 만듦)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "`ProductRank`", nullable = false)
    private ProductRank rank;
    /**
     * 카테고리 (ENUM class 로 만듦)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    /**
     * 상품 갯수
     */
    @Column(nullable = false)
    private Long productCount;

    /**
     * 상품 제질
     */
    @Column(nullable = false)
    private String material;

    /**
     * 상품 사이즈 -> 100 x 100 형식
     */
    @Column(nullable = false)
    private String size;

    /**
     * 브랜드/메이커
     */
    @Column(nullable = false, length = 100)
    private String brand;

    /**
     * 제조년도
     */
    @Column
    private Integer manufactureYear;

    /**
     * 상품 상태 설명
     */
    @Column(columnDefinition = "TEXT", name = "`condition`")
    private String condition;

    /**
     * 등록한 관리자 ID
     */
    @Column(nullable = false)
    private Long registeredBy;

    /**
     * 판매자 ID
     */
    @Column(nullable = false)
    private Long sellerId;
    
    /**
     * 프리미엄 상품 여부
     */
    @Column(nullable = false)
    private Boolean isPremium = false;

    /**
     * 판매자 정보 (Lazy Loading)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sellerId", insertable = false, updatable = false)
    private Seller seller;
    
    /**
     * 상품 이미지 목록
     */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProductImage> images = new ArrayList<>();
    
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
     * 상품 상태 열거형
     */
    public enum ProductStatus {
        DRAFT("임시저장"),
        ACTIVE("활성"),
        INACTIVE("비활성"),
        AUCTION_READY("경매대기"),
        IN_AUCTION("경매중"),
        AUCTION_COMPLETED("경매완료");
        
        private final String description;
        
        ProductStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }

    /**
     * 현재 상품의 등급
     */
    @Getter
    public enum ProductRank {
        BEST("최상"),
        GREAT("상"),
        GOOD("중"),;
        private final String description;

        ProductRank(String description) {
            this.description = description;
        }

    }

    /**
     * 상품의 카테고리 // 추가
     */
    @Getter
    public enum Category {
        PROPS("소품"),
        FURNITURE("가구"),
        HOME_APPLIANCES("가전"),
        SCULPTURE("조형"),
        FASHION("패션"),
        CERAMICS("도예"),
        PAINTING("회화");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

    }
    
    /**
     * 상품 이미지 추가
     */
    public void addImage(ProductImage image) {
        images.add(image);
        image.setProduct(this);
    }
    
    /**
     * 상품 이미지 제거
     */
    public void removeImage(ProductImage image) {
        images.remove(image);
        image.setProduct(null);
    }
    
    /**
     * 대표 이미지 조회
     */
    public ProductImage getPrimaryImage() {
        return images.stream()
            .filter(ProductImage::isPrimary)
            .findFirst()
            .orElse(images.isEmpty() ? null : images.get(0));
    }
    
    /**
     * 대표 이미지 URL 조회
     */
    public String getMainImageUrl() {
        ProductImage primaryImage = getPrimaryImage();
        return primaryImage != null ? primaryImage.getImageUrl() : null;
    }
}