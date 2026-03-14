package com.highlight.highlight_backend.common.logEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventConsumerLogService {

    private final EventConsumerLogRepository eventConsumerLogRepository;

    @Transactional(propagation = Propagation.REQUIRED) // 메인 트랜잭션에 합류
    public void preRegisterLog(Long eventId, String consumerName) {
        if (!eventConsumerLogRepository.existsByEventIdAndConsumerName(eventId, consumerName)) {
            eventConsumerLogRepository.save(new EventConsumerLog(eventId, consumerName));
        }
    }

    /**
     * [비동기] 멱등성 검증: 이미 성공한 이력인지 확인
     */
    @Transactional(readOnly = true)
    public boolean isAlreadySuccess(Long eventId, String consumerName) {
        return eventConsumerLogRepository.findByEventIdAndConsumerName(eventId, consumerName)
                .map(log -> log.getStatus() == EventStatus.SUCCESS)
                .orElse(false); // 아예 없으면 재시도해야 하니 false (예외 상황)
    }

    /**
     * [비동기] 성공 처리
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsSuccess(Long eventId, String consumerName) {
        eventConsumerLogRepository.findByEventIdAndConsumerName(eventId, consumerName)
                .ifPresent(EventConsumerLog::markAsSuccess);
    }

    /**
     * [비동기] 실패 처리
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsFailed(Long eventId, String consumerName, String errorMessage) {
        eventConsumerLogRepository.findByEventIdAndConsumerName(eventId, consumerName)
                .ifPresent(log -> {
                    log.markAsFailed(errorMessage);
                });
    }

    /**
     * 스케줄러가 재시도 대상을 가져올 때 Entity가 아닌 DTO 리스트를 반환
     */
    @Transactional(readOnly = true)
    public List<RetryTargetDto> getRetryTargets(LocalDateTime cutoffTime) {
        List<EventStatus> targetStatuses = List.of(EventStatus.PENDING, EventStatus.FAILED);

        return eventConsumerLogRepository.findAllByStatusInAndUpdatedAtBefore(targetStatuses, cutoffTime)
                .stream()
                .map(log -> new RetryTargetDto(
                        log.getEventId(),
                        log.getConsumerName(),
                        log.getRetryCount()
                ))
                .collect(Collectors.toList());
    }

    /**
     * 스케줄러가 재시도를 위해 조회 횟수를 증가시킬 때 사용
     */
    @Transactional
    public void increaseRetryCount(Long eventId, String consumerName) {
        eventConsumerLogRepository.findByEventIdAndConsumerName(eventId, consumerName)
                .ifPresent(EventConsumerLog::increaseRetryCount);
    }
}


