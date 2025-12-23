package com.highlight.highlight_backend.user.listener;

import com.highlight.highlight_backend.exception.BusinessException;
import com.highlight.highlight_backend.exception.UserErrorCode;
import com.highlight.highlight_backend.user.domain.User;
import com.highlight.highlight_backend.bid.event.BidCreateEvent;
import com.highlight.highlight_backend.user.repository.UserRepository;
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

    private final UserRepository userRepository;

    @Async // ★ 별도 스레드에서 실행 (메인 스레드 대기 안 함)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // ★ 커밋 성공 후에만 실행
    @Transactional(propagation = Propagation.REQUIRES_NEW) // ★ 새로운 트랜잭션 시작
    public void handleUserUpdate(BidCreateEvent event) {

        // 1. 참여 횟수 증가 로직
        if (event.isNewBidder()) {
            User user = userRepository.findById(event.getUserId())
                    .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

            user.participateInAuction();
            // 더티 체킹으로 UPDATE 쿼리 나감
        }
        log.info("입찰 후 User.participation_count++ 비동기 이벤트 처리 완료 UserId={}", event.getUserId());
    }
}
