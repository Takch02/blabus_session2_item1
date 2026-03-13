package com.highlight.highlight_backend.user.listener;

import com.highlight.highlight_backend.bid.event.BidCreatedEvent;
import com.highlight.highlight_backend.common.logEvent.EventConsumerLogService;
import com.highlight.highlight_backend.common.outbox.OutboxService;
import com.highlight.highlight_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
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
    private final EventConsumerLogService eventConsumerLogService;
    private static final String CONSUMER_NAME = "USER_PARTICIPATION_UPDATE";

    /**
     * 입찰 트렌젝션과 묶여서 실행됨.
     * 로그를 저장하여 이벤트 유실 시 스케줄러가 나중에 실행시킴.
     */
    @EventListener
    public void preRegisterLog(BidCreatedEvent event) {
        eventConsumerLogService.preRegisterLog(event.getOutboxId(), CONSUMER_NAME);
        log.info("🎫 [동기] 유저 업데이트 대기표 발급 완료 (EventId={})", event.getOutboxId());
    }

    /**
     * userParticipationCount++ 로직은 비동기로 실행 (DeadLock 회피)
     */
    @Async //  별도 스레드에서 실행
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // 커밋 성공 후에만 실행
    @Transactional(propagation = Propagation.REQUIRES_NEW) // 새로운 트랜잭션 시작
    public void handleUserUpdate(BidCreatedEvent event) {

        userService.increaseParticipationCount(event.getUserId());
        log.info("입찰 후 User.participation_count++ 비동기 이벤트 처리 완료 UserId={}", event.getUserId());
    }
}
