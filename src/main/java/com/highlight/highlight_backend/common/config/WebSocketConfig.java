package com.highlight.highlight_backend.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 설정
 * 
 * 실시간 경매 정보 전송을 위한 WebSocket 설정을 담당합니다.
 * STOMP 프로토콜을 사용하여 메시지 브로커 패턴으로 구현됩니다.
 * 
 * @author 전우선
 * @since 2025.08.15
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 메시지 브로커 설정
     * 
     * /topic - 1:N 브로드캐스트 (경매 실시간 정보)
     * /queue - 1:1 개인 메시지 (개인 알림)
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 클라이언트가 구독할 경로 prefix
        config.enableSimpleBroker("/topic", "/queue");
        
        // 클라이언트가 서버로 메시지 보낼 때 사용할 prefix
        config.setApplicationDestinationPrefixes("/app");
        
        // 개인 메시지를 위한 prefix
        config.setUserDestinationPrefix("/user");
    }

    /**
     * WebSocket 엔드포인트 등록
     * 
     * 클라이언트가 WebSocket에 연결할 때 사용하는 엔드포인트
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 엔드포인트 등록
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                    "http://localhost:3000",        // React 개발 서버
                    "http://localhost:3001",        // Next.js 개발 서버  
                    "http://127.0.0.1:3000",        // 로컬호스트 별칭
                    "http://ec2-52-78-128-131.ap-northeast-2.compute.amazonaws.com:3000",   // 프론트엔드 배포 서버
                    "http://ec2-52-78-128-131.ap-northeast-2.compute.amazonaws.com",        // 프론트엔드 배포 서버 (포트 없이)
                    "http://ec2-43-201-71-156.ap-northeast-2.compute.amazonaws.com:8085",   // 이전 백엔드 서버
                    "http://ec2-52-78-128-131.ap-northeast-2.compute.amazonaws.com:8085",   // 현재 백엔드 서버
                    "http://ec2-52-78-128-131.ap-northeast-2.compute.amazonaws.com",        // 현재 백엔드 서버 (포트 없이)
                    "http://*.amazonaws.com",       // AWS 배포 (HTTP만)
                    "http://*.execute-api.*.amazonaws.com"  // API Gateway (HTTP만)
                )
                .withSockJS();                  // SockJS fallback 지원
        
        // 경매별 전용 엔드포인트
        registry.addEndpoint("/ws/auctions")
                .setAllowedOriginPatterns(
                    "http://localhost:3000",
                    "http://localhost:3001", 
                    "http://127.0.0.1:3000",
                    "http://ec2-52-78-128-131.ap-northeast-2.compute.amazonaws.com:3000",   // 프론트엔드 배포 서버
                    "http://ec2-52-78-128-131.ap-northeast-2.compute.amazonaws.com",        // 프론트엔드 배포 서버 (포트 없이)
                    "http://ec2-52-78-128-131.ap-northeast-2.compute.amazonaws.com:8085",   // 현재 백엔드 서버
                    "http://ec2-52-78-128-131.ap-northeast-2.compute.amazonaws.com",        // 현재 백엔드 서버 (포트 없이)
                    "http://*.amazonaws.com",       // AWS 배포 (HTTP만)
                    "http://*.execute-api.*.amazonaws.com"  // API Gateway (HTTP만)
                )
                .withSockJS();
                
        // WebSocket만을 위한 추가 엔드포인트 (SockJS 없이)
        registry.addEndpoint("/ws/direct")
                .setAllowedOriginPatterns(
                    "http://localhost:3000",
                    "http://localhost:3001", 
                    "http://127.0.0.1:3000",
                    "http://ec2-52-78-128-131.ap-northeast-2.compute.amazonaws.com:3000",   // 프론트엔드 배포 서버
                    "http://ec2-52-78-128-131.ap-northeast-2.compute.amazonaws.com",        // 프론트엔드 배포 서버 (포트 없이)
                    "http://ec2-52-78-128-131.ap-northeast-2.compute.amazonaws.com:8085",   // 현재 백엔드 서버
                    "http://ec2-52-78-128-131.ap-northeast-2.compute.amazonaws.com",        // 현재 백엔드 서버 (포트 없이)
                    "http://*.amazonaws.com",       // AWS 배포 (HTTP만)
                    "http://*.execute-api.*.amazonaws.com"  // API Gateway (HTTP만)
                );
    }
}