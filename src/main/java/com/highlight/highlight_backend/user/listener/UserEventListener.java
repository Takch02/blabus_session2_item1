package com.highlight.highlight_backend.user.listener;

import com.highlight.highlight_backend.bid.event.BidCompleteEvent;
import com.highlight.highlight_backend.common.outbox.OutboxService;
import com.highlight.highlight_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 입찰 후 User.participation_count++ 로직은 EventListener를 통해 비동기로 실행
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventListener {

    private final UserService userService;
    private final OutboxService outboxService;

    @Async // ★ 별도 스레드에서 실행 (메인 스레드 대기 안 함)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // ★ 커밋 성공 후에만 실행
    @Transactional(propagation = Propagation.REQUIRES_NEW) // ★ 새로운 트랜잭션 시작
    public void handleUserUpdate(BidCompleteEvent event) {

        userService.increaseParticipationCount(event.getUserId());

        outboxService.markPublished(event.getOutboxId());

        log.info("입찰 후 User.participation_count++ 비동기 이벤트 처리 완료 UserId={}", event.getUserId());
    }
}
