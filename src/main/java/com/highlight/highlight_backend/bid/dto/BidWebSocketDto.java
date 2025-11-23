package com.highlight.highlight_backend.bid.dto;

import com.highlight.highlight_backend.bid.domain.Bid;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 입찰 WebSocket 메시지 데이터 DTO
 * 
 * @author 전우선
 * @since 2025.08.15
 */
@Getter
@AllArgsConstructor
@Schema(description = "입찰 WebSocket 메시지 데이터 DTO")
public class BidWebSocketDto {
    
    /**
     * 입찰 ID
     */
    @Schema(description = "입찰 ID", example = "1")
    private Long bidId;
    
    /**
     * 입찰자 닉네임 (마스킹)
     */
    @Schema(description = "입찰자 닉네임 (마스킹)", example = "전**")
    private String bidderNickname;
    
    /**
     * 입찰 금액
     */
    @Schema(description = "입찰 금액", example = "50000")
    private BigDecimal bidAmount;
    
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
     * 자동 입찰 여부
     */
    @Schema(description = "자동 입찰 여부", example = "false")
    private Boolean isAutoBid;
    
    /**
     * 입찰 시간
     */
    @Schema(description = "입찰 시간", example = "2025-08-15T14:30:00")
    private LocalDateTime bidTime;
    
    /**
     * Bid 엔티티와 통계 정보로부터 DTO 생성
     */
    public static BidWebSocketDto from(Bid bid, Long totalBidders, Long totalBids) {
        String maskedNickname = maskNickname(bid.getUser().getNickname());
        
        return new BidWebSocketDto(
            bid.getId(),
            maskedNickname,
            bid.getBidAmount(),
            bid.getAuction().getCurrentHighestBid(),
            totalBidders,
            totalBids,
            bid.isAutoBid(),
            bid.getCreatedAt()
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