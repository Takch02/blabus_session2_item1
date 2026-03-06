package com.highlight.highlight_backend.auction.listener; // 패키지 변경!

import com.highlight.highlight_backend.auction.repository.AuctionRepository;
import com.highlight.highlight_backend.common.outbox.OutboxService;
import com.highlight.highlight_backend.user.dto.UserNicknameUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionUserEventListener { // 클래스명 변경

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
}