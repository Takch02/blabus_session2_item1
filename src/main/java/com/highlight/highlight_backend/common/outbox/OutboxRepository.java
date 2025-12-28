package com.highlight.highlight_backend.common.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findAllByPublishedFalseAndCreatedAtBefore(LocalDateTime time);
}