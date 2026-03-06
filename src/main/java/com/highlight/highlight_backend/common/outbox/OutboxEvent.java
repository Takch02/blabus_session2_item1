package com.highlight.highlight_backend.common.outbox;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "outbox_event", indexes = {
        @Index(name = "idx_outbox_published", columnList = "published"), // ★ 스케줄러 성능 핵심
        //@Index(name = "idx_outbox_created_at", columnList = "createdAt") // 청소(Delete)용
})
public class OutboxEvent {

    @Id
    private Long id;

    @Column(nullable = false)
    private String aggregateType; // 예: "BID_LOGIC", "BID_NOTI"

    @Column(nullable = false)
    private Long aggregateId;     // 예: userId, bidId

    @Column(nullable = false)
    private String eventType;     // 예: "com.highlight...BidCoreEvent"

    @Lob 
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;       // JSON 데이터

    @Column(nullable = false)
    private boolean published;    // 처리 여부 (false -> true)

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Builder
    public OutboxEvent(Long id, String aggregateType, Long aggregateId, String eventType, String payload) {
        this.id = id;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.published = false; // 기본값은 미처리 상태
    }

    // 처리 완료 마킹
    public void markAsPublished() {
        this.published = true;
    }
}