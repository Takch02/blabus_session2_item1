package com.highlight.highlight_backend.bid.domain;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.bid.dto.BidCreateRequestDto;
import com.highlight.highlight_backend.user.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 입찰 엔티티
 * 
 * 경매에 참여한 사용자들의 입찰 정보를 저장하는 엔티티입니다.
 * 
 * @author 전우선
 * @since 2025.08.15
 */
@Entity
@Table(name = "bid")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Bid {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 경매 정보
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;
    
    /**
     * 입찰한 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * 입찰 금액
     */
    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal bidAmount;
    
    /**
     * 입찰 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BidStatus status = BidStatus.ACTIVE;
    
    /**
     * 자동 입찰 여부
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isAutoBid = false;
    
    /**
     * 즉시구매 여부
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isBuyItNow = false;
    
    /**
     * 자동 입찰 최대 금액 (자동 입찰인 경우)
     */
    @Column(precision = 15, scale = 0)
    private BigDecimal maxAutoBidAmount;
    
    /**
     * 입찰 취소 시간
     */
    private LocalDateTime cancelledAt;
    
    /**
     * 입찰 취소 사유
     */
    @Column(length = 500)
    private String cancelReason;
    
    /**
     * 입찰 시간
     */
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public void setNewBid(Auction auction, User user, BidCreateRequestDto request) {
        this.setAuction(auction);
        this.setUser(user);
        this.setBidAmount(request.getBidAmount());
        this.setIsAutoBid(request.getIsAutoBid() != null ? request.getIsAutoBid() : false);
        this.setMaxAutoBidAmount(request.getMaxAutoBidAmount());
        this.setStatus(Bid.BidStatus.WINNING);
    }

    /**
     * 입찰 상태 열거형
     */
    public enum BidStatus {
        ACTIVE("활성"),           // 유효한 입찰
        OUTBID("경합패배"),       // 더 높은 입찰에 의해 밀림
        WINNING("최고가"),        // 현재 최고 입찰
        WON("낙찰"),             // 낙찰 성공
        CANCELLED("취소");        // 입찰 취소
        
        private final String description;
        
        BidStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 입찰 취소
     */
    public void cancel(String reason) {
        this.status = BidStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancelReason = reason;
    }
    
    /**
     * 최고가 입찰로 변경
     */
    public void setAsWinning() {
        this.status = BidStatus.WINNING;
    }
    
    /**
     * 경합 패배로 변경
     */
    public void setAsOutbid() {
        this.status = BidStatus.OUTBID;
    }
    
    /**
     * 낙찰 성공으로 변경
     */
    public void setAsWon() {
        this.status = BidStatus.WON;
    }
    
    /**
     * 입찰이 활성 상태인지 확인
     */
    public boolean isActive() {
        return this.status == BidStatus.ACTIVE || this.status == BidStatus.WINNING;
    }
    
    /**
     * 자동 입찰인지 확인
     */
    public boolean isAutoBid() {
        return this.isAutoBid != null && this.isAutoBid;
    }
    
    /**
     * 즉시구매인지 확인
     */
    public boolean isBuyItNow() {
        return this.isBuyItNow != null && this.isBuyItNow;
    }
}