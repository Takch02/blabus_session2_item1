package com.highlight.highlight_backend.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 결제 미리보기 DTO
 * 
 * @author 탁찬홍
 * @since 2025.08.21
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "결제 미리보기 DTO")
public class PaymentPreviewDto {
    
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
     * 사용자 보유 포인트
     */
    @Schema(description = "사용자 보유 포인트", example = "15000")
    private BigDecimal userPoint;
    
    /**
     * 사용 가능한 최대 포인트 (낙찰가와 보유 포인트 중 작은 값)
     */
    @Schema(description = "사용 가능한 최대 포인트", example = "15000")
    private BigDecimal maxUsablePoint;
    
    /**
     * 실제 결제될 금액 (총 금액 - 사용 포인트)
     */
    @Schema(description = "실제 결제될 금액", example = "48000")
    private BigDecimal actualPaymentAmount;
    
    /**
     * 포인트 사용 후 남을 포인트
     */
    @Schema(description = "포인트 사용 후 남을 포인트", example = "0")
    private BigDecimal remainingPointAfterPayment;
    
    @Schema(description = "상품 이미지 URL", example = "https://example.com/image.jpg")
    private String productImageUrl;
}
