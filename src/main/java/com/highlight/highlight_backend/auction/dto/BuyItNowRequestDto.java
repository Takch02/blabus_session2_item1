package com.highlight.highlight_backend.auction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 즉시 구매 요청 DTO
 * 
 * @author 탁찬홍
 * @since 2025.08.21
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "즉시 구매 요청 DTO")
public class BuyItNowRequestDto {
    
    /**
     * 경매 ID
     */
    @NotNull(message = "경매 ID는 필수입니다.")
    @Schema(description = "경매 ID", example = "1")
    private Long auctionId;
    
    /**
     * 사용할 포인트 금액
     */
    @Schema(description = "사용할 포인트 금액 (0 이상)", example = "10000")
    @Builder.Default
    private BigDecimal usePointAmount = BigDecimal.valueOf(0);
}