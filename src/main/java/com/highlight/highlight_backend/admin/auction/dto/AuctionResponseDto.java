package com.highlight.highlight_backend.admin.auction.dto;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.admin.product.dto.ProductResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 경매 응답 DTO
 * 
 * @author 전우선
 * @since 2025.08.13
 */
@Getter
@AllArgsConstructor
@Builder
public class AuctionResponseDto {
    
    /**
     * 경매 ID
     */
    private Long auctionId;


    /**
     * 경매 상품 정보
     */
    private ProductResponseDto product;
    
    /**
     * 경매 상태
     */
    private Auction.AuctionStatus status;
    
    /**
     * 경매 상태 설명
     */
    private String statusDescription;
    
    /**
     * 경매 시작 예정 시간
     */
    private LocalDateTime scheduledStartTime;
    
    /**
     * 경매 종료 예정 시간
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
     * 시작가
     */
    private BigDecimal startPrice;
    /**
     * 현재 최고 입찰가
     */
    private BigDecimal currentHighestBid;
    
    /**
     * 즉시구매가
     */
    private BigDecimal buyItNowPrice;

    /**
     * 최소 인상폭
     */
    private BigDecimal minimumBid;

    /**
     * 최대 인상폭
     */
    private BigDecimal maxBid;

    /**
     * 입찰 단위
     */
    private BigDecimal bidUnit;

    /**
     * 배송비
     */
    private BigDecimal shippingFee;

    /**
     * 직접 픽업 가능 여부
     */
    private Boolean isPickupAvailable;
    
    /**
     * 총 입찰 참여자 수
     */
    private Integer totalBidders;
    
    /**
     * 총 입찰 횟수
     */
    private Integer totalBids;
    
    /**
     * 경매 생성한 관리자 ID
     */
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
     * 종료 사유
     */
    private String endReason;
    
    /**
     * 경매 설명/메모
     */
    private String description;
    
    /**
     * 생성 시간
     */
    private LocalDateTime createdAt;
    
    /**
     * 수정 시간
     */
    private LocalDateTime updatedAt;
    
    /**
     * Auction 엔티티로부터 DTO 생성
     */
    public static AuctionResponseDto from(Auction auction) {
        ProductResponseDto productDto = auction.getProduct() != null ?
                ProductResponseDto.from(auction.getProduct()) : null;

        return AuctionResponseDto.builder()
                .auctionId(auction.getId())
                .product(productDto)
                .status(auction.getStatus())
                .statusDescription(auction.getStatus().getDescription())
                .scheduledStartTime(auction.getScheduledStartTime())
                .scheduledEndTime(auction.getScheduledEndTime())
                .actualStartTime(auction.getActualStartTime())
                .actualEndTime(auction.getActualEndTime())
                .startPrice(auction.getStartPrice())
                .currentHighestBid(auction.getCurrentHighestBid())
                .buyItNowPrice(auction.getBuyItNowPrice())
                .minimumBid(auction.getMinimumBid())
                .maxBid(auction.getMaxBid())
                .bidUnit(auction.getBidUnit())
                .shippingFee(auction.getShippingFee())
                .isPickupAvailable(auction.getIsPickupAvailable())
                .totalBidders(auction.getTotalBidders())
                .totalBids(auction.getTotalBids())
                .createdBy(auction.getCreatedBy())
                .startedBy(auction.getStartedBy())
                .endedBy(auction.getEndedBy())
                .endReason(auction.getEndReason())
                .description(auction.getDescription())
                .createdAt(auction.getCreatedAt())
                .updatedAt(auction.getUpdatedAt())
                .build();
    }

    /**
     * 상품 정보 없이 경매 기본 정보만 포함한 DTO 생성
     */
    public static AuctionResponseDto fromWithoutProduct(Auction auction) {
        return new AuctionResponseDto(
            auction.getId(),
            null, // 상품 정보 제외
            auction.getStatus(),
            auction.getStatus().getDescription(),
            auction.getScheduledStartTime(),
            auction.getScheduledEndTime(),
            auction.getActualStartTime(),
            auction.getActualEndTime(),
            auction.getStartPrice(),
            auction.getCurrentHighestBid(),
            auction.getBuyItNowPrice(),
            auction.getMinimumBid(),
            auction.getMaxBid(),
            auction.getBidUnit(),
            auction.getShippingFee(),
            auction.getIsPickupAvailable(),
            auction.getTotalBidders(),
            auction.getTotalBids(),
            auction.getCreatedBy(),
            auction.getStartedBy(),
            auction.getEndedBy(),
            auction.getEndReason(),
            auction.getDescription(),
            auction.getCreatedAt(),
            auction.getUpdatedAt()
        );
    }
    
    /**
     * 경매 엔티티와 계산된 통계로부터 AuctionResponseDto를 생성합니다.
     * 사용자별 최신 입찰 기준으로 정확한 통계를 제공합니다.
     * 
     * @param auction 원본 Auction 엔티티
     * @param calculatedTotalBidders 실제 계산된 입찰자 수
     * @param calculatedTotalBids 실제 계산된 입찰 수 (사용자별 최신 기준)
     * @return 변환된 DTO
     */
    public static AuctionResponseDto fromWithCalculatedStats(Auction auction, Integer calculatedTotalBidders, Integer calculatedTotalBids) {
        return AuctionResponseDto.builder()
                .auctionId(auction.getId())
                .product(ProductResponseDto.from(auction.getProduct()))
                .status(auction.getStatus())
                .statusDescription(auction.getStatus().getDescription())
                .scheduledStartTime(auction.getScheduledStartTime())
                .scheduledEndTime(auction.getScheduledEndTime())
                .actualStartTime(auction.getActualStartTime())
                .actualEndTime(auction.getActualEndTime())
                .startPrice(auction.getStartPrice())
                .currentHighestBid(auction.getCurrentHighestBid())
                .buyItNowPrice(auction.getBuyItNowPrice())
                .minimumBid(auction.getMinimumBid())
                .maxBid(auction.getMaxBid())
                .bidUnit(auction.getBidUnit())
                .shippingFee(auction.getShippingFee())
                .isPickupAvailable(auction.getIsPickupAvailable())
                .totalBidders(calculatedTotalBidders) // 계산된 값 사용
                .totalBids(calculatedTotalBids) // 계산된 값 사용
                .createdBy(auction.getCreatedBy())
                .startedBy(auction.getStartedBy())
                .endedBy(auction.getEndedBy())
                .endReason(auction.getEndReason())
                .description(auction.getDescription())
                .createdAt(auction.getCreatedAt())
                .updatedAt(auction.getUpdatedAt())
                .build();
    }
}