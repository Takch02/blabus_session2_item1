package com.highlight.highlight_backend.bid.service;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.auction.dto.AuctionStatusWebSocketDto;
import com.highlight.highlight_backend.auction.repository.AuctionRepository;
import com.highlight.highlight_backend.bid.domain.Bid;
import com.highlight.highlight_backend.bid.dto.BidWebSocketDto;
import com.highlight.highlight_backend.bid.repository.BidRepository;
import com.highlight.highlight_backend.common.socket.service.GlobalSocketService;
import com.highlight.highlight_backend.common.socket.dto.WebSocketMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BidNotificationService {

    private final BidRepository bidRepository;
    private final GlobalSocketService globalSocketService;
    private final AuctionRepository auctionRepository;

    /**
     * 새로운 입찰이 생기면 webSocket을 통해 브로드캐스트로 보냄
     */
    public void sendNewBidNotification(Bid bid) {

        Long auctionId = bid.getAuction().getId();
        log.info("WebSocket - 새 입찰 알림 전송: 경매={}, 입찰자={}, 금액={}",
                 auctionId, bid.getUser().getNickname(), bid.getBidAmount());

        // 입찰 통계 조회
        Long totalBidders = auctionRepository.findAuctionByTotalBidders(auctionId);
        Long totalBids = auctionRepository.findAuctionByTotalBids(auctionId);

        // WebSocket 메시지 데이터 생성
        BidWebSocketDto bidData = BidWebSocketDto.from(bid, totalBidders, totalBids);
        WebSocketMessageDto message = WebSocketMessageDto.of(
                WebSocketMessageDto.WebSocketMessageType.NEW_BID,
                auctionId,
                bidData
        );

        // 해당 경매를 구독하고 있는 모든 클라이언트에게 브로드캐스트
        String destination = "/topic/auction/" + auctionId;
        globalSocketService.sendToTopic(destination, message);
    }

    /**
     * 경매 상태 업데이트 알림
     *
     * @param auction 경매 정보
     */
    public void sendAuctionStatusUpdate(Auction auction) {
        Long auctionId = auction.getId();

        // 입찰 통계 조회
        Long totalBidders = auctionRepository.findAuctionByTotalBidders(auctionId);
        Long totalBids = auctionRepository.findAuctionByTotalBids(auctionId);

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
                WebSocketMessageDto.WebSocketMessageType.AUCTION_STATUS_UPDATE,
                auctionId,
                statusData
        );

        // 브로드캐스트
        String destination = "/topic/auction/" + auctionId;
        globalSocketService.sendToTopic(destination, message);

        // log.info("WebSocket - 경매 상태 업데이트 전송 완료: {}", destination);
    }

    /**
     * 경매 승리 시 개인에게 보냄
     */
    public void notifyWin(Bid winnerBid) {
        if (winnerBid == null) return; // 방어 로직

        Long userId = winnerBid.getUser().getId();
        Long auctionId = winnerBid.getAuction().getId();
        String message = "축하합니다! 경매에서 낙찰받으셨습니다.";

        WebSocketMessageDto notification = WebSocketMessageDto.of(
                WebSocketMessageDto.WebSocketMessageType.WIN_BID,
                auctionId,
                message
        );

        // 3. 개인 큐에 전송 (/queue/...)
        globalSocketService.sendToUser(userId, notification);
    }

    /**
     * 기존 입찰보다 더 큰 금액의 입찰이 들어오면 알림을 보냄
     */
    public void sendBidOutbidNotification(Bid previousWinner, Bid savedBid) {
        Long userId = previousWinner.getUser().getId();
        Long auctionId = previousWinner.getAuction().getId();
        String message = savedBid.getBidAmount() + "원 " + "입찰이 들어왔습니다.";

        WebSocketMessageDto notification = WebSocketMessageDto.of(
                WebSocketMessageDto.WebSocketMessageType.NEW_BID,
                auctionId,
                message
        );
        globalSocketService.sendToUser(userId, notification);
    }
}
