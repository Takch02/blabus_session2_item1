package com.highlight.highlight_backend.auction.notification;

import com.highlight.highlight_backend.bid.event.BidCreatedEvent;
import com.highlight.highlight_backend.auction.service.UserAuctionService;
import com.highlight.highlight_backend.auction.dto.AuctionNewBidBroadcastDto;
import com.highlight.highlight_backend.common.socket.dto.WebSocketMessageDto;
import com.highlight.highlight_backend.common.socket.service.GlobalSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionWebSocketNotifier {

    private final GlobalSocketService globalSocketService;

    /**
     * 새로운 입찰이 생기면 webSocket을 통해 브로드캐스트로 보냄
     */
    public void sendNewBidNotification(BidCreatedEvent event) {
        Long auctionId = event.getAuctionId();

        log.info("WebSocket - 새 입찰 알림 전송: 경매={}, 입찰자={}, 금액={}",
                auctionId, event.getUserNickname(), event.getBidAmount());

        // WebSocket 메시지 데이터 생성
        AuctionNewBidBroadcastDto bidData = AuctionNewBidBroadcastDto.fromEvent(
                event.getBidId(),
                event.getBidAmount(),
                event.getUserNickname(),
                event.getTotalBidders(),
                event.getTotalBids()
        );

        WebSocketMessageDto message = WebSocketMessageDto.of(
                WebSocketMessageDto.WebSocketMessageType.NEW_BID,
                auctionId,
                bidData
        );

        // 해당 경매를 구독하고 있는 모든 클라이언트에게 브로드캐스트
        String destination = "/topic/auction/" + auctionId;
        globalSocketService.sendToTopic(destination, message);
    }
}
