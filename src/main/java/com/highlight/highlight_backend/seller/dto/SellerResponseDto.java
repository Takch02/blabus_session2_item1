package com.highlight.highlight_backend.seller.dto;

import com.highlight.highlight_backend.seller.domain.Seller;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 판매자 정보 응답 DTO
 * 
 * @author 전우선
 * @since 2025.08.16
 */
@Getter
@Builder
public class SellerResponseDto {
    
    private Long id;
    private String sellerName;
    private String description;
    private String profileImageUrl;
    private String phoneNumber;
    private String email;
    private String address;
    private BigDecimal rating;
    private Long reviewCount;
    private Long salesCount;
    private String status;
    private String businessNumber;
    private LocalDateTime createdAt;
    private LocalDateTime lastActiveAt;
    
    /**
     * Seller 엔티티를 DTO로 변환
     * 
     * @param seller 판매자 엔티티
     * @return SellerResponseDto
     */
    public static SellerResponseDto from(Seller seller) {
        return SellerResponseDto.builder()
            .id(seller.getId())
            .sellerName(seller.getSellerName())
            .description(seller.getDescription())
            .profileImageUrl(seller.getProfileImageUrl())
            .phoneNumber(seller.getPhoneNumber())
            .email(seller.getEmail())
            .address(seller.getAddress())
            .rating(seller.getRating())
            .reviewCount(seller.getReviewCount())
            .salesCount(seller.getSalesCount())
            .status(seller.getStatus().getDescription())
            .businessNumber(seller.getBusinessNumber())
            .createdAt(seller.getCreatedAt())
            .lastActiveAt(seller.getLastActiveAt())
            .build();
    }
    
    /**
     * 간단한 판매자 정보만 포함하는 DTO 생성
     * (상품 목록에서 사용)
     * 
     * @param seller 판매자 엔티티
     * @return 간단한 판매자 정보 DTO
     */
    public static SellerResponseDto simple(Seller seller) {
        return SellerResponseDto.builder()
            .id(seller.getId())
            .sellerName(seller.getSellerName())
            .profileImageUrl(seller.getProfileImageUrl())
            .rating(seller.getRating())
            .reviewCount(seller.getReviewCount())
            .salesCount(seller.getSalesCount())
            .build();
    }
}