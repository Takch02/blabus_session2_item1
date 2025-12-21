package com.highlight.highlight_backend.auction.domain;

import com.highlight.highlight_backend.auction.dto.AuctionScheduleRequestDto;
import com.highlight.highlight_backend.auction.dto.AuctionUpdateRequestDto;
import com.highlight.highlight_backend.exception.AuctionErrorCode;
import com.highlight.highlight_backend.exception.BusinessException;
import com.highlight.highlight_backend.product.domian.Product;
import com.highlight.highlight_backend.user.domain.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
    private Long totalBidders = 0L;
    
    /**
     * 총 입찰 횟수
     */
    @Column(nullable = false)
    private Long totalBids = 0L;

    /**
     * 현재 우승자 이름
     */
    private String currentWinnerName;

    /**
     * 최종 우승자 userID
     */
    private Long winnerId;
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
     * Product 에 있지만 넣은 이유
     * 사용자는 Auction을 보게 되므로 탐색도 Auction을 기준으로 하게됨.
     * 여기서 Category 정렬을 자주 이용할텐데 Auction에 없을 경우 탐색 효율이 최악이 됨.
     * 용량을 조금 희생하더라도 탐색시간은 5s -> 50ms 로 바꿀 수 있다면 1000배 좋아지므로 선택함.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category") // DB 컬럼명
    private Product.Category category;

    /**
     * 생성 시간
     */
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Auction 수정 시 사용
     */
    @LastModifiedDate
    private LocalDateTime updatedAt;

    public void updateDetail(AuctionUpdateRequestDto request, LocalDateTime kstStartTime, LocalDateTime kstEndTime) {
        // 가격 정보 수정
        if (request.getStartPrice() != null) {
            this.setStartPrice(request.getStartPrice());
        }

        if (request.getBidUnit() != null) {
            this.setBidUnit(request.getBidUnit());
        }

        if (request.getMaxBid() != null) {
            this.setMaxBid(request.getMaxBid());
        }

        if (request.getMinimumBid() != null) {
            this.setMinimumBid(request.getMinimumBid());
        }

        if (request.getShippingFee() != null) {
            this.setShippingFee(request.getShippingFee());
        }

        if (request.getIsPickupAvailable() != null) {
            this.setIsPickupAvailable(request.getIsPickupAvailable());
        }

        // 설명 수정
        if (request.getDescription() != null) {
            this.setDescription(request.getDescription());
        }

        //  시간 정보 수정
        if (kstStartTime != null) {
            this.setScheduledStartTime(kstStartTime);
        }

        if (kstEndTime != null) {
            this.setScheduledEndTime(kstEndTime);
        }
    }

    /**
     * Auction 에 처음 추가시 사용
     */
    public void addDetail(Product product, Long adminId, LocalDateTime kstStartTime,
                          LocalDateTime kstEndTime, AuctionScheduleRequestDto request) {

        this.setProduct(product);
        this.setStatus(Auction.AuctionStatus.SCHEDULED); // 초기 상태는 '예약됨'
        this.setScheduledStartTime(kstStartTime);
        this.setScheduledEndTime(kstEndTime);
        this.setDescription(request.getDescription());
        this.setBuyItNowPrice(request.getBuyItNowPrice());
        this.setCreatedBy(adminId);
        this.setBidUnit(request.getBidUnit());
        this.setStartPrice(request.getStartPrice());         // 경매 시작가 설정
        this.setCurrentHighestBid(request.getStartPrice());
        this.setMinimumBid(request.getMinimumBid());       // 최소 인상폭 설정
        this.setMaxBid(request.getMaxBid());               // 최대 인상폭 설정
        this.setShippingFee(request.getShippingFee());       // 배송비 설정
        this.setIsPickupAvailable(request.getIsPickupAvailable()); // 직접 픽업 가능 여부
        this.setCategory(product.getCategory());
    }

    public void validateBid(BigDecimal bidAmount) {
        // 1. 상태 체크
        if (this.status != AuctionStatus.IN_PROGRESS) {
            throw new BusinessException(AuctionErrorCode.CANNOT_START_AUCTION);
        }

        // 2. 금액 체크 (현재가 + 단위 vs 시작가)
        BigDecimal minBid = (currentHighestBid == null)
                ? startPrice
                : currentHighestBid.add(minimumBid); // minimumBid는 최소 인상폭

        if (bidAmount.compareTo(minBid) < 0) {
            throw new BusinessException(AuctionErrorCode.INVALID_MINIMUM_BID);
        }
    }

    /**
     * Bid 생성 시 실행
     * 최고가로 입찰한 유저의 userId, nickname, 금액, TotalBids, TotalBidders 갱신
     * 이 컬럼들은 전부 반정규화 진행헀음.
     */
    public void updateHighestBid(User user, BigDecimal bidAmount, boolean isNewBidder) {
        this.currentHighestBid = bidAmount;
        this.currentWinnerName = user.getNickname();  // 현재 최고가 입찰 닉네임 넣기
        this.winnerId = user.getId();  // 현재 최고가 입찰한 userId 입력
        this.totalBids++;
        if (isNewBidder) {
            this.totalBidders++;
        }
    }

    /**
     * 경매 상태 열거형
     */
    @Getter
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