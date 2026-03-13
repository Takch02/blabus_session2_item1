package com.highlight.highlight_backend.auction.listener;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.bid.event.BidCreatedEvent;
import com.highlight.highlight_backend.auction.notification.AuctionWebSocketNotifier;
import com.highlight.highlight_backend.auction.repository.AuctionRepository;
import com.highlight.highlight_backend.common.logEvent.EventConsumerLogService;
import com.highlight.highlight_backend.common.outbox.OutboxService;
import com.highlight.highlight_backend.user.dto.UserNicknameUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionEventListener {

    private final AuctionWebSocketNotifier auctionWebSocketNotifier;
    private final AuctionRepository auctionRepository;
    private final OutboxService outboxService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleNicknameUpdate(UserNicknameUpdateEvent event) {
        try {
            int updatedCount = auctionRepository.updateWinnerNameByWinnerId(
                    event.getUserId(),
                    event.getNickname()
            );
            log.info("닉네임 변경 반영 완료: 업데이트된 경매 수 = {}", updatedCount);

            outboxService.markPublished(event.getOutboxId());
        } catch (Exception e) {
            log.error("닉네임 동기화 중 에러 발생", e);
        }
    }

    /**
     * 입찰 성공 시 경매 정보 수정 event
     * Bid.CreateBid() 와 같은 트렌젝션에서 수행되야 한다.
     * 비동기가 아닌 동기로 실행
     */
    @EventListener
    public void handleBidCreatedEvent(BidCreatedEvent event) {
        try {
            // 1. 이벤트(Event, 사건)에 담긴 식별자(ID)로 경매 데이터를 찾습니다.
            Auction auction = auctionRepository.findById(event.getAuctionId())
                    .orElseThrow(() -> new IllegalArgumentException("경매를 찾을 수 없습니다."));

            // 2. 경매 도메인 내부의 로직을 통해 상태(최고가 등)를 변경합니다.
            auction.updateHighestBid(
                    event.getBidAmount(),
                    event.getUserId(),
                    event.getUserNickname(),
                    event.isNewBidder()
            );

            auctionRepository.save(auction);

        } catch (Exception e) {
            log.error("경매 최고가 갱신 실패. 배치 재시도 대상이 됩니다. auctionId={}", event.getAuctionId(), e);

            throw e;
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAuctionWebSocketNotification(BidCreatedEvent event) {
        log.info("🔔 [경매 모듈] 웹소켓 방송 이벤트 수신: AuctionId={}", event.getAuctionId());

        try {
            auctionWebSocketNotifier.sendNewBidNotification(event);
        } catch (Exception e) {
            log.error("❌ [경매 모듈] 웹소켓 발송 실패: {}", e.getMessage(), e);
        }
    }
}