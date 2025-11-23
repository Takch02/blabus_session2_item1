package com.highlight.highlight_backend.product.wishlist.dto;

import com.highlight.highlight_backend.product.wishlist.domian.ProductWishlist;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 상품 찜하기 응답 DTO
 * 
 * @author 전우선
 * @since 2025.08.16
 */
@Getter
@Builder
public class ProductWishlistResponseDto {
    
    private Long id;
    private Long userId;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private String status;
    private boolean isWishlisted;
    private LocalDateTime createdAt;
    
    /**
     * ProductWishlist 엔티티를 DTO로 변환
     * 
     * @param wishlist 찜하기 엔티티
     * @return ProductWishlistResponseDto
     */
    public static ProductWishlistResponseDto from(ProductWishlist wishlist) {
        return ProductWishlistResponseDto.builder()
            .id(wishlist.getId())
            .userId(wishlist.getUserId())
            .productId(wishlist.getProductId())
            .productName(wishlist.getProduct() != null ? wishlist.getProduct().getProductName() : null)
            .productImageUrl(wishlist.getProduct() != null && wishlist.getProduct().getPrimaryImage() != null ? 
                wishlist.getProduct().getPrimaryImage().getImageUrl() : null)
            .status(wishlist.getProduct() != null ? wishlist.getProduct().getStatus().getDescription() : null)
            .isWishlisted(true)
            .createdAt(wishlist.getCreatedAt())
            .build();
    }
    
    /**
     * 간단한 찜하기 상태만 포함하는 DTO 생성
     * (상품 목록에서 찜하기 상태 표시용)
     * 
     * @param productId 상품 ID
     * @param isWishlisted 찜하기 여부
     * @return 간단한 찜하기 상태 DTO
     */
    public static ProductWishlistResponseDto simple(Long productId, boolean isWishlisted) {
        return ProductWishlistResponseDto.builder()
            .productId(productId)
            .isWishlisted(isWishlisted)
            .build();
    }
}