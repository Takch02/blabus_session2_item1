package com.highlight.highlight_backend.common.socket.service;

import com.highlight.highlight_backend.common.socket.dto.WebSocketMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class GlobalSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 공통 전송 메소드 (토픽)
     */
    public void sendToTopic(String destination, WebSocketMessageDto message) {
        try {
            messagingTemplate.convertAndSend(destination, message);
        } catch (Exception e) {
            log.error("WebSocket 전송 실패: destination={}, error={}", destination, e.getMessage());
            // 필요하다면 여기서 공통 에러 처리를 하거나 예외를 던짐
        }
    }

    /**
     * 개인 전송 메소드 (큐)
     */
    public void sendToUser(Long userId, WebSocketMessageDto message) {
        String destination = "/queue/user/" + userId + "/notifications";
        try {
            messagingTemplate.convertAndSend(destination, message);
        } catch (Exception e) {
            log.error("개인 알림 전송 실패: user={}, error={}", userId, e.getMessage());
        }
    }
}
