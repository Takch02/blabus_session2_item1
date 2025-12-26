package com.highlight.highlight_backend.bid.listener;

import com.highlight.highlight_backend.bid.domain.Bid;
import com.highlight.highlight_backend.bid.event.BidNotificationEvent;
import com.highlight.highlight_backend.bid.repository.BidRepository;
import com.highlight.highlight_backend.bid.service.BidNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * 입찰 후 Websocket 을 통해 메시지 전송은 오래 걸리므로 EventListener 를 통해 비동기로 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BidEventListener {

    private final BidRepository bidRepository;
    private final BidNotificationService bidNotificationService;

    /**
     * 입찰 성공 후 실행되는 알림 로직 (비동기)
     * 메인 트랜잭션(Lock)과 완전히 분리되어 실행됨
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW) // 새로운 트랜잭션에서 안전하게 조회
    public void handleBidNotification(BidNotificationEvent event) {

        log.info("🔔 알림 이벤트 수신: AuctionId={}", event.getAuctionId());

        try {
            // 1. [새 입찰 정보 조회]
            // 알림을 보내려면 '누가(User)', '어디에(Auction)' 입찰했는지 다 필요함.
            Bid newBid = bidRepository.findByIdWithUserAndAuction(event.getNewBidId())
                    .orElseThrow(() -> new RuntimeException("알림 발송 중 데이터 증발: BidId=" + event.getNewBidId()));

            // 2. [전체 방송] "새 입찰이 왔습니다!"
            bidNotificationService.sendNewBidNotification(newBid);

            // 3. [개인 알림] 역전 당할 경우 개인 메시지를 보냄.
            if (event.getPreviousBidId() != null) {
                // 이전 1등 정보 조회 (User 정보만 있으면 됨)
                Bid previousBid = bidRepository.findByIdWithUser(event.getPreviousBidId())
                        .orElse(null);

                // 이전 1등이 존재하고, 그게 '나'가 아닐 때만 알림
                if (previousBid != null && !previousBid.getUser().equals(newBid.getUser())) {
                    bidNotificationService.sendBidOutbidNotification(previousBid, newBid);
                }
            }

        } catch (Exception e) {
            // 비동기라 여기서 에러 나도 입찰은 취소 안 됨. 로그만 남김.
            log.error("❌ 알림 발송 실패: {}", e.getMessage(), e);
        }
    }
}
