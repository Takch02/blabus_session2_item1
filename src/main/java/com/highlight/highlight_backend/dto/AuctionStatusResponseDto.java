package com.highlight.highlight_backend.dto;

import com.highlight.highlight_backend.auction.domain.Auction;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 실시간 경매 상태 응답 DTO
 * 
 * @author 전우선
 * @since 2025.08.15
 */
@Getter
@AllArgsConstructor
@Schema(description = "실시간 경매 상태 응답 DTO")
public class AuctionStatusResponseDto {
    
    /**
     * 경매 ID
     */
    @Schema(description = "경매 ID", example = "1")
    private Long auctionId;
    
    /**
     * 상품 ID
     */
    @Schema(description = "상품 ID", example = "1")
    private Long productId;
    
    /**
     * 상품명
     */
    @Schema(description = "상품명", example = "아이폰 14 프로")
    private String productName;
    
    /**
     * 경매 상태
     */
    @Schema(description = "경매 상태", example = "IN_PROGRESS")
    private String status;
    
    /**
     * 경매 상태 설명
     */
    @Schema(description = "경매 상태 설명", example = "경매 진행 중")
    private String statusDescription;
    
    /**
     * 시작가
     */
    @Schema(description = "시작가", example = "30000")
    private BigDecimal startingPrice;
    
    /**
     * 현재 최고 입찰가
     */
    @Schema(description = "현재 최고 입찰가", example = "50000")
    private BigDecimal currentHighestBid;
    
    /**
     * 즉시구매가
     */
    @Schema(description = "즉시구매가", example = "100000")
    private BigDecimal buyItNowPrice;
    
    /**
     * 총 입찰자 수
     */
    @Schema(description = "총 입찰자 수", example = "5")
    private Long totalBidders;
    
    /**
     * 총 입찰 횟수
     */
    @Schema(description = "총 입찰 횟수", example = "12")
    private Long totalBids;
    
    /**
     * 입찰 단위
     */
    private BigDecimal bidUnit;
    
    /**
     * 최소 인상폭
     */
    private BigDecimal minimumBid;
    
    /**
     * 최대 인상폭
     */
    private BigDecimal maxBid;
    
    /**
     * 배송비
     */
    private BigDecimal shippingFee;
    
    /**
     * 직접 픽업 가능 여부
     */
    private Boolean isPickupAvailable;
    
    /**
     * 경매 예정 시작 시간
     */
    private LocalDateTime scheduledStartTime;
    
    /**
     * 경매 예정 종료 시간
     */
    private LocalDateTime scheduledEndTime;
    
    /**
     * 실제 경매 시작 시간
     */
    private LocalDateTime actualStartTime;
    
    /**
     * 실제 경매 종료 시간
     */
    private LocalDateTime actualEndTime;
    
    /**
     * 현재 최고 입찰자 닉네임 (마스킹 처리)
     */
    @Schema(description = "현재 최고 입찰자 닉네임 (마스킹 처리)", example = "홍**")
    private String currentWinnerNickname;
    
    /**
     * 마지막 업데이트 시간
     */
    @Schema(description = "마지막 업데이트 시간", example = "2025-08-15T14:30:00")
    private LocalDateTime lastUpdatedAt;
    
    /**
     * Auction 엔티티로부터 DTO 생성
     */
    public static AuctionStatusResponseDto from(Auction auction, Long totalBidders, Long totalBids, String winnerNickname) {
        return new AuctionStatusResponseDto(
            auction.getId(),
            auction.getProduct().getId(),
            auction.getProduct().getProductName(),
            auction.getStatus().name(),
            auction.getStatus().getDescription(),
            auction.getStartPrice(),
            auction.getCurrentHighestBid(),
            auction.getBuyItNowPrice(),
            totalBidders,
            totalBids,
            auction.getBidUnit(),
            auction.getMinimumBid(),
            auction.getMaxBid(),
            auction.getShippingFee(),
            auction.getIsPickupAvailable(),
            auction.getScheduledStartTime(),
            auction.getScheduledEndTime(),
            auction.getActualStartTime(),
            auction.getActualEndTime(),
            maskNickname(winnerNickname),
            LocalDateTime.now()
        );
    }
    
    /**
     * 닉네임 마스킹 처리
     */
    private static String maskNickname(String nickname) {
        if (nickname == null || nickname.length() <= 1) {
            return nickname;
        }
        
        if (nickname.length() == 2) {
            return nickname.charAt(0) + "*";
        }
        
        if (nickname.length() >= 3) {
            return nickname.charAt(0) + "**";
        }
        
        return nickname;
    }
}