package com.highlight.highlight_backend.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 응답 DTO
 * 
 * @author 탁찬홍
 * @since 2025.08.21
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "결제 응답 DTO")
public class PaymentResponseDto {
    
    /**
     * 결제 ID
     */
    @Schema(description = "결제 ID", example = "1")
    private Long paymentId;
    
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
     * 낙찰가
     */
    @Schema(description = "낙찰가", example = "60000")
    private BigDecimal winningBidAmount;
    
    /**
     * 배송비
     */
    @Schema(description = "배송비", example = "3000")
    private BigDecimal shippingFee;
    
    /**
     * 총 결제 금액 (낙찰가 + 배송비)
     */
    @Schema(description = "총 결제 금액", example = "63000")
    private BigDecimal totalAmount;
    
    /**
     * 사용한 포인트
     */
    @Schema(description = "사용한 포인트", example = "10000")
    private BigDecimal usedPointAmount;
    
    /**
     * 실제 결제 금액
     */
    @Schema(description = "실제 결제 금액", example = "50000")
    private BigDecimal actualPaymentAmount;
    
    /**
     * 결제 후 남은 포인트
     */
    @Schema(description = "결제 후 남은 포인트", example = "5000")
    private BigDecimal remainingPoint;
    
    /**
     * 포인트 적립 금액
     */
    @Schema(description = "결제 금액의 1% 포인트 적립", example = "500")
    private BigDecimal pointReward;
    
    /**
     * 결제 상태
     */
    @Schema(description = "결제 상태", example = "COMPLETED")
    private String paymentStatus;
    
    /**
     * 결제 완료 시간
     */
    @Schema(description = "결제 완료 시간", example = "2025-08-19T16:30:00")
    private LocalDateTime completedAt;
}
