package com.highlight.highlight_backend.bid.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 입찰 참여 요청 DTO
 * 
 * @author 전우선
 * @since 2025.08.15
 */
@Getter
@NoArgsConstructor
@Schema(description = "입찰 참여 요청 DTO")
public class BidCreateRequestDto {
    
    /**
     * 경매 ID
     */
    @NotNull(message = "경매 ID는 필수입니다")
    @Schema(description = "경매 ID", example = "1", required = true)
    private Long auctionId;
    
    /**
     * 입찰 금액
     */
    @NotNull(message = "입찰 금액은 필수입니다")
    @DecimalMin(value = "1000", message = "입찰 금액은 최소 1,000원 이상이어야 합니다")
    @Schema(description = "입찰 금액", example = "50000", required = true)
    private BigDecimal bidAmount;
    
    /**
     * 자동 입찰 여부
     */
    @Schema(description = "자동 입찰 여부", example = "false", defaultValue = "false")
    private Boolean isAutoBid = false;
    
    /**
     * 자동 입찰 최대 금액 (자동 입찰인 경우 필수)
     */
    @DecimalMin(value = "1000", message = "자동 입찰 최대 금액은 최소 1,000원 이상이어야 합니다")
    @Schema(description = "자동 입찰 최대 금액 (자동 입찰인 경우 필수)", example = "100000")
    private BigDecimal maxAutoBidAmount;
}