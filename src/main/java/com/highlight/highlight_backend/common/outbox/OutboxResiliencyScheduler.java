package com.highlight.highlight_backend.common.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxResiliencyScheduler {

    private final OutboxRepository outboxRepository;
    private final OutboxService outboxService; // (선택) 필요 시 사용
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * 5분마다 실행 (fixedDelay = 300000ms)
     */
    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void resendMissingEvents() {
        // 1. "지금으로부터 1분 전"보다 더 옛날에 생성된 것만 조회 (Safety Margin)
        // 방금 트랜잭션 도는 중인 애들을 건드리지 않기 위함
        LocalDateTime cutOffTime = LocalDateTime.now().minusSeconds(2);
        
        List<OutboxEvent> missingEvents = outboxRepository.findAllByPublishedFalseAndCreatedAtBefore(cutOffTime);

        if (missingEvents.isEmpty()) {
            return;
        }

        log.info("📢 발견된 유실 이벤트: {}건. 재발행을 시작합니다.", missingEvents.size());

        for (OutboxEvent outbox : missingEvents) {
            try {
                // 2. 이벤트 복원 (JSON String -> Java Object)
                // DB에 저장된 클래스 이름("com.highlight...BidCompleteEvent")으로 클래스 타입을 찾음
                Class<?> eventClass = Class.forName(outbox.getEventType());
                Object eventObject = objectMapper.readValue(outbox.getPayload(), eventClass);

                // 3. 이벤트 재발행 (Publish)
                // 리스너가 다시 이 이벤트를 받아서 처리 시도
                eventPublisher.publishEvent(eventObject);

                log.info("♻️ 이벤트 재발행 성공: ID={}, Type={}", outbox.getId(), outbox.getAggregateType());

            } catch (Exception e) {
                log.error("❌ 이벤트 재발행 중 오류 발생 (ID={}): {}", outbox.getId(), e.getMessage());
                // 여기서 실패하면? 그냥 둠. 다음 스케줄링 때 다시 시도함.
            }
        }
    }
}