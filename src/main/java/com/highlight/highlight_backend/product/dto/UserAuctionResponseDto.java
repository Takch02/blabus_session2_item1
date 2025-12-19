package com.highlight.highlight_backend.product.dto;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.product.domian.Product;
import com.highlight.highlight_backend.product.domian.ProductImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 일반 유저에게 썸네일 화면에 보여줄 경매 정보 응답 DTO
 *
 * @author 탁찬홍
 * @since 2025.08.14
 */
@Getter
@Builder // new UserAuctionResponseDto(...) 대신 Builder 패턴을 사용하기 위해 추가
@AllArgsConstructor
public class UserAuctionResponseDto {

    // Product 에서 가져올 정보
    private Long productId;  // 상품 ID
    private Long auctionId;  // 경매 ID
    private String thumbnailUrl; // 썸네일 이미지 URL
    private String productName;  // 상품명
    private String brand; // 브렌드명
    // Auction 에서 가져올 정보
    private BigDecimal startPrice;
    private BigDecimal currentPrice;   // 현재가
    private BigDecimal buyNowPrice;    // 즉시구매가
    private BigDecimal minimumBid;     // 최소 입찰가 (시작가)
    private Long bidCount;      // 입찰 수
    private LocalDateTime endTime; // 종료 시간
    private LocalDateTime startTime;
    private String auctionStatus;  // 현재 경매 상태
    private Boolean isPremium;

    /**
     * Auction 엔티티로부터 UserAuctionResponseDto를 생성합니다.
     * @param auction 원본 Auction 엔티티
     * @return 변환된 DTO
     */
    public static UserAuctionResponseDto from(Auction auction) {
        Product product = auction.getProduct();
        List<ProductImage> images = product.getImages();

        // 썸네일 이미지를 가져옵니다. 이미지가 없으면 null 처리.
        String thumbnailUrl = (images != null && !images.isEmpty()) ? images.get(0).getImageUrl() : null;

        return UserAuctionResponseDto.builder()
                // Product 정보
                .auctionId(auction.getId())
                .productId(product.getId()) 
                .thumbnailUrl(thumbnailUrl)
                .productName(product.getProductName())
                .brand(product.getBrand())
                // Auction 정보
                .startPrice(auction.getStartPrice())
                .buyNowPrice(auction.getBuyItNowPrice())
                .currentPrice(auction.getCurrentHighestBid())
                .minimumBid(auction.getStartPrice())
                .bidCount(auction.getTotalBids())
                .endTime(auction.getScheduledEndTime())
                .startTime(auction.getScheduledStartTime())
                .auctionStatus(auction.getStatus().name()) // Enum 값을 문자열로 변환
                .isPremium(product.getIsPremium())
                .build();
    }
    
    /**
     * Auction 엔티티와 계산된 입찰 수로부터 UserAuctionResponseDto를 생성합니다.
     * 사용자별 최신 입찰 기준으로 정확한 통계를 제공합니다.
     * 
     * @param auction 원본 Auction 엔티티
     * @param calculatedBidCount 실제 계산된 입찰 수 (사용자별 최신 기준)
     * @return 변환된 DTO
     */
    public static UserAuctionResponseDto fromWithCalculatedCount(Auction auction, Long calculatedBidCount) {
        Product product = auction.getProduct();
        List<ProductImage> images = product.getImages();

        // 썸네일 이미지를 가져옵니다. 이미지가 없으면 null 처리.
        String thumbnailUrl = (images != null && !images.isEmpty()) ? images.get(0).getImageUrl() : null;

        return UserAuctionResponseDto.builder()
                // Product 정보
                .auctionId(auction.getId())
                .productId(product.getId()) 
                .thumbnailUrl(thumbnailUrl)
                .productName(product.getProductName())
                .brand(product.getBrand())
                // Auction 정보
                .startPrice(auction.getStartPrice())
                .buyNowPrice(auction.getBuyItNowPrice())
                .currentPrice(auction.getCurrentHighestBid())
                .minimumBid(auction.getStartPrice())
                .bidCount(calculatedBidCount) // 계산된 값 사용
                .endTime(auction.getScheduledEndTime())
                .startTime(auction.getScheduledStartTime())
                .auctionStatus(auction.getStatus().name()) // Enum 값을 문자열로 변환
                .build();
    }
}