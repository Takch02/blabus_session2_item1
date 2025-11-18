package com.highlight.highlight_backend.admin.auction.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 경매 종료 요청 DTO
 * 
 * @author 전우선
 * @since 2025.08.13
 */
@Getter
@Setter
@NoArgsConstructor
public class AuctionEndRequestDto {
    
    /**
     * 즉시 종료 여부 (true: 즉시 종료, false: 정상 종료)
     */
    private boolean immediateEnd = false;
    
    /**
     * 종료 사유
     */
    private String endReason;
    
    /**
     * 경매 중단 여부 (true: 중단, false: 정상 종료)
     */
    private boolean isCancel = false;
}