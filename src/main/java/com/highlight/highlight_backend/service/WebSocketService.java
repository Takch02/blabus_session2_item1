package com.highlight.highlight_backend.service;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.bid.domain.Bid;
import com.highlight.highlight_backend.dto.websocket.AuctionStatusWebSocketDto;
import com.highlight.highlight_backend.dto.websocket.BidOutbidNotificationDto;
import com.highlight.highlight_backend.dto.websocket.BidWebSocketDto;
import com.highlight.highlight_backend.dto.websocket.WebSocketMessageDto;
import com.highlight.highlight_backend.dto.websocket.WebSocketMessageDto.WebSocketMessageType;
import com.highlight.highlight_backend.exception.CommonErrorCode;
import com.highlight.highlight_backend.exception.ErrorCode;
import com.highlight.highlight_backend.bid.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * WebSocket 메시지 전송 서비스
 * 
 * 경매 관련 실시간 메시지를 클라이언트에게 전송하는 서비스입니다.
 * 
 * @author 전우선
 * @since 2025.08.15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final BidRepository bidRepository;
    
    /**
     * 새로운 입찰 발생 알림
     * 
     * @param bid 새로운 입찰 정보
     */
    public void sendNewBidNotification(Bid bid) {
        try {
            Long auctionId = bid.getAuction().getId();
            // log.info("WebSocket - 새 입찰 알림 전송: 경매={}, 입찰자={}, 금액={}", 
            //         auctionId, bid.getUser().getNickname(), bid.getBidAmount());
            
            // 입찰 통계 조회
            Long totalBidders = bidRepository.countDistinctBiddersByAuction(bid.getAuction());
            Long totalBids = bidRepository.countBidsByAuction(bid.getAuction());
            
            // WebSocket 메시지 데이터 생성
            BidWebSocketDto bidData = BidWebSocketDto.from(bid, totalBidders, totalBids);
            WebSocketMessageDto message = WebSocketMessageDto.of(
                WebSocketMessageType.NEW_BID, 
                auctionId, 
                bidData
            );
            
            // 해당 경매를 구독하고 있는 모든 클라이언트에게 브로드캐스트
            String destination = "/topic/auction/" + auctionId;
            messagingTemplate.convertAndSend(destination, message);
            
            // log.info("WebSocket - 새 입찰 알림 전송 완료: {}", destination);
        } catch (Exception e) {
            log.error("WebSocket - 새 입찰 알림 전송 실패: {}", e.getMessage(), e);
            sendErrorMessage(bid.getAuction().getId(), CommonErrorCode.WEBSOCKET_MESSAGE_SEND_FAILED.getMessage());
        }
    }
    
    /**
     * 경매 상태 업데이트 알림
     * 
     * @param auction 경매 정보
     */
    public void sendAuctionStatusUpdate(Auction auction) {
        Long auctionId = auction.getId();
        // log.info("WebSocket - 경매 상태 업데이트 전송: 경매={}, 상태={}", 
        //         auctionId, auction.getStatus());
        
        // 입찰 통계 조회
        Long totalBidders = bidRepository.countDistinctBiddersByAuction(auction);
        Long totalBids = bidRepository.countBidsByAuction(auction);
        
        // 현재 최고 입찰자 조회
        String winnerNickname = null;
        Optional<Bid> currentWinner = bidRepository.findCurrentHighestBidByAuction(auction);
        if (currentWinner.isPresent()) {
            winnerNickname = currentWinner.get().getUser().getNickname();
        }
        
        // WebSocket 메시지 데이터 생성
        AuctionStatusWebSocketDto statusData = AuctionStatusWebSocketDto.from(
            auction, totalBidders, totalBids, winnerNickname
        );
        WebSocketMessageDto message = WebSocketMessageDto.of(
            WebSocketMessageType.AUCTION_STATUS_UPDATE, 
            auctionId, 
            statusData
        );
        
        // 브로드캐스트
        String destination = "/topic/auction/" + auctionId;
        messagingTemplate.convertAndSend(destination, message);
        
        // log.info("WebSocket - 경매 상태 업데이트 전송 완료: {}", destination);
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
            WebSocketMessageType.AUCTION_STARTED, 
            auctionId, 
            "경매가 시작되었습니다."
        );
        
        String destination = "/topic/auction/" + auctionId;
        messagingTemplate.convertAndSend(destination, message);
        
        // 전체 경매 목록 구독자에게도 알림
        messagingTemplate.convertAndSend("/topic/auctions", message);
        
        // log.info("WebSocket - 경매 시작 알림 전송 완료");
    }
    
    /**
     * 경매 종료 알림
     * 
     * @param auction 종료된 경매
     * @param winnerBid 낙찰된 입찰 (없을 수 있음)
     */
    public void sendAuctionEndedNotification(Auction auction, Bid winnerBid) {
        Long auctionId = auction.getId();
        // log.info("WebSocket - 경매 종료 알림 전송: 경매={}, 낙찰자={}", 
        //         auctionId, winnerBid != null ? winnerBid.getUser().getNickname() : "없음");
        
        String endMessage = winnerBid != null ? 
            "경매가 종료되었습니다. 낙찰자: " + winnerBid.getUser().getNickname() :
            "경매가 종료되었습니다. (낙찰자 없음)";
        
        WebSocketMessageDto message = WebSocketMessageDto.of(
            WebSocketMessageType.AUCTION_ENDED, 
            auctionId, 
            endMessage
        );
        
        String destination = "/topic/auction/" + auctionId;
        messagingTemplate.convertAndSend(destination, message);
        
        // 전체 경매 목록 구독자에게도 알림
        messagingTemplate.convertAndSend("/topic/auctions", message);
        
        // 낙찰자에게 개인 알림 (있는 경우)
        if (winnerBid != null) {
            sendPersonalNotification(
                winnerBid.getUser().getId(), 
                "축하합니다! 경매에서 낙찰받으셨습니다.",
                auctionId
            );
        }
        
        // log.info("WebSocket - 경매 종료 알림 전송 완료");
    }
    
    /**
     * 입찰 경합 패배 알림 (개인 알림) - 강화된 버전
     * 
     * @param outbidBid 밀려난 입찰
     * @param newBid 새로운 최고 입찰
     */
    public void sendBidOutbidNotification(Bid outbidBid, Bid newBid) {
        // log.info("WebSocket - 입찰 경합 패배 알림 전송: 사용자={}, 경매={}", 
        //         outbidBid.getUser().getId(), outbidBid.getAuction().getId());
        
        try {
            // 연속 패배 횟수 조회
            Long consecutiveLosses = bidRepository.countConsecutiveLossesByUserAndAuction(
                outbidBid.getUser(), outbidBid.getAuction());
            
            // 입찰 통계 조회
            Long totalBidders = bidRepository.countDistinctBiddersByAuction(outbidBid.getAuction());
            Long totalBids = bidRepository.countBidsByAuction(outbidBid.getAuction());
            
            // 강화된 알림 데이터 생성
            BidOutbidNotificationDto notificationData = BidOutbidNotificationDto.from(
                outbidBid, newBid, consecutiveLosses.intValue(), totalBidders, totalBids);
            
            WebSocketMessageDto message = WebSocketMessageDto.of(
                WebSocketMessageType.BID_OUTBID, 
                outbidBid.getAuction().getId(), 
                notificationData
            );
            
            // 특정 사용자에게만 전송
            String destination = "/queue/user/" + outbidBid.getUser().getId() + "/notifications";
            messagingTemplate.convertAndSend(destination, message);
            
            // log.info("WebSocket - 강화된 입찰 경합 패배 알림 전송 완료: 사용자={}, 연속패배={}", 
            //         outbidBid.getUser().getId(), consecutiveLosses);
            
        } catch (Exception e) {
            log.error("WebSocket - 강화된 입찰 경합 패배 알림 전송 실패: {}", e.getMessage(), e);
            // 기본 알림으로 폴백
            sendBasicOutbidNotification(outbidBid, newBid);
        }
    }
    
    /**
     * 기본 입찰 경합 패배 알림 (폴백용)
     */
    private void sendBasicOutbidNotification(Bid outbidBid, Bid newBid) {
        String outbidMessage = String.format(
            "다른 입찰자가 %,d원으로 입찰하여 귀하의 입찰이 밀렸습니다.",
            newBid.getBidAmount().longValue()
        );
        
        sendPersonalNotification(
            outbidBid.getUser().getId(), 
            outbidMessage,
            outbidBid.getAuction().getId()
        );
    }
    
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
            WebSocketMessageType.BID_OUTBID, 
            auctionId, 
            message
        );
        
        // 특정 사용자에게만 전송
        String destination = "/queue/user/" + userId + "/notifications";
        messagingTemplate.convertAndSend(destination, notification);
        
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
                WebSocketMessageType.CONNECTION_ESTABLISHED, 
                auctionId, 
                "WebSocket 연결이 성공적으로 설정되었습니다."
            );
            
            String destination = "/topic/auction/" + auctionId;
            messagingTemplate.convertAndSend(destination, message);
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
                WebSocketMessageType.ERROR, 
                auctionId, 
                errorMessage
            );
            
            String destination = "/topic/auction/" + auctionId;
            messagingTemplate.convertAndSend(destination, message);
        } catch (Exception e) {
            log.error("WebSocket - 에러 메시지 전송 실패: 경매={}, 원본에러={}, 전송에러={}", 
                    auctionId, errorMessage, e.getMessage());
        }
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
            WebSocketMessageType.AUCTION_ENDING_SOON, 
            auctionId, 
            alertMessage
        );
        
        String destination = "/topic/auction/" + auctionId;
        messagingTemplate.convertAndSend(destination, message);
        
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
            WebSocketMessageType.AUCTION_CANCELLED, 
            auctionId, 
            "경매가 취소되었습니다"
        );
        
        String destination = "/topic/auction/" + auctionId;
        messagingTemplate.convertAndSend(destination, message);
        
        // log.info("WebSocket - 경매 취소 알림 전송 완료: {}", destination);
    }
    
    /**
     * 경매 정보 수정 알림 전송
     * 
     * @param auction 수정된 경매
     */
    public void sendAuctionUpdatedNotification(Auction auction) {
        Long auctionId = auction.getId();
        // log.info("WebSocket - 경매 정보 수정 알림 전송: 경매={}", auctionId);
        
        WebSocketMessageDto message = WebSocketMessageDto.of(
            WebSocketMessageType.AUCTION_UPDATED, 
            auctionId, 
            "경매 정보가 수정되었습니다"
        );
        
        String destination = "/topic/auction/" + auctionId;
        messagingTemplate.convertAndSend(destination, message);
        
        // log.info("WebSocket - 경매 정보 수정 알림 전송 완료: {}", destination);
    }
    
    /**
     * 연결 끊김 알림 전송
     * 
     * @param auctionId 경매 ID
     */
    public void sendConnectionLostNotification(Long auctionId) {
        // log.info("WebSocket - 연결 끊김 알림 전송: 경매={}", auctionId);
        
        WebSocketMessageDto message = WebSocketMessageDto.of(
            WebSocketMessageType.CONNECTION_LOST, 
            auctionId, 
            "연결이 끊어졌어요"
        );
        
        String destination = "/topic/auction/" + auctionId;
        messagingTemplate.convertAndSend(destination, message);
        
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
            WebSocketMessageType.PAYMENT_REQUIRED, 
            auctionId, 
            message
        );
        
        String destination = "/topic/auction/" + auctionId;
        messagingTemplate.convertAndSend(destination, webSocketMessage);
        
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
            WebSocketMessageType.PAYMENT_COMPLETED, 
            auctionId, 
            message
        );
        
        String destination = "/topic/auction/" + auctionId;
        messagingTemplate.convertAndSend(destination, webSocketMessage);
        
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
            WebSocketMessageType.BUY_IT_NOW_COMPLETED, 
            auctionId, 
            message
        );
        
        String destination = "/topic/auction/" + auctionId;
        messagingTemplate.convertAndSend(destination, webSocketMessage);
        
        // log.info("WebSocket - 즉시 구매 완료 알림 전송 완료: {}", destination);
    }
}