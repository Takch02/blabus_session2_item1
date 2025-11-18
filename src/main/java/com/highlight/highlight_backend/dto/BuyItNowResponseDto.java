package com.highlight.highlight_backend.dto;

import com.highlight.highlight_backend.admin.auction.domain.Auction;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 즉시 구매 응답 DTO
 * 
 * @author 탁찬홍
 * @since 2025.08.21
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "즉시 구매 응답 DTO")
public class BuyItNowResponseDto {
    
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
     * 즉시 구매가
     */
    @Schema(description = "즉시 구매가", example = "100000")
    private BigDecimal buyItNowPrice;
    
    /**
     * 배송비
     */
    @Schema(description = "배송비", example = "3000")
    private BigDecimal shippingFee;
    
    /**
     * 총 결제 금액 (즉시구매가 + 배송비)
     */
    @Schema(description = "총 결제 금액", example = "103000")
    private BigDecimal totalAmount;
    
    /**
     * 사용한 포인트
     */
    @Schema(description = "사용한 포인트", example = "10000")
    private BigDecimal usedPointAmount;
    
    /**
     * 실제 결제 금액
     */
    @Schema(description = "실제 결제 금액", example = "90000")
    private BigDecimal actualPaymentAmount;
    
    /**
     * 포인트 적립 금액
     */
    @Schema(description = "결제 금액의 1% 포인트 적립", example = "900")
    private BigDecimal pointReward;
    
    /**
     * 결제 후 남은 포인트
     */
    @Schema(description = "결제 후 남은 포인트", example = "5900")
    private BigDecimal remainingPoint;
    
    /**
     * 구매 완료 시간
     */
    @Schema(description = "구매 완료 시간", example = "2025-08-19T16:30:00")
    private LocalDateTime completedAt;
    
    /**
     * Auction 엔티티로부터 BuyItNowResponseDto 생성
     * 
     * @param auction 경매 정보
     * @param userId 사용자 ID
     * @return BuyItNowResponseDto
     */
    public static BuyItNowResponseDto from(Auction auction, Long userId) {
        BigDecimal shippingFee = auction.getShippingFee() != null ? auction.getShippingFee() : BigDecimal.ZERO;
        BigDecimal totalAmount = auction.getBuyItNowPrice().add(shippingFee);
        
        return BuyItNowResponseDto.builder()
            .auctionId(auction.getId())
            .productName(auction.getProduct().getProductName())
            .buyItNowPrice(auction.getBuyItNowPrice())
            .shippingFee(shippingFee)
            .totalAmount(totalAmount)
            .usedPointAmount(BigDecimal.ZERO) // 기본값, 실제로는 PaymentService에서 계산
            .actualPaymentAmount(totalAmount) // 기본값, 실제로는 PaymentService에서 계산
            .pointReward(BigDecimal.ZERO) // 기본값, 실제로는 PaymentService에서 계산
            .remainingPoint(BigDecimal.ZERO) // 기본값, 실제로는 PaymentService에서 계산
            .completedAt(LocalDateTime.now())
            .build();
    }
}