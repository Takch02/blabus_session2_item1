package com.highlight.highlight_backend.integration.bid.event;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
public class EventRetrySchedulerTest {

    private static final Long USER_ID = 1L;
    private static final Long AUCTION_ID = 2L;
    private static final String USER_CONSUMER = "USER_PARTICIPATION_UPDATE";

    @Autowired private BidFacade bidFacade;
    @Autowired private UserRepository userRepository;
    @Autowired private AuctionRepository auctionRepository;
    @Autowired private EventConsumerLogRepository logRepository;
    @Autowired private OutboxRepository outboxRepository;
    @Autowired private EventRetryScheduler eventRetryScheduler;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoSpyBean
    private UserService userService;

    @AfterEach
    void tearDown() {
        logRepository.deleteAllInBatch();
        outboxRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("스케줄러: retryCount >= 3인 Consumer를 DEAD 상태로 마킹하고 재발행하지 않는다")
    void consumer_withMaxRetryCount_markedAsDead() throws InterruptedException {
        // Given: 입찰 실패 → USER Consumer FAILED 상태 생성
        doThrow(new RuntimeException("반복 실패 시뮬레이션"))
                .when(userService).increaseParticipationCount(any());

        BigDecimal bidAmount = auctionRepository.findById(AUCTION_ID).orElseThrow()
                .getCurrentHighestBid().add(BigDecimal.valueOf(10000));

        bidFacade.createBidFacade(
                new BidCreateRequestDto(AUCTION_ID, bidAmount, false, BigDecimal.valueOf(1000)),
                USER_ID
        );
        Thread.sleep(500);

        OutboxEvent outbox = outboxRepository.findAll().get(0);

        // retryCount = 3으로 직접 설정 (최대 재시도 도달 시뮬레이션)
        jdbcTemplate.update(
                "UPDATE event_consumer_log SET retry_count = 3 WHERE event_id = ? AND consumer_name = ?",
                outbox.getId(), USER_CONSUMER
        );
        // 다른 Consumer들은 SUCCESS 처리해 간섭 제거
        jdbcTemplate.update(
                "UPDATE event_consumer_log SET status = 'SUCCESS' WHERE event_id = ? AND consumer_name != ?",
                outbox.getId(), USER_CONSUMER
        );

        // 스케줄러 조회 대상이 되도록 updatedAt stale 처리
        EventConsumerLog log = logRepository.findByEventIdAndConsumerName(outbox.getId(), USER_CONSUMER).orElseThrow();
        logRepository.forceUpdateUpdatedAt(log.getId(), LocalDateTime.now().minusMinutes(6));

        // When
        eventRetryScheduler.retryFailedEvents();

        // Then: DEAD 상태로 마킹, 비즈니스 로직 미실행
        EventConsumerLog deadLog = logRepository.findByEventIdAndConsumerName(outbox.getId(), USER_CONSUMER).orElseThrow();
        assertThat(deadLog.getStatus()).isEqualTo(EventStatus.DEAD);
    }

    @Test
    @DisplayName("스케줄러: 타임아웃된 RUNNING 로그를 FAILED로 리셋한 뒤 재시도하여 복구한다")
    void stalledRunning_resetsToFailed_andRecoversOnRetry() throws InterruptedException {
        // Given: 입찰 → USER Consumer 1차 실패, 2차(스케줄러 재시도) 성공
        doThrow(new RuntimeException("1차 실패 — 스레드 증발 시뮬레이션"))
                .doCallRealMethod()
                .when(userService).increaseParticipationCount(any());

        BigDecimal bidAmount = auctionRepository.findById(AUCTION_ID).orElseThrow()
                .getCurrentHighestBid().add(BigDecimal.valueOf(10000));

        User userBefore = userRepository.findById(USER_ID).orElseThrow();
        Long initialCount = userBefore.getParticipationCount();

        bidFacade.createBidFacade(
                new BidCreateRequestDto(AUCTION_ID, bidAmount, false, BigDecimal.valueOf(1000)),
                USER_ID
        );
        Thread.sleep(500);

        OutboxEvent outbox = outboxRepository.findAll().get(0);
        Long outboxId = outbox.getId();

        // 중간 검증: USER Consumer는 FAILED 상태
        EventConsumerLog failedLog = logRepository.findByEventIdAndConsumerName(outboxId, USER_CONSUMER).orElseThrow();
        assertThat(failedLog.getStatus()).isEqualTo(EventStatus.FAILED);

        // FAILED 로그를 RUNNING + stale updatedAt으로 변조 (스레드 증발 상황 재현)
        jdbcTemplate.update(
                "UPDATE event_consumer_log SET status = 'RUNNING', updated_at = ? WHERE event_id = ? AND consumer_name = ?",
                Timestamp.valueOf(LocalDateTime.now().minusMinutes(6)), outboxId, USER_CONSUMER
        );
        // 다른 Consumer 간섭 제거
        jdbcTemplate.update(
                "UPDATE event_consumer_log SET status = 'SUCCESS' WHERE event_id = ? AND consumer_name != ?",
                outboxId, USER_CONSUMER
        );

        // RUNNING 상태 확인
        EventConsumerLog runningLog = logRepository.findByEventIdAndConsumerName(outboxId, USER_CONSUMER).orElseThrow();
        assertThat(runningLog.getStatus()).isEqualTo(EventStatus.RUNNING);

        // When: 스케줄러 실행
        // 흐름: resetStalledRunning(RUNNING→FAILED) → findTargets → publishEvent → claimRunning(FAILED→RUNNING) → 성공
        eventRetryScheduler.retryFailedEvents();
        Thread.sleep(500);

        // Then: SUCCESS + 참여 횟수 복구
        EventConsumerLog recovered = logRepository.findByEventIdAndConsumerName(outboxId, USER_CONSUMER).orElseThrow();
        assertThat(recovered.getStatus()).isEqualTo(EventStatus.SUCCESS);

        User userAfter = userRepository.findById(USER_ID).orElseThrow();
        assertThat(userAfter.getParticipationCount()).isEqualTo(initialCount + 1);
    }

    @Test
    @DisplayName("스케줄러: 동일 outboxId에 Consumer 여러 개가 FAILED여도 모두 한 번의 재발행으로 복구된다")
    void multiple_failedConsumers_for_same_event_allRecovered_withSinglePublish() throws InterruptedException {
        // Given: 입찰 → USER Consumer 실패 후 retryCount 리셋, 나머지 Consumer도 FAILED로 변조
        doThrow(new RuntimeException("1차 실패"))
                .doCallRealMethod()
                .when(userService).increaseParticipationCount(any());

        BigDecimal bidAmount = auctionRepository.findById(AUCTION_ID).orElseThrow()
                .getCurrentHighestBid().add(BigDecimal.valueOf(10000));

        User userBefore = userRepository.findById(USER_ID).orElseThrow();
        Long initialCount = userBefore.getParticipationCount();

        bidFacade.createBidFacade(
                new BidCreateRequestDto(AUCTION_ID, bidAmount, false, BigDecimal.valueOf(1000)),
                USER_ID
        );
        Thread.sleep(500);

        OutboxEvent outbox = outboxRepository.findAll().get(0);
        Long outboxId = outbox.getId();

        // 모든 Consumer를 FAILED + retryCount=0 으로 통일 (재시도 가능 상태)
        jdbcTemplate.update(
                "UPDATE event_consumer_log SET status = 'FAILED', retry_count = 0 WHERE event_id = ?",
                outboxId
        );

        // 모든 Consumer stale 처리
        logRepository.findAll().stream()
                .filter(l -> l.getEventId().equals(outboxId))
                .forEach(l -> logRepository.forceUpdateUpdatedAt(l.getId(), LocalDateTime.now().minusMinutes(6)));

        long consumerCount = logRepository.findAll().stream()
                .filter(l -> l.getEventId().equals(outboxId)).count();

        // When
        eventRetryScheduler.retryFailedEvents();
        Thread.sleep(1000);

        // Then: 모든 Consumer가 SUCCESS
        long successCount = logRepository.findAll().stream()
                .filter(l -> l.getEventId().equals(outboxId))
                .filter(l -> l.getStatus() == EventStatus.SUCCESS)
                .count();
        assertThat(successCount).isEqualTo(consumerCount);

        // USER 비즈니스 로직은 정확히 1번만 실행되어야 함 (중복 publishEvent 방지 효과)
        User userAfter = userRepository.findById(USER_ID).orElseThrow();
        assertThat(userAfter.getParticipationCount()).isEqualTo(initialCount + 1);
    }
}
