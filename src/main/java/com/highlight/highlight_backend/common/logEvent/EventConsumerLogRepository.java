package com.highlight.highlight_backend.common.logEvent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventConsumerLogRepository extends JpaRepository<EventConsumerLog, Long> {

    Optional<EventConsumerLog> findByEventIdAndConsumerName(Long eventId, String consumerName);

    List<EventConsumerLog> findAllByStatusInAndUpdatedAtBefore(List<EventStatus> statuses, LocalDateTime cutoffTime);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE EventConsumerLog e SET e.updatedAt = :time WHERE e.id = :id")
    void forceUpdateUpdatedAt(@Param("id") Long id, @Param("time") LocalDateTime time);

    @Query("SELECT ecl.consumerName FROM EventConsumerLog ecl " +
            "WHERE ecl.eventId = :eventId AND ecl.consumerName IN :consumerNames")
    List<String> findExistingConsumerNames(
            @Param("eventId") Long eventId,
            @Param("consumerNames") List<String> consumerNames
    );

    // PENDING/FAILED 상태인 로그를 RUNNING으로 원자적으로 전환 (처리 권한 획득)
    // 반환값 1 = 권한 획득 성공, 0 = 다른 스레드가 이미 처리 중이거나 SUCCESS
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE EventConsumerLog e SET e.status = com.highlight.highlight_backend.common.logEvent.EventStatus.RUNNING " +
           "WHERE e.eventId = :eventId AND e.consumerName = :consumerName " +
           "AND e.status IN (com.highlight.highlight_backend.common.logEvent.EventStatus.PENDING, " +
           "com.highlight.highlight_backend.common.logEvent.EventStatus.FAILED)")
    int claimAsRunning(@Param("eventId") Long eventId, @Param("consumerName") String consumerName);

    // 타임아웃된 RUNNING 상태(스레드 증발)를 FAILED로 리셋
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE EventConsumerLog e SET e.status = com.highlight.highlight_backend.common.logEvent.EventStatus.FAILED " +
           "WHERE e.status = com.highlight.highlight_backend.common.logEvent.EventStatus.RUNNING " +
           "AND e.updatedAt < :cutoffTime")
    int resetStalledRunning(@Param("cutoffTime") LocalDateTime cutoffTime);
}