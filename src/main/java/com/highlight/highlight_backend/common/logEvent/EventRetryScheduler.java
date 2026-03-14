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

/**
 * Event 유실된 경우 다시 실행하는 스케줄러
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventRetryScheduler {

    private final EventConsumerLogRepository logRepository;
    private final OutboxRepository outboxRepository; // Outbox 조회를 위해 주입
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * 5분마다 실행 (실패하거나 누락된 비동기 이벤트 재처리)
     */
    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void retryFailedEvents() {
        // 재시도 대상 조회: PENDING이면서 5분이 지났거나(스레드 증발), FAILED인 상태
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(5);
        List<EventStatus> targetStatuses = List.of(EventStatus.PENDING, EventStatus.FAILED);
        List<EventConsumerLog> retryTargets = logRepository.findAllByStatusInAndUpdatedAtBefore(targetStatuses, cutoffTime);

        if (retryTargets.isEmpty()) {
            return;
        }

        log.info("📢 발견된 재시도 대상 로그: {}건. 재발행을 시작합니다.", retryTargets.size());

        for (EventConsumerLog logEntity : retryTargets) {
            try {
                OutboxEvent outbox = outboxRepository.findById(logEntity.getEventId())
                        .orElseThrow(() -> new IllegalStateException("Outbox 원본 데이터를 찾을 수 없습니다. EventId=" + logEntity.getEventId()));

                // JSON 문자열을 원래의 자바 객체(Event)로 역직렬화
                Class<?> eventClass = Class.forName(outbox.getEventType());
                Object eventObject = objectMapper.readValue(outbox.getPayload(), eventClass);

                // 재시도 횟수 증가 처리
                logEntity.increaseRetryCount();
                
                // 재시도 3회 이상은 무시
                if (logEntity.getRetryCount() > 3) {
                    log.error("🚨 이벤트 재시도 3회 초과! 관리자 확인 필요: Consumer={}", logEntity.getConsumerName());
                    continue;
                }

                // 5. 이벤트 재발행
                eventPublisher.publishEvent(eventObject);

                log.info("♻️ 스케줄러에 의한 이벤트 재발행 성공: EventId={}, Consumer={}", logEntity.getEventId(), logEntity.getConsumerName());

            } catch (Exception e) {
                log.error("❌ 스케줄러 이벤트 재발행 중 오류 발생 (EventId={}): {}", logEntity.getEventId(), e.getMessage());
            }
        }
    }
}