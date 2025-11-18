package com.highlight.highlight_backend.dto.websocket;

import com.highlight.highlight_backend.admin.auction.domain.Auction;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 경매 상태 WebSocket 메시지 데이터 DTO
 * 
 * @author 전우선
 * @since 2025.08.15
 */
@Getter
@AllArgsConstructor
@Schema(description = "경매 상태 WebSocket 메시지 데이터 DTO")
public class AuctionStatusWebSocketDto {
    
    /**
     * 경매 ID
     */
    @Schema(description = "경매 ID", example = "1")
    private Long auctionId;
    
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
     * 현재 최고 입찰가
     */
    @Schema(description = "현재 최고 입찰가", example = "50000")
    private BigDecimal currentHighestBid;
    
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
     * 현재 최고 입찰자 닉네임 (마스킹)
     */
    @Schema(description = "현재 최고 입찰자 닉네임 (마스킹)", example = "전**")
    private String currentWinnerNickname;
    
    /**
     * 경매 시작 시간
     */
    private LocalDateTime scheduledStartTime;
    
    /**
     * 경매 종료 시간
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
     * 남은 시간 (초)
     */
    @Schema(description = "경매 종료까지 남은 시간 (초)", example = "3600")
    private Long remainingTimeInSeconds;
    
    /**
     * 마지막 업데이트 시간
     */
    @Schema(description = "마지막 업데이트 시간", example = "2025-08-15T14:30:00")
    private LocalDateTime lastUpdatedAt;
    
    /**
     * Auction 엔티티와 통계 정보로부터 DTO 생성
     */
    public static AuctionStatusWebSocketDto from(Auction auction, Long totalBidders, Long totalBids, String winnerNickname) {
        // 남은 시간 계산
        Long remainingSeconds = calculateRemainingSeconds(auction);
        
        return new AuctionStatusWebSocketDto(
            auction.getId(),
            auction.getProduct().getProductName(),
            auction.getStatus().name(),
            auction.getStatus().getDescription(),
            auction.getCurrentHighestBid(),
            totalBidders,
            totalBids,
            maskNickname(winnerNickname),
            auction.getScheduledStartTime(),
            auction.getScheduledEndTime(),
            auction.getActualStartTime(),
            auction.getActualEndTime(),
            remainingSeconds,
            LocalDateTime.now()
        );
    }
    
    /**
     * 경매 종료까지 남은 시간 계산 (초 단위)
     */
    private static Long calculateRemainingSeconds(Auction auction) {
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        LocalDateTime endTime = auction.getScheduledEndTime();
        
        // 경매가 진행 중이 아니거나 이미 종료된 경우
        if (auction.getStatus() != Auction.AuctionStatus.IN_PROGRESS || 
            now.isAfter(endTime)) {
            return 0L;
        }
        
        // 남은 시간 계산 (초 단위)
        java.time.Duration duration = java.time.Duration.between(now, endTime);
        return Math.max(0L, duration.getSeconds());
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