package com.highlight.highlight_backend.integration.bid.event;

import com.highlight.highlight_backend.common.logEvent.EventConsumerLogRepository;
import com.highlight.highlight_backend.common.logEvent.EventConsumerLogService;
import com.highlight.highlight_backend.common.logEvent.EventStatus;
import com.highlight.highlight_backend.common.outbox.OutboxRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * claimRunning()의 원자적 CAS(Compare-And-Swap) 동작 검증.
 *
 * 기존 isAlreadySuccess() 방식의 race condition:
 *   Thread A: SELECT(PENDING) → 비즈니스 로직 → SUCCESS
 *   Thread B: SELECT(PENDING) → 비즈니스 로직 → SUCCESS  ← 중복 실행!
 *
 * claimRunning() UPDATE WHERE 방식:
 *   Thread A: UPDATE SET RUNNING WHERE status=PENDING → 1행 → 처리 진행
 *   Thread B: UPDATE SET RUNNING WHERE status=PENDING → 0행 (이미 RUNNING) → return
 */
@SpringBootTest
@ActiveProfiles("test")
public class ClaimRunningConcurrencyTest {

    @Autowired private EventConsumerLogService eventConsumerLogService;
    @Autowired private EventConsumerLogRepository logRepository;
    @Autowired private OutboxRepository outboxRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        logRepository.deleteAllInBatch();
        outboxRepository.deleteAllInBatch();
    }

    private long insertOutboxAndLog(String status) {
        long outboxId = System.nanoTime(); // 수동 ID (OutboxEvent는 @GeneratedValue 없음)
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());

        jdbcTemplate.update(
                "INSERT INTO outbox_event (id, aggregate_type, aggregate_id, event_type, payload, published, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                outboxId, "TEST", 0L, "com.test.Event", "{}", false, now
        );
        jdbcTemplate.update(
                "INSERT INTO event_consumer_log (event_id, consumer_name, status, retry_count, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
                outboxId, "TEST_CONSUMER", status, 0, now, now
        );
        return outboxId;
    }

    @Test
    @DisplayName("동시에 두 스레드가 claimRunning()을 호출하면 정확히 하나만 처리 권한을 획득한다")
    void concurrent_claimRunning_exactlyOneThreadAcquiresLock() throws InterruptedException {
        long outboxId = insertOutboxAndLog("PENDING");

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        AtomicInteger claimSuccessCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        for (int i = 0; i < 2; i++) {
            executor.execute(() -> {
                try {
                    ready.countDown();
                    start.await();
                    if (eventConsumerLogService.claimRunning(outboxId, "TEST_CONSUMER")) {
                        claimSuccessCount.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        done.await();
        executor.shutdown();

        // 정확히 1개 스레드만 처리 권한 획득
        assertThat(claimSuccessCount.get()).isEqualTo(1);

        // DB 상태: RUNNING
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM event_consumer_log WHERE event_id = ? AND consumer_name = ?",
                String.class, outboxId, "TEST_CONSUMER"
        );
        assertThat(status).isEqualTo(EventStatus.RUNNING.name());
    }

    @Test
    @DisplayName("SUCCESS 상태인 Consumer는 claimRunning()으로 재처리를 시도해도 권한을 획득하지 못한다")
    void claimRunning_onSuccessStatus_returnsFalse() {
        long outboxId = insertOutboxAndLog("SUCCESS");

        boolean claimed = eventConsumerLogService.claimRunning(outboxId, "TEST_CONSUMER");

        assertThat(claimed).isFalse();
    }

    @Test
    @DisplayName("FAILED 상태인 Consumer는 claimRunning()으로 RUNNING 전환 후 처리 가능하다")
    void claimRunning_onFailedStatus_returnsTrue() {
        long outboxId = insertOutboxAndLog("FAILED");

        boolean claimed = eventConsumerLogService.claimRunning(outboxId, "TEST_CONSUMER");

        assertThat(claimed).isTrue();

        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM event_consumer_log WHERE event_id = ? AND consumer_name = ?",
                String.class, outboxId, "TEST_CONSUMER"
        );
        assertThat(status).isEqualTo(EventStatus.RUNNING.name());
    }
}
