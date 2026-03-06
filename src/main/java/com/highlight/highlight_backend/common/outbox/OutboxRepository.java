package com.highlight.highlight_backend.common.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findAllByPublishedFalseAndCreatedAtBefore(LocalDateTime time);


    @Modifying
    @Transactional // 즉시 커밋
    @Query("UPDATE OutboxEvent o SET o.createdAt = :pastTime WHERE o.id = :id")
    void forceUpdateCreatedAt(Long id, LocalDateTime pastTime);
}