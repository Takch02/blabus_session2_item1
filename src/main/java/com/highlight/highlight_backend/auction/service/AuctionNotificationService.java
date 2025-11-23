package com.highlight.highlight_backend.auction.service;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.common.socket.service.GlobalSocketService;
import com.highlight.highlight_backend.common.socket.dto.WebSocketMessageDto;
import com.highlight.highlight_backend.exception.CommonErrorCode;
import com.highlight.highlight_backend.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuctionNotificationService {

    private final GlobalSocketService globalSocketService;

    /**
     * 개인 알림 전송
     *
     * @param userId 사용자 ID
     * @param message 알림 메시지
     * @param auctionId 관련 경매 ID
     */
    public void sendPersonalNotification(Long userId, String message, Long auctionId) {
        // log.info("WebSocket - 개인 알림 전송: 사용자={}, 메시지={}", userId, message);

        WebSocketMessageDto notification = WebSocketMessageDto.of(
                WebSocketMessageDto.WebSocketMessageType.BID_OUTBID,
                auctionId,
                message
        );

        // 특정 사용자에게만 전송
        globalSocketService.sendToUser(userId, notification);

        // log.info("WebSocket - 개인 알림 전송 완료: {}", destination);
    }

    /**
     * 연결 성공 확인 메시지
     *
     * @param auctionId 경매 ID
     */
    public void sendConnectionEstablished(Long auctionId) {
        try {
            // log.info("WebSocket - 연결 성공 메시지 전송: 경매={}", auctionId);

            WebSocketMessageDto message = WebSocketMessageDto.of(
                    WebSocketMessageDto.WebSocketMessageType.CONNECTION_ESTABLISHED,
                    auctionId,
                    "WebSocket 연결이 성공적으로 설정되었습니다."
            );

            String destination = "/topic/auction/" + auctionId;
            globalSocketService.sendToTopic(destination, message);
        } catch (Exception e) {
            log.error("WebSocket - 연결 성공 메시지 전송 실패: 경매={}, 에러={}", auctionId, e.getMessage());
            sendErrorMessage(auctionId, CommonErrorCode.WEBSOCKET_CONNECTION_FAILED.getMessage());
        }
    }

    /**
     * 에러 메시지 전송
     *
     * @param auctionId 경매 ID
     * @param errorMessage 에러 메시지
     */
    public void sendErrorMessage(Long auctionId, String errorMessage) {
        try {
            log.error("WebSocket - 에러 메시지 전송: 경매={}, 에러={}", auctionId, errorMessage);

            WebSocketMessageDto message = WebSocketMessageDto.of(
                    WebSocketMessageDto.WebSocketMessageType.ERROR,
                    auctionId,
                    errorMessage
            );

            String destination = "/topic/auction/" + auctionId;
            globalSocketService.sendToTopic(destination, message);
        } catch (Exception e) {
            log.error("WebSocket - 에러 메시지 전송 실패: 경매={}, 원본에러={}, 전송에러={}",
                    auctionId, errorMessage, e.getMessage());
        }
    }

    /**
     * 경매 시작 알림
     *
     * @param auction 시작된 경매
     */
    public void sendAuctionStartedNotification(Auction auction) {
        Long auctionId = auction.getId();
        // log.info("WebSocket - 경매 시작 알림 전송: 경매={}", auctionId);

        WebSocketMessageDto message = WebSocketMessageDto.of(
                WebSocketMessageDto.WebSocketMessageType.AUCTION_STARTED,
                auctionId,
                "경매가 시작되었습니다."
        );

        String destination = "/topic/auction/" + auctionId;
        globalSocketService.sendToTopic(destination, message);

        // 전체 경매 목록 구독자에게도 알림
        globalSocketService.sendToTopic("/topic/auctions", message);

        // log.info("WebSocket - 경매 시작 알림 전송 완료");
    }

    /**
     * 경매 종료 임박 알림 (1분 이내)
     *
     * @param auction 종료 임박 경매
     * @param remainingSeconds 남은 시간 (초)
     */
    public void sendEndingSoonAlert(Auction auction, long remainingSeconds) {
        Long auctionId = auction.getId();
        // log.info("WebSocket - 경매 종료 임박 알림 전송: 경매={}, 남은시간={}초", auctionId, remainingSeconds);

        String alertMessage = String.format("경매가 %d초 후 종료됩니다!", remainingSeconds);

        WebSocketMessageDto message = WebSocketMessageDto.of(
                WebSocketMessageDto.WebSocketMessageType.AUCTION_ENDING_SOON,
                auctionId,
                alertMessage
        );

        String destination = "/topic/auction/" + auctionId;
        globalSocketService.sendToTopic(destination, message);

        // log.info("WebSocket - 경매 종료 임박 알림 전송 완료: {}", destination);
    }



    /**
     * 경매 취소 알림 전송
     *
     * @param auction 취소된 경매
     */
    public void sendAuctionCancelledNotification(Auction auction) {
        Long auctionId = auction.getId();
        // log.info("WebSocket - 경매 취소 알림 전송: 경매={}", auctionId);

        WebSocketMessageDto message = WebSocketMessageDto.of(
                WebSocketMessageDto.WebSocketMessageType.AUCTION_CANCELLED,
                auctionId,
                "경매가 취소되었습니다"
        );

        String destination = "/topic/auction/" + auctionId;
        globalSocketService.sendToTopic(destination, message);

        // log.info("WebSocket - 경매 취소 알림 전송 완료: {}", destination);
    }


    /**
     * 연결 끊김 알림 전송
     *
     * @param auctionId 경매 ID
     */
    public void sendConnectionLostNotification(Long auctionId) {
        // log.info("WebSocket - 연결 끊김 알림 전송: 경매={}", auctionId);

        WebSocketMessageDto message = WebSocketMessageDto.of(
                WebSocketMessageDto.WebSocketMessageType.CONNECTION_LOST,
                auctionId,
                "연결이 끊어졌어요"
        );

        String destination = "/topic/auction/" + auctionId;
        globalSocketService.sendToTopic(destination, message);

        // log.info("WebSocket - 연결 끊김 알림 전송 완료: {}", destination);
    }

    /**
     * 에러코드 기반 에러 메시지 전송
     *
     * @param auctionId 경매 ID
     * @param errorCode 에러 코드
     */
    public void sendErrorMessage(Long auctionId, ErrorCode errorCode) {
        sendErrorMessage(auctionId, errorCode.getMessage());
    }

    /**
     * 결제 필요 알림 전송
     *
     * @param auctionId 경매 ID
     * @param winningBidAmount 낙찰가
     */
    public void sendPaymentRequiredNotification(Long auctionId, BigDecimal winningBidAmount) {
        // log.info("WebSocket - 결제 필요 알림 전송: 경매={}, 낙찰가={}", auctionId, winningBidAmount);

        String message = String.format("축하합니다! %s원에 낙찰되었습니다. 결제를 진행해주세요.", winningBidAmount);

        WebSocketMessageDto webSocketMessage = WebSocketMessageDto.of(
                WebSocketMessageDto.WebSocketMessageType.PAYMENT_REQUIRED,
                auctionId,
                message
        );

        String destination = "/topic/auction/" + auctionId;
        globalSocketService.sendToTopic(destination, webSocketMessage);

        // log.info("WebSocket - 결제 필요 알림 전송 완료: {}", destination);
    }

    /**
     * 결제 완료 알림 전송
     *
     * @param auctionId 경매 ID
     * @param paymentAmount 결제 금액
     */
    public void sendPaymentCompletedNotification(Long auctionId, BigDecimal paymentAmount) {
        // log.info("WebSocket - 결제 완료 알림 전송: 경매={}, 결제금액={}", auctionId, paymentAmount);

        String message = String.format("결제가 완료되었습니다! 결제 금액: %s원", paymentAmount);

        WebSocketMessageDto webSocketMessage = WebSocketMessageDto.of(
                WebSocketMessageDto.WebSocketMessageType.PAYMENT_COMPLETED,
                auctionId,
                message
        );

        String destination = "/topic/auction/" + auctionId;
        globalSocketService.sendToTopic(destination, webSocketMessage);

        // log.info("WebSocket - 결제 완료 알림 전송 완료: {}", destination);
    }

    /**
     * 즉시 구매 완료 알림 전송
     *
     * @param auctionId 경매 ID
     * @param buyItNowPrice 즉시 구매가
     */
    public void sendBuyItNowCompletedNotification(Long auctionId, BigDecimal buyItNowPrice) {
        // log.info("WebSocket - 즉시 구매 완료 알림 전송: 경매={}, 즉시구매가={}", auctionId, buyItNowPrice);

        String message = String.format("즉시 구매가 완료되었습니다! 구매 금액: %s원", buyItNowPrice);

        WebSocketMessageDto webSocketMessage = WebSocketMessageDto.of(
                WebSocketMessageDto.WebSocketMessageType.BUY_IT_NOW_COMPLETED,
                auctionId,
                message
        );

        String destination = "/topic/auction/" + auctionId;
        globalSocketService.sendToTopic(destination, webSocketMessage);

        // log.info("WebSocket - 즉시 구매 완료 알림 전송 완료: {}", destination);
    }

    /**
     * 경매 승리 시 알림 발송
     */
    public void notifyAuctionEnded(Auction auction, String winnerNickname) {
        Long auctionId = auction.getId();

        // 1. 메시지 생성 (낙찰자 이름이 있으면 포함, 없으면 없음)
        String endMessage = (winnerNickname != null) ?
                "경매가 종료되었습니다. 낙찰자: " + winnerNickname :
                "경매가 종료되었습니다. (낙찰자 없음)";

        WebSocketMessageDto message = WebSocketMessageDto.of(
                WebSocketMessageDto.WebSocketMessageType.AUCTION_ENDED,
                auctionId,
                endMessage
        );

        // 2. 공개 채널에 방송 (/topic/...)
        globalSocketService.sendToTopic("/topic/auction/" + auctionId, message);
        globalSocketService.sendToTopic("/topic/auctions", message);
    }
}
