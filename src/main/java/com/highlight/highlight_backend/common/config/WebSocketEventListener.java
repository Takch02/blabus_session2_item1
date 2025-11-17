package com.highlight.highlight_backend.common.config;

import com.highlight.highlight_backend.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * WebSocket 연결/해제 이벤트 리스너
 * 
 * 클라이언트의 WebSocket 연결 상태를 감지하고 적절한 알림을 전송합니다.
 * 
 * @author 전우선
 * @since 2025.08.16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {
    
    private final WebSocketService webSocketService;
    
    /**
     * WebSocket 연결 성공 이벤트 처리
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        log.info("WebSocket 연결 성공 - 세션ID: {}", sessionId);
        log.info("WebSocket 연결 헤더 정보: {}", headerAccessor.toNativeHeaderMap());
        
        // 연결 성공 메시지는 개별 경매별로 전송하므로 여기서는 로깅만 수행
    }
    
    /**
     * WebSocket 연결 해제 이벤트 처리
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        log.info("WebSocket 연결 해제 - 세션ID: {}", sessionId);
        
        // 연결 해제 시 특정 경매 ID를 알기 어려우므로
        // 실제 연결 끊김 감지는 클라이언트 측 하트비트나 핑퐁으로 처리하는 것이 좋습니다.
        // 여기서는 연결 해제 로깅만 수행합니다.
        
        // 필요시 사용자별 활성 세션 관리 로직 추가 가능
        // 예: sessionId로 해당 사용자가 구독 중인 경매 찾아서 연결 끊김 알림 전송
    }
    
    /**
     * 특정 경매의 연결 끊김 알림 전송
     * 
     * @param auctionId 경매 ID
     */
    public void notifyConnectionLost(Long auctionId) {
        webSocketService.sendConnectionLostNotification(auctionId);
    }
}