package com.highlight.highlight_backend.bid.event;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.auction.repository.AuctionRepository;
import com.highlight.highlight_backend.bid.application.BidFacade;
import com.highlight.highlight_backend.bid.dto.BidCreateRequestDto;
import com.highlight.highlight_backend.common.logEvent.EventConsumerLog;
import com.highlight.highlight_backend.common.logEvent.EventConsumerLogRepository;
import com.highlight.highlight_backend.common.logEvent.EventRetryScheduler;
import com.highlight.highlight_backend.common.logEvent.EventStatus;
import com.highlight.highlight_backend.common.outbox.OutboxEvent;
import com.highlight.highlight_backend.common.outbox.OutboxRepository;
import com.highlight.highlight_backend.user.domain.User;
import com.highlight.highlight_backend.user.repository.UserRepository;
import com.highlight.highlight_backend.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
public class EventRecoveryTest {

    @Autowired private BidFacade bidFacade;
    @Autowired private UserRepository userRepository;
    @Autowired private AuctionRepository auctionRepository;
    @Autowired private OutboxRepository outboxRepository;
    
    // ★ 새롭게 추가된 Log 레포지토리와 신규 스케줄러
    @Autowired private EventConsumerLogRepository logRepository;
    @Autowired private EventRetryScheduler eventRetryScheduler;

    @MockitoSpyBean
    private UserService userService;

    @AfterEach
    void tearDown() {
        logRepository.deleteAllInBatch();
        outboxRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("복구 증명: User 비동기 로직이 에러로 실패하더라도, DLQ 스케줄러가 이를 감지하고 완벽하게 재처리한다.")
    void testLogicEventLossAndRecovery() throws InterruptedException {
        // 1. [Given] 초기 상태 셋팅
        Long userId = 1L;
        Long auctionId = 2L;
        BigDecimal bidAmount = BigDecimal.valueOf(114000);

        User userBefore = userRepository.findById(userId).orElseThrow();
        Long initialCount = userBefore.getParticipationCount();

        // 2. 유저 업데이트(비동기) 로직에서 1번째는 에러 발생, 2번째(스케줄러 재시도)는 성공하도록 조작
        doThrow(new RuntimeException("🔥 1차 시도: 네트워크 불안정으로 인한 유저 업데이트 실패!"))
                .doCallRealMethod() // ★ 두 번째 호출부터는 정상 작동
                .when(userService).increaseParticipationCount(any());

        // 3. [When 1] 입찰 시도 (메인 트랜잭션 실행)
        bidFacade.createBidFacade(new BidCreateRequestDto(
                auctionId, bidAmount, false, BigDecimal.valueOf(1000)
        ), userId);

        // 비동기 스레드가 에러를 뿜고 FAILED 상태를 기록할 때까지 아주 잠시 대기
        Thread.sleep(500); 

        // 4. [중간 검증] 메인 로직은 성공했고, 유저 로직은 실패(FAILED)했음을 확인
        Auction auction = auctionRepository.findById(auctionId).orElseThrow();
        assertThat(auction.getCurrentHighestBid()).isEqualByComparingTo(bidAmount); // 입찰은 정상 반영

        OutboxEvent outbox = outboxRepository.findAll().get(0);
        assertThat(outbox.isPublished()).isTrue(); // Outbox는 정상 발행 처리됨!

        User userMid = userRepository.findById(userId).orElseThrow();
        assertThat(userMid.getParticipationCount()).isEqualTo(initialCount); // ★ 유저 카운트는 증가하지 않음 (부분 실패)

        EventConsumerLog failedLog = logRepository.findByEventIdAndConsumerName(outbox.getId(), "USER_PARTICIPATION_UPDATE").orElseThrow();
        assertThat(failedLog.getStatus()).isEqualTo(EventStatus.FAILED); // ★ 상태가 FAILED로 잘 기록되었는지 확인

        // 5. [When 2] 스케줄러가 작동할 수 있도록 시간을 과거로 조작
        logRepository.forceUpdateUpdatedAt(failedLog.getId(), LocalDateTime.now().minusMinutes(6));

        // 6. 스케줄러 수동 실행
        eventRetryScheduler.retryFailedEvents();
        
        // 비동기 재시도가 완료될 때까지 잠시 대기
        Thread.sleep(500);

        // 7. [Then] 완벽한 복구 결과 검증
        User userAfter = userRepository.findById(userId).orElseThrow();
        
        // ★ 1차 시도에서 누락되었던 유저 카운트가 스케줄러에 의해 완벽하게 복구(증가)되었는가?
        assertThat(userAfter.getParticipationCount()).isEqualTo(initialCount + 1);

        // ★ 로그 테이블의 상태가 FAILED에서 SUCCESS로 변경되었는가?
        EventConsumerLog recoveredLog = logRepository.findByEventIdAndConsumerName(outbox.getId(), "USER_PARTICIPATION_UPDATE").orElseThrow();
        assertThat(recoveredLog.getStatus()).isEqualTo(EventStatus.SUCCESS);
    }
}