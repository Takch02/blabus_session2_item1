package com.highlight.highlight_backend.common.logEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventConsumerLogService {

    private final EventConsumerLogRepository eventConsumerLogRepository;
    private final JdbcTemplate jdbcTemplate;

    public void preRegisterLogs(Long eventId, List<String> consumerNames) {
        List<String> existing = eventConsumerLogRepository
                .findExistingConsumerNames(eventId, consumerNames);

        List<EventConsumerLog> logs = consumerNames.stream()
                .filter(name -> !existing.contains(name))
                .map(name -> new EventConsumerLog(eventId, name))
                .toList();

        if (!logs.isEmpty()) {
            bulkInsert(logs);
        }
    }

    public void bulkInsert(List<EventConsumerLog> logs) {
        String sql = "INSERT INTO event_consumer_log " +
                "(event_id, consumer_name, status, retry_count, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.batchUpdate(sql, logs, logs.size(),
                (ps, log) -> {
                    ps.setLong(1, log.getEventId());
                    ps.setString(2, log.getConsumerName());
                    ps.setString(3, "PENDING");
                    ps.setInt(4, 0);
                    ps.setTimestamp(5, Timestamp.valueOf(now));
                    ps.setTimestamp(6, Timestamp.valueOf(now));
                }
        );
    }

    /**
     * PENDING/FAILED → RUNNING 원자적 전환으로 처리 권한 획득
     * true = 이 스레드가 처리 권한 획득, false = 이미 다른 스레드가 처리 중이거나 SUCCESS
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimRunning(Long eventId, String consumerName) {
        return eventConsumerLogRepository.claimAsRunning(eventId, consumerName) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsSuccess(Long eventId, String consumerName) {
        eventConsumerLogRepository.findByEventIdAndConsumerName(eventId, consumerName)
                .ifPresent(EventConsumerLog::markAsSuccess);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsFailed(Long eventId, String consumerName, String errorMessage) {
        eventConsumerLogRepository.findByEventIdAndConsumerName(eventId, consumerName)
                .ifPresent(log -> log.markAsFailed(errorMessage));
    }
}
