package com.highlight.highlight_backend.dto;

import com.highlight.highlight_backend.admin.auction.domain.Auction;
import com.highlight.highlight_backend.admin.product.domian.Product;
import com.highlight.highlight_backend.domain.ProductAssociation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 함께 본 상품 응답 DTO
 * 
 * ProductCard와 동일한 구조로 설계되어
 * 프론트엔드에서 기존 상품 카드 컴포넌트를 재사용할 수 있습니다.
 * 
 * @author 전우선  
 * @since 2025.08.18
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "함께 본 상품 응답 DTO")
public class ViewTogetherProductResponseDto {

    /**
     * 상품 ID
     */
    @Schema(description = "상품 ID", example = "1")
    private Long productId;

    /**
     * 경매 ID
     */
    @Schema(description = "경매 ID", example = "1")
    private Long auctionId;
    
    /**
     * 상품명
     */
    @Schema(description = "상품명", example = "빈티지 카펫")
    private String productName;

    /**
     * 상품 카테고리
     */
    @Schema(description = "카테고리", example = "FURNITURE")
    private String category;

    /**
     * 메인 이미지 URL
     */
    @Schema(description = "메인 이미지 URL", example = "https://example.com/image.jpg")
    private String primaryImageUrl;

    /**
     * 시작 가격
     */
    @Schema(description = "현재 가격", example = "50000")
    private BigDecimal currentHighestBid;

    /**
     * 즉시 구매 가격
     */
    @Schema(description = "즉시 구매 가격", example = "100000")
    private BigDecimal buyNowPrice;

    /**
     * 브랜드명
     */
    @Schema(description = "브랜드명", example = "NAFAL")
    private String brand;

    /**
     * 경매 상태
     */
    @Schema(description = "경매 상태", example = "IN_PROGRESS", 
            allowableValues = {"SCHEDULED", "IN_PROGRESS", "ENDED"})
    private String auctionStatus;

    /**
     * 경매 종료 시간
     */
    @Schema(description = "경매 종료 시간", example = "2025-08-20T18:00:00")
    private LocalDateTime endTime;

    /**
     * 입찰 횟수
     */
    @Schema(description = "입찰 횟수", example = "15")
    private Integer bidCount;

    /**
     * 연관도 점수 (정렬용)
     */
    @Schema(description = "연관도 점수", example = "87.5")
    private BigDecimal associationScore;

    /**
     * ProductAssociation으로부터 DTO 생성
     * 
     * @param association 상품 연관도 엔티티
     * @return ViewTogetherProductResponseDto
     */
    public static ViewTogetherProductResponseDto fromAssociation(ProductAssociation association) {
        Product targetProduct = association.getTargetProduct();
        Auction activeAuction = getActiveAuction(targetProduct);

        return ViewTogetherProductResponseDto.builder()
                .productId(targetProduct.getId())
                .auctionId(activeAuction.getId())
                .productName(targetProduct.getProductName())
                .category(targetProduct.getCategory() != null ? targetProduct.getCategory().name() : null)
                .primaryImageUrl(targetProduct.getPrimaryImage() != null ? 
                    targetProduct.getPrimaryImage().getImageUrl() : null)
                .currentHighestBid(activeAuction != null ? activeAuction.getCurrentHighestBid() : null)
                .buyNowPrice(activeAuction != null ? activeAuction.getBuyItNowPrice() : null)
                .brand(targetProduct.getBrand())
                .auctionStatus(getAuctionStatusForFrontend(activeAuction))
                .endTime(activeAuction != null ? activeAuction.getScheduledEndTime() : null)
                .bidCount(activeAuction != null ? activeAuction.getTotalBids() : 0)
                .associationScore(association.getAssociationScore())
                .build();
    }

    /**
     * Product와 연관도 점수로부터 DTO 생성
     * 
     * @param product 상품 엔티티
     * @param associationScore 연관도 점수
     * @return ViewTogetherProductResponseDto
     */
    public static ViewTogetherProductResponseDto fromProduct(Product product, BigDecimal associationScore) {
        Auction activeAuction = getActiveAuction(product);
        
        return ViewTogetherProductResponseDto.builder()
                .productId(product.getId())
                .auctionId(activeAuction.getId())
                .productName(product.getProductName())
                .category(product.getCategory() != null ? product.getCategory().name() : null)
                .primaryImageUrl(product.getPrimaryImage() != null ? 
                    product.getPrimaryImage().getImageUrl() : null)
                .currentHighestBid(activeAuction != null ? activeAuction.getCurrentHighestBid() : null)
                .buyNowPrice(activeAuction != null ? activeAuction.getBuyItNowPrice() : null)
                .brand(product.getBrand())
                .auctionStatus(getAuctionStatusForFrontend(activeAuction))
                .endTime(activeAuction != null ? activeAuction.getScheduledEndTime() : null)
                .bidCount(activeAuction != null ? activeAuction.getTotalBids() : 0)
                .associationScore(associationScore)
                .build();
    }

    /**
     * Product, Auction, 연관도 점수로부터 DTO 생성
     * 
     * @param product 상품 엔티티
     * @param auction 경매 엔티티 (null 가능)
     * @param associationScore 연관도 점수
     * @return ViewTogetherProductResponseDto
     */
    public static ViewTogetherProductResponseDto fromProductWithAuction(Product product, Auction auction, BigDecimal associationScore) {
        return ViewTogetherProductResponseDto.builder()
                .productId(product.getId())
                .auctionId(auction.getId())
                .productName(product.getProductName())
                .category(product.getCategory() != null ? product.getCategory().name() : null)
                .primaryImageUrl(product.getPrimaryImage() != null ? 
                    product.getPrimaryImage().getImageUrl() : null)
                .currentHighestBid(auction != null ? auction.getCurrentHighestBid() : null)
                .buyNowPrice(auction != null ? auction.getBuyItNowPrice() : null)
                .brand(product.getBrand())
                .auctionStatus(getAuctionStatusForFrontend(auction))
                .endTime(auction != null ? auction.getScheduledEndTime() : null)
                .bidCount(auction != null ? auction.getTotalBids() : 0)
                .associationScore(associationScore)
                .build();
    }
    
    /**
     * Product, Auction, 연관도 점수, 계산된 입찰 수로부터 DTO 생성
     * 사용자별 최신 입찰 기준으로 정확한 통계를 제공합니다.
     * 
     * @param product 상품 엔티티
     * @param auction 경매 엔티티 (null 가능)
     * @param associationScore 연관도 점수
     * @param calculatedBidCount 실제 계산된 입찰 수 (사용자별 최신 기준)
     * @return ViewTogetherProductResponseDto
     */
    public static ViewTogetherProductResponseDto fromProductWithCalculatedCount(
            Product product, Auction auction, BigDecimal associationScore, Integer calculatedBidCount) {
        return ViewTogetherProductResponseDto.builder()
                .productId(product.getId())
                .auctionId(auction.getId())
                .productName(product.getProductName())
                .category(product.getCategory() != null ? product.getCategory().name() : null)
                .primaryImageUrl(product.getPrimaryImage() != null ? 
                    product.getPrimaryImage().getImageUrl() : null)
                .currentHighestBid(auction != null ? auction.getCurrentHighestBid() : null)
                .buyNowPrice(auction != null ? auction.getBuyItNowPrice() : null)
                .brand(product.getBrand())
                .auctionStatus(getAuctionStatusForFrontend(auction))
                .endTime(auction != null ? auction.getScheduledEndTime() : null)
                .bidCount(calculatedBidCount != null ? calculatedBidCount : 0) // 계산된 값 사용
                .associationScore(associationScore)
                .build();
    }

    /**
     * 상품의 활성 경매 조회
     * Product 엔티티에 auctions 관계가 없으므로 null 반환
     * 실제 구현에서는 AuctionRepository를 통해 조회해야 함
     */
    private static Auction getActiveAuction(Product product) {
        // TODO: AuctionRepository를 통해 product.getId()로 활성 경매 조회
        return null;
    }

    /**
     * 프론트엔드용 경매 상태 변환
     */
    private static String getAuctionStatusForFrontend(Auction auction) {
        if (auction == null) {
            return "ENDED";
        }
        
        switch (auction.getStatus()) {
            case SCHEDULED:
            case READY:
                return "SCHEDULED";
            case IN_PROGRESS:
                return "IN_PROGRESS";
            case COMPLETED:
            case CANCELLED:
            case FAILED:
            default:
                return "ENDED";
        }
    }
}