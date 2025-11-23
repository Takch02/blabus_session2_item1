package com.highlight.highlight_backend.auction.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 경매 시작 요청 DTO
 *
 */
@Getter
@Setter
@NoArgsConstructor
public class AuctionStartRequestDto {
    
    /**
     * 즉시 시작 여부 (true: 즉시 시작, false: 시간 입력)
     */
    private boolean immediateStart = false;
    
    /**
     * 경매 시작 시간 (immediateStart가 false일 때 사용)
     */
    private LocalDateTime scheduledStartTime;
    
    /**
     * 경매 종료 시간 (immediateStart가 false일 때 사용)
     */
    private LocalDateTime scheduledEndTime;
}