package com.highlight.highlight_backend.dto;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.domain.Bid;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 경매 내 결과 조회 응답 DTO
 * 
 * 사용자가 특정 경매에서 얻은 결과를 조회합니다.
 * 정적 진입 시 모달 표시용 데이터입니다.
 * 
 * @author 전우선
 * @since 2025.08.16
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "경매 내 결과 조회 응답 DTO")
public class AuctionMyResultResponseDto {
    
    @Schema(description = "경매 ID", example = "1")
    private Long auctionId;
    
    @Schema(description = "상품명", example = "아이폰 15 Pro")
    private String productName;
    
    @Schema(description = "경매 결과 타입", example = "WON")
    private AuctionResultType resultType;
    
    @Schema(description = "사용자의 최종 입찰가", example = "1500000")
    private BigDecimal myFinalBid;
    
    @Schema(description = "낙찰가 (낙찰 시에만)", example = "1500000")
    private BigDecimal winningBid;
    
    @Schema(description = "경매 종료 시간", example = "2025-08-16T15:30:00")
    private LocalDateTime endTime;
    
    @Schema(description = "상품 이미지 URL", example = "https://example.com/image.jpg")
    private String productImageUrl;
    
    @Schema(description = "액션 버튼 텍스트", example = "결제하기")
    private String actionButtonText;
    
    @Schema(description = "액션 URL", example = "/payment/1")
    private String actionUrl;
    
    /**
     * 경매 결과 타입
     */
    @Getter
    public enum AuctionResultType {
        WON("낙찰"),
        LOST("유찰"), 
        CANCELLED("취소"),
        NO_PARTICIPATION("미참여");
        
        private final String description;
        
        AuctionResultType(String description) {
            this.description = description;
        }

    }
    
    /**
     * 낙찰 결과 생성
     */
    public static AuctionMyResultResponseDto createWonResult(Auction auction, Bid winningBid) {
        return new AuctionMyResultResponseDto(
            auction.getId(),
            auction.getProduct().getProductName(),
            AuctionResultType.WON,
            winningBid.getBidAmount(),
            winningBid.getBidAmount(),
            auction.getActualEndTime() != null ? auction.getActualEndTime() : auction.getScheduledEndTime(),
            auction.getProduct().getMainImageUrl(), // 상품 이미지 URL
            "결제하기",
            "/api/payments/preview/" + auction.getId()
        );
    }
    
    /**
     * 유찰 결과 생성
     */
    public static AuctionMyResultResponseDto createLostResult(Auction auction, Bid userBid, BigDecimal winningAmount) {
        return new AuctionMyResultResponseDto(
            auction.getId(),
            auction.getProduct().getProductName(),
            AuctionResultType.LOST,
            userBid.getBidAmount(),
            winningAmount,
            auction.getActualEndTime() != null ? auction.getActualEndTime() : auction.getScheduledEndTime(),
            auction.getProduct().getMainImageUrl(), // 상품 이미지 URL
            "다른 경매 참여하기",
            "/auctions?category=" + auction.getProduct().getCategory()
        );
    }
    
    /**
     * 취소 결과 생성
     */
    public static AuctionMyResultResponseDto createCancelledResult(Auction auction, Bid userBid) {
        return new AuctionMyResultResponseDto(
            auction.getId(),
            auction.getProduct().getProductName(),
            AuctionResultType.CANCELLED,
            userBid.getBidAmount(),
            null,
            auction.getActualEndTime() != null ? auction.getActualEndTime() : auction.getScheduledEndTime(),
            auction.getProduct().getMainImageUrl(), // 상품 이미지 URL
            "내 입찰 내역 확인하기",
            "/my-bids"
        );
    }
    
    /**
     * 미참여 결과 생성
     */
    public static AuctionMyResultResponseDto createNoParticipationResult(Auction auction) {
        return new AuctionMyResultResponseDto(
            auction.getId(),
            auction.getProduct().getProductName(),
            AuctionResultType.NO_PARTICIPATION,
            null,
            null,
            auction.getActualEndTime() != null ? auction.getActualEndTime() : auction.getScheduledEndTime(),
            auction.getProduct().getMainImageUrl(), // 상품 이미지 URL
            "다른 경매 보기",
            "/auctions"
        );
    }
}