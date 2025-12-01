package com.highlight.highlight_backend.bid.dto;

import com.highlight.highlight_backend.bid.domain.Bid;
import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.product.domian.Product;
import com.highlight.highlight_backend.seller.domain.Seller;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 낙찰 상세 정보 응답 DTO
 * 
 * 낙찰받은 상품의 상세 정보, 판매자 정보, 경매 정보를 포함합니다.
 * 
 * @author 전우선
 * @since 2025.08.16
 */
@Getter
@AllArgsConstructor
@Schema(description = "낙찰 상세 정보 응답 DTO")
public class WinBidDetailResponseDto {
    
    /**
     * 입찰 ID
     */
    @Schema(description = "입찰 ID", example = "1")
    private Long bidId;
    
    /**
     * 경매 ID
     */
    @Schema(description = "경매 ID", example = "1")
    private Long auctionId;
    
    /**
     * 낙찰 금액
     */
    @Schema(description = "낙찰 금액", example = "50000")
    private BigDecimal winningAmount;
    
    /**
     * 낙찰 시간
     */
    @Schema(description = "낙찰 시간", example = "2025-08-16T14:30:00")
    private LocalDateTime winningTime;
    
    /**
     * 상품 정보
     */
    @Schema(description = "상품 정보")
    private ProductInfo product;
    
    /**
     * 판매자 정보
     */
    @Schema(description = "판매자 정보")
    private SellerInfo seller;
    
    /**
     * 경매 정보
     */
    @Schema(description = "경매 정보")
    private AuctionInfo auction;
    
    /**
     * 상품 정보 내부 클래스
     */
    @Getter
    @AllArgsConstructor
    @Schema(description = "상품 정보")
    public static class ProductInfo {
        @Schema(description = "상품 ID", example = "1")
        private Long productId;
        
        @Schema(description = "상품명", example = "빈티지 카펫")
        private String productName;
        
        @Schema(description = "상품 설명", example = "고급 빈티지 카펫입니다.")
        private String description;
        
        @Schema(description = "브랜드", example = "NAFAL")
        private String brand;
        
        @Schema(description = "카테고리", example = "가구/인테리어")
        private String category;
        
        @Schema(description = "재질", example = "울")
        private String material;
        
        @Schema(description = "사이즈", example = "200x150cm")
        private String size;
        
        @Schema(description = "제조년도", example = "2020")
        private Integer manufactureYear;
        
        @Schema(description = "상품 등급", example = "A")
        private String productRank;
        
        @Schema(description = "상품 상태", example = "판매완료")
        private String status;
        
        @Schema(description = "메인 이미지 URL", example = "https://example.com/image.jpg")
        private String primaryImageUrl;
        
        @Schema(description = "시작가", example = "30000")
        private BigDecimal startingPrice;
    }
    
    /**
     * 판매자 정보 내부 클래스
     */
    @Getter
    @AllArgsConstructor
    @Schema(description = "판매자 정보")
    public static class SellerInfo {
        @Schema(description = "판매자 ID", example = "1")
        private Long sellerId;
        
        @Schema(description = "판매자명", example = "NAFAL")
        private String sellerName;
        
        @Schema(description = "판매자 평점", example = "4.8")
        private Double rating;
        
        @Schema(description = "총 판매 건수", example = "156")
        private Integer totalSales;
        
        @Schema(description = "연락처", example = "02-1234-5678")
        private String contactNumber;
        
        @Schema(description = "이메일", example = "nafal@example.com")
        private String email;
    }
    
    /**
     * 경매 정보 내부 클래스
     */
    @Getter
    @AllArgsConstructor
    @Schema(description = "경매 정보")
    public static class AuctionInfo {
        @Schema(description = "경매 시작 시간", example = "2025-08-15T10:00:00")
        private LocalDateTime startTime;
        
        @Schema(description = "경매 종료 시간", example = "2025-08-15T18:00:00")
        private LocalDateTime endTime;
        
        @Schema(description = "총 입찰 건수", example = "25")
        private Long totalBids;
        
        @Schema(description = "총 입찰자 수", example = "12")
        private Long totalBidders;
        
        @Schema(description = "입찰 단위", example = "1000")
        private BigDecimal bidUnit;
        
        @Schema(description = "배송비", example = "3000")
        private BigDecimal shippingFee;
        
        @Schema(description = "경매 상태", example = "종료")
        private String status;
    }
    
    /**
     * Bid 엔티티로부터 낙찰 상세 DTO 생성
     * 
     * @param bid 낙찰된 입찰 엔티티
     * @return 낙찰 상세 응답 DTO
     */
    public static WinBidDetailResponseDto from(Bid bid) {
        var product = bid.getAuction().getProduct();
        var auction = bid.getAuction();
        var seller = product.getSeller();
        
        // 상품 정보 생성
        ProductInfo productInfo = new ProductInfo(
            product.getId(),
            product.getProductName(),
            product.getShortDescription(),
            product.getBrand(),
            product.getCategory() != null ? product.getCategory().name() : null,
            product.getMaterial(),
            product.getSize(),
            product.getManufactureYear(),
            product.getRank() != null ? product.getRank().name() : null,
            product.getStatus() != null ? product.getStatus().name() : null,
            product.getPrimaryImage() != null ? product.getPrimaryImage().getImageUrl() : null,
            auction.getStartPrice()
        );
        
        // 판매자 정보 생성
        SellerInfo sellerInfo = new SellerInfo(
            seller != null ? seller.getId() : null,
            seller != null ? seller.getSellerName() : "NAFAL",
            seller != null ? seller.getRating().doubleValue() : 4.8,
            seller != null ? seller.getSalesCount().intValue() : 0,
            seller != null ? seller.getPhoneNumber() : null,
            seller != null ? seller.getEmail() : null
        );
        
        // 경매 정보 생성
        AuctionInfo auctionInfo = new AuctionInfo(
            auction.getActualStartTime() != null ? auction.getActualStartTime() : auction.getScheduledStartTime(),
            auction.getActualEndTime() != null ? auction.getActualEndTime() : auction.getScheduledEndTime(),
            auction.getTotalBids(),
            auction.getTotalBidders(),
            auction.getBidUnit(),
            auction.getShippingFee(),
            auction.getStatus() != null ? auction.getStatus().name() : null
        );
        
        return new WinBidDetailResponseDto(
            bid.getId(),
            auction.getId(),
            bid.getBidAmount(),
            bid.getCreatedAt(),
            productInfo,
            sellerInfo,
            auctionInfo
        );
    }
    
    /**
     * 우승한 입찰의 상세 정보를 보여줍니다.
     */
    public static WinBidDetailResponseDto fromWithCalculatedStats(Bid bid, Long calculatedTotalBids, Long calculatedTotalBidders) {
        Auction auction = bid.getAuction();
        Product product = auction.getProduct();
        Seller seller = product.getSeller();
        
        // 상품 정보 생성 (기존 from() 메서드와 동일한 구조)
        ProductInfo productInfo = new ProductInfo(
            product.getId(),
            product.getProductName(),
            product.getShortDescription(),
            product.getBrand(),
            product.getCategory() != null ? product.getCategory().name() : null,
            product.getMaterial(),
            product.getSize(),
            product.getManufactureYear(),
            product.getRank() != null ? product.getRank().name() : null,
            product.getStatus() != null ? product.getStatus().name() : null,
            product.getPrimaryImage() != null ? product.getPrimaryImage().getImageUrl() : null,
            auction.getStartPrice()
        );
        
        // 판매자 정보 생성
        SellerInfo sellerInfo = new SellerInfo(
            seller != null ? seller.getId() : null,
            seller != null ? seller.getSellerName() : "NAFAL",
            seller != null ? seller.getRating().doubleValue() : 4.8,
            seller != null ? seller.getSalesCount().intValue() : 0,
            seller != null ? seller.getPhoneNumber() : null,
            seller != null ? seller.getEmail() : null
        );
        
        // 경매 정보 생성 (계산된 통계 사용)
        AuctionInfo auctionInfo = new AuctionInfo(
            auction.getActualStartTime() != null ? auction.getActualStartTime() : auction.getScheduledStartTime(),
            auction.getActualEndTime() != null ? auction.getActualEndTime() : auction.getScheduledEndTime(),
            calculatedTotalBids, // 계산된 값 사용
            calculatedTotalBidders, // 계산된 값 사용
            auction.getBidUnit(),
            auction.getShippingFee(),
            auction.getStatus() != null ? auction.getStatus().name() : null
        );
        
        return new WinBidDetailResponseDto(
            bid.getId(),
            auction.getId(),
            bid.getBidAmount(),
            bid.getCreatedAt(),
            productInfo,
            sellerInfo,
            auctionInfo
        );
    }
}