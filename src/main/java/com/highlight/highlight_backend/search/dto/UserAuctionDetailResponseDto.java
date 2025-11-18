package com.highlight.highlight_backend.search.dto;

import com.highlight.highlight_backend.domain.Auction;
import com.highlight.highlight_backend.domain.Product;
import com.highlight.highlight_backend.domain.Seller;
import com.highlight.highlight_backend.dto.ProductResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 일반 User에게 제공하는 경매 상품 상세 페이지 정보
 * (계층형 구조로 리팩토링됨)
 *
 *  JSON 형식 예시
 * {
 *   "auctionId": 100,
 *   "currentHighestBid": 50000,
 *   "scheduledEndTime": "2025-08-20T12:00:00",
 *
 *   "product": {
 *     "productName": "나이키 한정판",
 *     "brand": "Nike",
 *     "images": [ ... ],
 *     "status": "S급"
 *   },
 *
 *   "seller": {
 *     "sellerName": "탁찬홍",
 *     "sellerRating": 4.9
 *   }
 * }
 *
 */
@Getter
@Builder
@AllArgsConstructor
public class UserAuctionDetailResponseDto {


    // 1. 경매(Auction) 진행 정보 (Root Level)

    private Long auctionId;
    private LocalDateTime scheduledStartTime; // 시작 시간
    private LocalDateTime scheduledEndTime;   // 종료 시간

    private BigDecimal startPrice;        // 시작가
    private BigDecimal currentHighestBid; // 현재가
    private BigDecimal buyItNowPrice;     // 즉시구매가
    private BigDecimal maxBid;            // 최대 입찰가 (시스템용이면 제외 가능)
    private BigDecimal minimumBid;        // 최소 입찰 단위

    @Setter
    private BigDecimal point;      // 적립 예정 포인트
    private Long productCount;     // 남은 수량? (문맥에 따라 ProductInfo로 이동 가능)

    // 2. 상품(Product) 상세 정보
    private ProductInfo product;

    // 3. 판매자(Seller) 정보
    private SellerInfo seller;

    /**
     * Auction 엔티티를 DTO로 변환하는 메인 팩토리 메서드
     */
    public static UserAuctionDetailResponseDto from(Auction auction) {
        Product productEntity = auction.getProduct();
        Seller sellerEntity = productEntity.getSeller();

        return UserAuctionDetailResponseDto.builder()
                .auctionId(auction.getId())
                .scheduledStartTime(auction.getScheduledStartTime())
                .scheduledEndTime(auction.getScheduledEndTime())
                .startPrice(auction.getStartPrice())
                .currentHighestBid(auction.getCurrentHighestBid())
                .buyItNowPrice(auction.getBuyItNowPrice())
                .maxBid(auction.getMaxBid())
                .minimumBid(auction.getMinimumBid())
                .productCount(productEntity.getProductCount())
                // 상품 상세정보 class 로 따로 정의
                .product(ProductInfo.from(productEntity))
                // Seller 정보 class 로 따로 정의
                .seller(sellerEntity != null ? SellerInfo.from(sellerEntity) : null)
                .build();
    }


    // 상품 상세정보 DTO
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ProductInfo {
        private String productName;
        private String brand;
        private String category;
        private String shortDescription;
        private String detailedInfo;
        private String history;
        private String expectedEffects;
        private String status;      // 상품 상태 (S급 등)
        private String rank;        // 등급
        private String material;    // 소재
        private String size;
        private String condition;
        private Integer manufactureYear;
        private Boolean isPremium;
        private List<ProductResponseDto.ProductImageResponseDto> images;

        public static ProductInfo from(Product product) {
            // 이미지 변환 로직
            List<ProductResponseDto.ProductImageResponseDto> imageDtos = product.getImages().stream()
                    .map(ProductResponseDto.ProductImageResponseDto::from)
                    .collect(Collectors.toList());

            return ProductInfo.builder()
                    .productName(product.getProductName())
                    .brand(product.getBrand())
                    .category(product.getCategory().getDisplayName()) // Enum 처리 가정
                    .shortDescription(product.getShortDescription())
                    .detailedInfo(product.getDetailedInfo())
                    .history(product.getHistory())
                    .expectedEffects(product.getExpectedEffects())
                    .status(product.getStatus().getDescription())
                    .rank(product.getRank().getDescription())
                    .material(product.getMaterial())
                    .size(product.getSize())
                    .condition(product.getCondition())
                    .manufactureYear(product.getManufactureYear())
                    .isPremium(product.getIsPremium())
                    .images(imageDtos)
                    .build();
        }
    }

    // 판매자정보 DTO
    @Getter
    @Builder
    @AllArgsConstructor
    public static class SellerInfo {
        private String sellerName;
        private String sellerDescription;
        private String sellerProfileImageUrl;
        private String sellerPhoneNumber;
        private String sellerEmail;
        private String sellerAddress;
        private BigDecimal sellerRating;

        public static SellerInfo from(Seller seller) {
            return SellerInfo.builder()
                    .sellerName(seller.getSellerName())
                    .sellerDescription(seller.getDescription())
                    .sellerProfileImageUrl(seller.getProfileImageUrl())
                    .sellerPhoneNumber(seller.getPhoneNumber())
                    .sellerEmail(seller.getEmail())
                    .sellerAddress(seller.getAddress())
                    .sellerRating(seller.getRating())
                    .build();
        }
    }
}