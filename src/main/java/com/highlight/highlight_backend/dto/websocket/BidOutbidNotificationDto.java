package com.highlight.highlight_backend.dto.websocket;

import com.highlight.highlight_backend.admin.auction.domain.Auction;
import com.highlight.highlight_backend.domain.Bid;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 입찰 경합 패배 알림 DTO
 * 
 * 다른 사용자에게 입찰이 밀렸을 때 개인 알림용 데이터
 * 추가 참여를 유도하는 정보 포함
 * 
 * @author 전우선
 * @since 2025.08.16
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "입찰 경합 패배 알림 DTO")
public class BidOutbidNotificationDto {
    
    @Schema(description = "경매 ID", example = "1")
    private Long auctionId;
    
    @Schema(description = "상품명", example = "아이폰 15 Pro")
    private String productName;
    
    @Schema(description = "밀려난 내 입찰가", example = "1500000")
    private BigDecimal myBidAmount;
    
    @Schema(description = "현재 최고 입찰가", example = "1600000")
    private BigDecimal currentHighestBid;
    
    @Schema(description = "추천 입찰가 (최고가 + 최소증가분)", example = "1610000")
    private BigDecimal suggestedBidAmount;
    
    @Schema(description = "최소 입찰 증가 단위", example = "10000")
    private BigDecimal minimumBidIncrement;
    
    @Schema(description = "경매 종료까지 남은 시간 (초)", example = "3600")
    private Long remainingTimeInSeconds;
    
    @Schema(description = "현재 입찰 참여자 수", example = "15")
    private Integer totalBidders;
    
    @Schema(description = "총 입찰 횟수", example = "42")
    private Integer totalBids;
    
    @Schema(description = "연속 패배 횟수", example = "2")
    private Integer consecutiveLosses;
    
    @Schema(description = "패배 알림 메시지", example = "다른 입찰자가 1,600,000원으로 입찰하여 귀하의 입찰이 밀렸습니다.")
    private String message;
    
    @Schema(description = "참여 독려 메시지", example = "지금 1,610,000원으로 다시 도전하세요!")
    private String encouragementMessage;
    
    /**
     * 입찰 경합 패배 알림 생성
     */
    public static BidOutbidNotificationDto from(
            Bid outbidBid, 
            Bid newHighestBid, 
            Integer consecutiveLosses,
            Long totalBidders,
            Long totalBids) {
        
        Auction auction = outbidBid.getAuction();
        BigDecimal suggestedAmount = newHighestBid.getBidAmount().add(auction.getMinimumBid());
        
        // 연속 패배 횟수에 따른 메시지 차별화
        String encouragementMsg = generateEncouragementMessage(consecutiveLosses, suggestedAmount);
        
        String message = String.format(
            "다른 입찰자가 %,d원으로 입찰하여 귀하의 입찰이 밀렸습니다.",
            newHighestBid.getBidAmount().longValue()
        );
        
        return new BidOutbidNotificationDto(
            auction.getId(),
            auction.getProduct().getProductName(),
            outbidBid.getBidAmount(),
            newHighestBid.getBidAmount(),
            suggestedAmount,
            auction.getMinimumBid(),
            calculateRemainingSeconds(auction.getScheduledEndTime()),
            totalBidders.intValue(),
            totalBids.intValue(),
            consecutiveLosses,
            message,
            encouragementMsg
        );
    }
    
    /**
     * 연속 패배 횟수에 따른 독려 메시지 생성
     */
    private static String generateEncouragementMessage(Integer consecutiveLosses, BigDecimal suggestedAmount) {
        if (consecutiveLosses == null) consecutiveLosses = 0;
        
        String baseMessage = String.format("지금 %,d원으로 다시 도전하세요!", suggestedAmount.longValue());
        
        if (consecutiveLosses >= 3) {
            return "포기하지 마세요! " + baseMessage + " 🔥 연속 도전으로 승리를 쟁취하세요!";
        } else if (consecutiveLosses >= 2) {
            return "한 번 더! " + baseMessage + " ⚡ 이번엔 꼭 성공할 수 있습니다!";
        } else {
            return baseMessage + " 💪 아직 기회가 있습니다!";
        }
    }
    
    /**
     * 경매 종료까지 남은 시간 계산 (초 단위)
     */
    private static Long calculateRemainingSeconds(LocalDateTime endTime) {
        if (endTime == null) return 0L;
        
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(endTime)) return 0L;
        
        return java.time.Duration.between(now, endTime).getSeconds();
    }
}