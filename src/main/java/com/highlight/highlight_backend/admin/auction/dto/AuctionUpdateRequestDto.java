package com.highlight.highlight_backend.admin.auction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 경매 수정 요청 DTO
 * 
 * @author 전우선
 * @since 2025.08.18
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "경매 수정 요청 DTO")
public class AuctionUpdateRequestDto {

    /**
     * 상품 ID (선택사항)
     */
    @Schema(description = "상품 ID", example = "1")
    @Min(value = 1, message = "상품 ID는 1 이상이어야 합니다.")
    private Long productId;

    /**
     * 경매 시작 예정 시간 (선택사항)
     */
    @Schema(description = "경매 시작 예정 시간", example = "2025-08-20T10:00:00")
    @Future(message = "시작 시간은 미래 시간이어야 합니다.")
    private LocalDateTime scheduledStartTime;

    /**
     * 경매 종료 예정 시간 (선택사항)
     */
    @Schema(description = "경매 종료 예정 시간", example = "2025-08-20T18:00:00")
    @Future(message = "종료 시간은 미래 시간이어야 합니다.")
    private LocalDateTime scheduledEndTime;

    /**
     * 시작가 (선택사항)
     */
    @Schema(description = "시작가", example = "50000")
    @DecimalMin(value = "0.0", inclusive = false, message = "시작가는 0보다 커야 합니다.")
    private BigDecimal startPrice;

    /**
     * 입찰 단위 (선택사항)
     */
    @Schema(description = "입찰 단위", example = "1000")
    @DecimalMin(value = "0.0", inclusive = false, message = "입찰 단위는 0보다 커야 합니다.")
    private BigDecimal bidUnit;

    /**
     * 최대 입찰가 (선택사항)
     */
    @Schema(description = "최대 입찰가", example = "1000000")
    @DecimalMin(value = "0.0", inclusive = false, message = "최대 입찰가는 0보다 커야 합니다.")
    private BigDecimal maxBid;

    /**
     * 최소 입찰가 (선택사항)
     */
    @Schema(description = "최소 입찰가", example = "1000")
    @DecimalMin(value = "0.0", inclusive = false, message = "최소 입찰가는 0보다 커야 합니다.")
    private BigDecimal minimumBid;

    /**
     * 즉시구매가 (선택사항, null 가능)
     */
    @Schema(description = "즉시구매가", example = "100000")
    @DecimalMin(value = "0.0", inclusive = false, message = "즉시구매가는 0보다 커야 합니다.")
    private BigDecimal buyItNowPrice;

    /**
     * 배송비 (선택사항)
     */
    @Schema(description = "배송비", example = "3000")
    @DecimalMin(value = "0.0", inclusive = true, message = "배송비는 0원 이상이어야 합니다.")
    private BigDecimal shippingFee;

    /**
     * 픽업 가능 여부 (선택사항)
     */
    @Schema(description = "픽업 가능 여부", example = "true")
    private Boolean isPickupAvailable;

    /**
     * 경매 설명/메모 (선택사항)
     */
    @Schema(description = "경매 설명/메모", example = "특별한 경매입니다.")
    private String description;
}
