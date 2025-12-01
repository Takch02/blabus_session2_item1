package com.highlight.highlight_backend.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;
import com.highlight.highlight_backend.product.domian.Product;

/**
 * 상품 등록 요청 DTO
 *
 */
@AllArgsConstructor
@Builder
@Getter
public class ProductCreateRequestDto {
    
    /**
     * 상품명
     */
    @NotBlank(message = "상품명은 필수입니다")
    @Size(max = 100, message = "상품명은 100자를 초과할 수 없습니다")
    private String productName;
    
    /**
     * 상품 소개 (100자 제한)
     */
    @NotBlank(message = "상품 소개는 필수입니다")
    @Size(max = 100, message = "상품 소개는 100자를 초과할 수 없습니다")
    private String shortDescription;
    
    /**
     * 상품 히스토리
     */
    @Size(max = 2000, message = "히스토리는 2000자를 초과할 수 없습니다")
    private String history;
    
    
    /**
     * 기대효과
     */
    @Size(max = 2000, message = "기대효과는 2000자를 초과할 수 없습니다")
    private String expectedEffects;
    
    /**
     * 상세 정보
     */
    @Size(max = 5000, message = "상세 정보는 5000자를 초과할 수 없습니다")
    private String detailedInfo;

    /**
     * 상세 정보
     */
    @NotNull(message = "프리미엄 여부는 필수입니다")
    private Boolean isPremium;
    
    
    
    /**
     * 카테고리
     */
    @NotNull(message = "카테고리는 필수입니다")
    private Product.Category category;

    /**
     * 상품 갯수
     */
    @NotNull(message = "상품 갯수는 필수입니다")
    @Min(value = 1, message = "상품 갯수는 1개 이상이어야 합니다")
    private Long productCount;

    /**
     * 상품 재질
     */
    @NotBlank(message = "상품 재질은 필수입니다")
    @Size(max = 100, message = "재질은 100자를 초과할 수 없습니다")
    private String material;

    /**
     * 상품 사이즈
     */
    @NotBlank(message = "상품 사이즈는 필수입니다")
    @Size(max = 100, message = "사이즈는 100자를 초과할 수 없습니다")
    private String size;

    /**
     * 브랜드/메이커
     */
    @NotBlank(message = "브랜드는 필수입니다")
    @Size(max = 100, message = "브랜드는 100자를 초과할 수 없습니다")
    private String brand;

    /**
     * 제조년도
     */
    @Min(value = 1800, message = "제조년도는 1800년 이후여야 합니다")
    @Max(value = 2030, message = "제조년도는 2030년 이전이어야 합니다")
    private Integer manufactureYear;

    /**
     * 상품 상태 설명
     */
    @Size(max = 500, message = "상품 상태 설명은 500자를 초과할 수 없습니다")
    private String condition;

    /**
     * 상품 등급
     */
    @NotNull(message = "상품 등급은 필수입니다")
    private Product.ProductRank rank;
    
    /**
     * 상품 이미지 정보 목록
     */
    @Valid
    private List<ProductImageDto> images;
    
    /**
     * 임시저장 여부 (true: 임시저장, false: 활성 상태로 등록)
     */
    private boolean isDraft;


    /**
     * 상품 이미지 DTO
     */
    @Getter
    @AllArgsConstructor
    @Builder
    public static class ProductImageDto {
        /**
         * 이미지 URL
         */
        private String imageUrl;
        
        /**
         * 원본 파일명
         */
        private String originalFileName;
        
        /**
         * 파일 크기
         */
        private Long fileSize;
        
        /**
         * MIME 타입
         */
        private String mimeType;
        
        /**
         * 대표 이미지 여부
         */
        private boolean isPrimary;
        
        /**
         * 정렬 순서
         */
        private Integer sortOrder;
    }
}