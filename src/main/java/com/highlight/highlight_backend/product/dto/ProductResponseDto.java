package com.highlight.highlight_backend.product.dto;

import com.highlight.highlight_backend.product.domian.Product;
import com.highlight.highlight_backend.product.domian.ProductImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 상품 응답 DTO
 * 
 * @author 전우선
 * @since 2025.08.13
 */
@Getter
@AllArgsConstructor
@Builder
public class ProductResponseDto {
    
    /**
     * 상품 ID
     */
    private Long id;
    
    /**
     * 상품명
     */
    private String productName;
    
    /**
     * 상품 소개 (25자 제한)
     */
    private String shortDescription;
    
    /**
     * 상품 히스토리
     */
    private String history;

    /**
     * 사이즈
     */
    private String size;

    /**
     * 갯수
     */
    private Long productCount;

    /**
     * 재질
     */
    private String material;

    /**
     * 생산년도
     */
    private Integer manufactureYear;

    /**
     * 브랜드 명
     */
    private String brand;
    /**
     * 기본 정보
     */
    //private String basicInfo;
    
    /**
     * 기대효과
     */
    private String expectedEffects;
    
    /**
     * 상세 정보
     */
    private String detailedInfo;
    
    
    /**
     * 입장료
     */
    //private BigDecimal entranceFee;
    
    /**
     * 상품 상태
     */
    private Product.ProductStatus status;
    
    /**
     * 상품 상태 설명
     */
    private String statusDescription;
    
    /**
     * 카테고리
     */
    private Product.Category category;
    
    /**
     * 등록한 관리자 ID
     */
    private Long registeredBy;
    
    /**
     * 상품 이미지 목록
     */
    private List<ProductImageResponseDto> images;
    
    /**
     * 대표 이미지 URL
     */
    private String primaryImageUrl;
    
    /**
     * 프리미엄 상품 여부
     */
    private Boolean isPremium;
    
    /**
     * 생성 시간
     */
    private LocalDateTime createdAt;
    
    /**
     * 수정 시간
     */
    private LocalDateTime updatedAt;
    
    /**
     * Product 엔티티로부터 DTO 생성
     */
    public static ProductResponseDto from(Product product) {
        // 이미지 리스트 변환 로직 (기존과 동일)
        List<ProductImageResponseDto> imageDtos = product.getImages().stream()
                .map(ProductImageResponseDto::from)
                .collect(Collectors.toList());

        // 대표 이미지 URL 추출 로직 (기존과 동일)
        String primaryImageUrl = product.getPrimaryImage() != null ?
                product.getPrimaryImage().getImageUrl() : null;

        // 빌더 패턴을 사용하여 DTO 객체 생성
        return ProductResponseDto.builder()
                .id(product.getId())
                .productName(product.getProductName())
                .shortDescription(product.getShortDescription())
                .history(product.getHistory())
                .size(product.getSize())
                .productCount(product.getProductCount())
                .material(product.getMaterial())
                .manufactureYear(product.getManufactureYear())
                .brand(product.getBrand())
                .expectedEffects(product.getExpectedEffects())
                .detailedInfo(product.getDetailedInfo())
                .status(product.getStatus())
                .statusDescription(product.getStatus().getDescription())
                .category(product.getCategory())
                .registeredBy(product.getRegisteredBy())
                .images(imageDtos)
                .primaryImageUrl(primaryImageUrl)
                .isPremium(product.getIsPremium())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
    
    /**
     * 상품 이미지 응답 DTO
     */
    @Getter
    @AllArgsConstructor
    public static class ProductImageResponseDto {
        /**
         * 이미지 ID
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
        private boolean isPrimary;
        
        /**
         * 정렬 순서
         */
        private Integer sortOrder;
        
        /**
         * 생성 시간
         */
        private LocalDateTime createdAt;
        
        /**
         * ProductImage 엔티티로부터 DTO 생성
         */
        public static ProductImageResponseDto from(ProductImage image) {
            return new ProductImageResponseDto(
                image.getId(),
                image.getImageUrl(),
                image.getOriginalFileName(),
                image.getFileSize(),
                image.getMimeType(),
                image.isPrimary(),
                image.getSortOrder(),
                image.getCreatedAt()
            );
        }
    }
}