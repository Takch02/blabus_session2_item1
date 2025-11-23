package com.highlight.highlight_backend.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 결제 요청 DTO
 * 
 * @author 탁찬홍
 * @since 2025.08.21
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "결제 요청 DTO")
public class PaymentRequestDto {
    
    /**
     * 경매 ID
     */
    @Schema(description = "경매 ID", example = "1", required = true)
    private Long auctionId;
    
    /**
     * 사용할 포인트 금액
     */
    @Schema(description = "사용할 포인트 금액", example = "10000", required = true)
    private BigDecimal usePointAmount;
    
    /**
     * 실제 결제할 금액 (낙찰가 - 사용 포인트)
     */
    @Schema(description = "실제 결제할 금액", example = "50000", required = true)
    private BigDecimal actualPaymentAmount;
}
