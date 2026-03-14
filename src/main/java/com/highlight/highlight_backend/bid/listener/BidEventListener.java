package com.highlight.highlight_backend.bid.listener;

import com.highlight.highlight_backend.bid.domain.Bid;
import com.highlight.highlight_backend.bid.event.BidCreatedEvent;
import com.highlight.highlight_backend.bid.repository.BidRepository;
import com.highlight.highlight_backend.bid.service.BidNotificationService;
import com.highlight.highlight_backend.common.logEvent.EventConsumerLogService;
import com.highlight.highlight_backend.common.outbox.OutboxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
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
    private final EventConsumerLogService eventConsumerLogService;
    private static final String CONSUMER_NAME = "BID_NOTI";


    @EventListener
    public void createLogEvent(BidCreatedEvent event) {
        eventConsumerLogService.preRegisterLog(event.getOutboxId(), CONSUMER_NAME);
    }
    /**
     * 입찰 성공 후 실행되는 알림 로직 (비동기)
     * 메인 트랜잭션(Lock)과 완전히 분리되어 실행됨
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW) // 새로운 트랜잭션에서 안전하게 조회
    public void handleBidNotification(BidCreatedEvent event) {

        log.info("🔔 알림 이벤트 수신: AuctionId={}", event.getAuctionId());
        if (eventConsumerLogService.isAlreadySuccess(event.getOutboxId(), CONSUMER_NAME)) {
            return;
        }
        try {
            if (event.getPreviousBidId() != null) {
                Bid previousBid = bidRepository.findByIdWithUser(event.getPreviousBidId()).orElse(null);
                Bid newBid = bidRepository.findByIdWithUser(event.getBidId()).orElse(null);

                if (previousBid != null && newBid != null && !previousBid.getUser().equals(newBid.getUser())) {
                    bidNotificationService.sendBidOutbidNotification(previousBid, newBid);
                }
            }
            eventConsumerLogService.markAsSuccess(event.getOutboxId(), CONSUMER_NAME);
            log.info("입찰 메시지 전송 시작 : {}", event.getBidId());
        } catch (Exception e) {
            log.error("❌ 알림 발송 실패: {}", e.getMessage(), e);
            eventConsumerLogService.markAsFailed(event.getBidId(), CONSUMER_NAME, e.getMessage());
        }
    }
}
