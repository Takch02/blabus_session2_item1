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

    // 1. 비동기 리스너가 자신의 로그를 찾을 때 사용
    Optional<EventConsumerLog> findByEventIdAndConsumerName(Long eventId, String consumerName);

    // 2. 스케줄러가 재시도 대상을 찾을 때 사용
    // 상태가 PENDING이거나 FAILED인 것 중, updatedAt이 특정 시간(예: 3분 전) 이전인 데이터만 조회
    List<EventConsumerLog> findAllByStatusInAndUpdatedAtBefore(List<EventStatus> statuses, LocalDateTime cutoffTime);

    @Transactional
    @Modifying(clearAutomatically = true) // UPDATE/DELETE 쿼리 실행 시 필수!
    @Query("UPDATE EventConsumerLog e SET e.updatedAt = :time WHERE e.id = :id")
    void forceUpdateUpdatedAt(@Param("id") Long id, @Param("time") LocalDateTime time);

    boolean existsByEventIdAndConsumerName(Long eventId, String consumerName);
}