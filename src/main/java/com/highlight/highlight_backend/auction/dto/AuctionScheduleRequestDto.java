package com.highlight.highlight_backend.auction.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 경매 예약 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class AuctionScheduleRequestDto {

    /**
     * 경매할 상품 ID
     */
    @NotNull(message = "상품 ID는 필수입니다")
    private Long productId;

    /**
     * 경매 시작가
     */
    @NotNull(message = "경매 시작가는 필수입니다.")
    @DecimalMin(value = "0", message = "경매 시작가는 0원 이상이어야 합니다.")
    private BigDecimal startPrice;

    /**
     * 입찰 단위
     */
    @NotNull(message = "경매 단위는 필수입니다.")
    @DecimalMin(value = "0", message = "경매 단위는 0원 이상이어야 합니다.")
    private BigDecimal bidUnit;

    /**
     * 최대 인상폭
     */
    @NotNull(message = "최대 인상폭은 필수입니다.")
    @DecimalMin(value = "0", message = "최대 인상폭은 0원 이상이어야 합니다.")
    private BigDecimal maxBid;

    /**
     * 최소 인상폭
     */
    @NotNull(message = "최소 인상폭은 필수입니다.")
    @DecimalMin(value = "1000", message = "최소 인상폭은 1000원 이상이어야 합니다.")
    private BigDecimal minimumBid;

    /**
     * 즉시구매가 (선택사항)
     */
    @DecimalMin(value = "0", message = "즉시구매가는 0원 이상이어야 합니다")
    @DecimalMax(value = "999999999999999", message = "즉시구매가가 너무 큽니다")
    private BigDecimal buyItNowPrice;

    /**
     * 배송비 (선택사항, 기본값 0)
     */
    @DecimalMin(value = "0", message = "배송비는 0원 이상이어야 합니다")
    private BigDecimal shippingFee = BigDecimal.ZERO;

    /**
     * 직접 픽업 가능 여부
     */
    @NotNull(message = "직접 픽업 가능 여부는 필수입니다.")
    private Boolean isPickupAvailable;

    /**
     * 경매 시작 예정 시간
     */
    @NotNull(message = "경매 시작 시간은 필수입니다")
    private LocalDateTime scheduledStartTime;

    /**
     * 경매 종료 예정 시간
     */
    @NotNull(message = "경매 종료 시간은 필수입니다")
    private LocalDateTime scheduledEndTime;

    /**
     * 경매 설명/메모
     */
    @Size(max = 500, message = "경매 설명은 500자를 초과할 수 없습니다")
    private String description;
}