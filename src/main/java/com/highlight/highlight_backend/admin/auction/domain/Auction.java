package com.highlight.highlight_backend.admin.auction.domain;

import com.highlight.highlight_backend.domain.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 경매 엔티티
 * 
 * nafal 경매 시스템의 경매 정보를 저장하는 엔티티입니다.
 * 
 * @author 전우선
 * @since 2025.08.13
 */
@Entity
@Table(name = "auction")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Auction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 경매 상품
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    /**
     * 경매 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionStatus status = AuctionStatus.SCHEDULED;
    
    /**
     * 경매 시작 예정 시간
     */
    @Column(nullable = false)
    private LocalDateTime scheduledStartTime;
    
    /**
     * 경매 종료 예정 시간
     */
    @Column(nullable = false)
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
     * 경매 시작가 -> 맨 처음 시작 시 가격
     */
    @Column(nullable = false)
    private BigDecimal startPrice;

    /**
     * 현재 최고 입찰가
     */
    @Column(precision = 15, scale = 0)
    private BigDecimal currentHighestBid;
    
    /**
     * 즉시구매가
     */
    @Column(name = "buy_it_now_price", precision = 15, scale = 0)
    private BigDecimal buyItNowPrice;

    /**
     * 최소 인상폭
     */
    @Column(nullable = false)   
    private BigDecimal minimumBid;

    /**
     * 최대 인상폭
     */
    @Column(nullable = false)
    private BigDecimal maxBid;

    /**
     * 입찰 단위
     */
    @Column(nullable = false)
    private BigDecimal bidUnit;

    /**
     * 배송비
     */
    @Column(precision = 15, scale = 0)
    private BigDecimal shippingFee;

    /**
     * 직접 픽업 가능 여부
     */
    @Column(nullable = false)
    private Boolean isPickupAvailable = false;
    
    /**
     * 총 입찰 참여자 수
     */
    @Column(nullable = false)
    private Integer totalBidders = 0;
    
    /**
     * 총 입찰 횟수
     */
    @Column(nullable = false)
    private Integer totalBids = 0;
    
    /**
     * 경매 생성한 관리자 ID
     */
    @Column(nullable = false)
    private Long createdBy;
    
    /**
     * 경매 시작한 관리자 ID
     */
    private Long startedBy;
    
    /**
     * 경매 종료한 관리자 ID
     */
    private Long endedBy;
    
    /**
     * 종료 사유 (정상종료, 중단 등)
     */
    @Column(length = 100)
    private String endReason;
    
    /**
     * 경매 설명/메모
     */
    @Column(columnDefinition = "TEXT")
    private String description;
    
    /**
     * 생성 시간
     */
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 수정 시간
     */
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    /**
     * 경매 상태 열거형
     */
    public enum AuctionStatus {
        SCHEDULED("예약됨"),          // 경매 예약 상태
        READY("시작대기"),            // 경매 시작 준비 완료
        IN_PROGRESS("진행중"),        // 경매 진행 중
        COMPLETED("완료"),           // 경매 정상 완료
        CANCELLED("중단"),           // 경매 중단
        FAILED("실패");              // 경매 실패 (낙찰자 없음 등)
        
        private final String description;
        
        AuctionStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 경매 시작 처리
     */
    public void startAuction(Long adminId) {
        this.status = AuctionStatus.IN_PROGRESS;
        this.actualStartTime = LocalDateTime.now();
        this.startedBy = adminId;
        this.currentHighestBid = this.startPrice;
    }
    
    /**
     * 경매 종료 처리
     */
    public void endAuction(Long adminId, String reason) {
        this.status = AuctionStatus.COMPLETED;
        this.actualEndTime = LocalDateTime.now();
        this.endedBy = adminId;
        this.endReason = reason;
    }
    
    /**
     * 경매 중단 처리
     */
    public void cancelAuction(Long adminId, String reason) {
        this.status = AuctionStatus.CANCELLED;
        this.actualEndTime = LocalDateTime.now();
        this.endedBy = adminId;
        this.endReason = reason;
    }
    
    /**
     * 경매 진행 가능 여부 확인
     */
    public boolean canStart() {
        return this.status == AuctionStatus.SCHEDULED || this.status == AuctionStatus.READY;
    }
    
    /**
     * 경매 종료 가능 여부 확인
     */
    public boolean canEnd() {
        return this.status == AuctionStatus.IN_PROGRESS;
    }
    
    /**
     * 경매 진행 중 여부 확인
     */
    public boolean isInProgress() {
        return this.status == AuctionStatus.IN_PROGRESS;
    }
}