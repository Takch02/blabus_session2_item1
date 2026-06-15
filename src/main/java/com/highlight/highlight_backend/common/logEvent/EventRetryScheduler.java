package com.highlight.highlight_backend.common.logEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.highlight.highlight_backend.common.outbox.OutboxEvent;
import com.highlight.highlight_backend.common.outbox.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventRetryScheduler {

    private static final int MAX_RETRY_COUNT = 3;

    private final EventConsumerLogRepository logRepository;
    private final OutboxRepository outboxRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void retryFailedEvents() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(5);

        // 1. 타임아웃된 RUNNING(스레드 증발) → FAILED 리셋
        int resetCount = logRepository.resetStalledRunning(cutoffTime);
        if (resetCount > 0) {
            log.warn("⚠️ 타임아웃된 RUNNING 이벤트 {}건을 FAILED로 리셋했습니다.", resetCount);
        }

        // 2. 재시도 대상 조회 (PENDING, FAILED)
        List<EventConsumerLog> retryTargets = logRepository.findAllByStatusInAndUpdatedAtBefore(
                List.of(EventStatus.PENDING, EventStatus.FAILED), cutoffTime
        );

        if (retryTargets.isEmpty()) return;

        log.info("📢 재시도 대상 Consumer 로그: {}건", retryTargets.size());

        // 3. outboxId 기준 그룹화 (동일 이벤트에 대한 publishEvent() 중복 방지)
        Map<Long, List<EventConsumerLog>> grouped = retryTargets.stream()
                .collect(Collectors.groupingBy(EventConsumerLog::getEventId));

        // 4. IN 쿼리로 Outbox 한 번에 조회 (N+1 제거)
        Map<Long, OutboxEvent> outboxMap = outboxRepository.findAllById(grouped.keySet()).stream()
                .collect(Collectors.toMap(OutboxEvent::getId, Function.identity()));

        // 5. outboxId 당 1번만 publishEvent
        for (Map.Entry<Long, List<EventConsumerLog>> entry : grouped.entrySet()) {
            Long eventId = entry.getKey();
            List<EventConsumerLog> logs = entry.getValue();

            // 재시도 횟수 초과 Consumer → DEAD 처리
            List<EventConsumerLog> deadLogs = logs.stream()
                    .filter(l -> l.getRetryCount() >= MAX_RETRY_COUNT)
                    .toList();
            deadLogs.forEach(l -> {
                l.markAsDead();
                log.error("🚨 최대 재시도 초과 — EventId={}, Consumer={}", l.getEventId(), l.getConsumerName());
            });

            List<EventConsumerLog> eligibleLogs = logs.stream()
                    .filter(l -> l.getRetryCount() < MAX_RETRY_COUNT)
                    .toList();

            if (eligibleLogs.isEmpty()) continue;

            // 재발행 시도 전에 retryCount 일괄 증가 — outbox 없음/역직렬화 실패도 시도 횟수에 포함
            logRepository.bulkIncrementRetryCount(
                    eligibleLogs.stream().map(EventConsumerLog::getId).toList());

            OutboxEvent outbox = outboxMap.get(eventId);
            if (outbox == null) {
                log.error("❌ Outbox를 찾을 수 없음 — EventId={}", eventId);
                continue;
            }

            try {
                Class<?> eventClass = Class.forName(outbox.getEventType());
                Object eventObject = objectMapper.readValue(outbox.getPayload(), eventClass);

                // 이벤트 재발행 (각 Listener에서 claimRunning()으로 중복 처리 차단)
                eventPublisher.publishEvent(eventObject);

                log.info("♻️ 재발행 완료 — EventId={}, 대상 Consumer: {}", eventId,
                        eligibleLogs.stream().map(EventConsumerLog::getConsumerName).collect(Collectors.joining(", ")));

            } catch (Exception e) {
                log.error("❌ 재발행 실패 — EventId={}: {}", eventId, e.getMessage());
            }
        }
    }
}
