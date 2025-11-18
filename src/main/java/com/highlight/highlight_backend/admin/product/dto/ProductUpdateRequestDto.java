package com.highlight.highlight_backend.admin.product.dto;

import com.highlight.highlight_backend.admin.product.domian.Product;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 상품 수정 요청 DTO
 * 
 * @author 전우선
 * @since 2025.08.13
 */
@Getter
@NoArgsConstructor
public class ProductUpdateRequestDto {
    
    /**
     * 상품명
     */
    private String productName;
    
    /**
     * 상품 소개 (50자 제한)
     */
    private String shortDescription;
    
    /**
     * 상품 히스토리
     */
    private String history;
    
    
    /**
     * 기대효과
     */
    private String expectedEffects;
    
    /**
     * 상세 정보
     */
    private String detailedInfo;
    
    
    
    /**
     * 카테고리
     */
    private Product.Category category;

    /**
     * 상품 갯수
     */
    private Long productCount;

    /**
     * 상품 재질
     */
    private String material;

    /**
     * 상품 사이즈
     */
    private String size;

    /**
     * 브랜드/메이커
     */
    private String brand;

    /**
     * 제조년도
     */
    private Integer manufactureYear;

    /**
     * 상품 상태 설명
     */
    private String condition;

    /**
     * 상품 등급
     */
    private Product.ProductRank rank;

    /**
     * 프리미엄 여부
     */
    private Boolean isPremium;
    
    /**
     * 상품 상태
     */
    private Product.ProductStatus status;
    
    /**
     * 상품 이미지 정보 목록 (전체 교체)
     */
    private List<ProductImageDto> images;
    
    /**
     * 상품 이미지 DTO
     */
    @Getter
    @NoArgsConstructor
    public static class ProductImageDto {
        /**
         * 이미지 ID (기존 이미지 수정시)
         */
        private Long id;
        
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
        private boolean isPrimary = false;
        
        /**
         * 정렬 순서
         */
        private Integer sortOrder = 0;
        
        /**
         * 삭제 여부
         */
        private boolean isDeleted = false;
    }
}