package com.highlight.highlight_backend.common.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.highlight.highlight_backend.exception.BusinessException;
import com.highlight.highlight_backend.exception.OutboxErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper; // JSON 변환기

    /**
     * 1. 이벤트를 Outbox 테이블에 저장 (트랜잭션 안에서 수행 필수)
     */
    @Transactional
    public void appendEvent(Long outboxId, String aggregateType, Long aggregateId, Object event) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new BusinessException(OutboxErrorCode.FAIL_CONVERT_JSON);
        }

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .id(outboxId)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(event.getClass().getName())
                .payload(payload)
                .build();

        outboxRepository.save(outboxEvent);
    }

    /**
     * 2. 리스너가 성공적으로 처리했을 때 호출 (완료 처리)
     */
    @Transactional
    public void markPublished(Long outboxId) {
        OutboxEvent outbox = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new BusinessException(OutboxErrorCode.NOT_FOUND_OUTBOX));
        
        outbox.markAsPublished();
    }
}