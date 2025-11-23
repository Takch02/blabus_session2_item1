package com.highlight.highlight_backend.common.socket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * WebSocket 메시지 기본 DTO
 * 
 * @author 전우선
 * @since 2025.08.15
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "WebSocket 메시지 기본 DTO")
public class WebSocketMessageDto {
    
    /**
     * 메시지 타입
     */
    @Schema(description = "메시지 타입", example = "NEW_BID")
    private WebSocketMessageType type;
    
    /**
     * 경매 ID
     */
    @Schema(description = "경매 ID", example = "1")
    private Long auctionId;
    
    /**
     * 메시지 데이터 (JSON)
     */
    @Schema(description = "메시지 데이터")
    private Object data;
    
    /**
     * 메시지 전송 시간
     */
    @Schema(description = "메시지 전송 시간", example = "2025-08-15T14:30:00")
    private LocalDateTime timestamp;
    
    /**
     * 메시지 생성자
     */
    public static WebSocketMessageDto of(WebSocketMessageType type, Long auctionId, Object data) {
        return new WebSocketMessageDto(type, auctionId, data, LocalDateTime.now());
    }
    
    /**
     * WebSocket 메시지 타입
     */
    @Schema(description = "WebSocket 메시지 타입")
    public enum WebSocketMessageType {
        NEW_BID("새로운 입찰"),                    // 새 입찰 발생
        AUCTION_STATUS_UPDATE("경매 상태 업데이트"), // 경매 상태 변경  
        AUCTION_STARTED("경매 시작"),              // 경매 시작
        AUCTION_ENDED("경매 종료"),                // 경매 종료
        AUCTION_CANCELLED("경매 취소"),            // 경매 취소
        AUCTION_UPDATED("경매 정보 수정"),          // 경매 정보 수정 알림
        AUCTION_ENDING_SOON("경매 종료 임박"),      // 경매 종료 임박 (1분 이내)
        BID_OUTBID("입찰 경합 패배"),              // 내 입찰이 다른 입찰에 밀림
        PAYMENT_REQUIRED("결제 필요"),              // 낙찰 후 결제 필요 알림
        PAYMENT_COMPLETED("결제 완료"),             // 결제 완료 알림
        BUY_IT_NOW_COMPLETED("즉시 구매 완료"),     // 즉시 구매 완료 알림
        CONNECTION_ESTABLISHED("연결 성공"),
        WIN_BID("경매 승리"),// WebSocket 연결 성공
        CONNECTION_LOST("연결 끊김"),              // WebSocket 연결 끊김
        ERROR("오류");                            // 오류 메시지
        
        private final String description;
        
        WebSocketMessageType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}