package com.highlight.highlight_backend.common.logEvent;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(
    name = "event_consumer_log",
    uniqueConstraints = {
        // 동일한 이벤트에 대해 같은 수신자가 대기표를 두 번 뽑지 못하도록 방어 (멱등성)
        @UniqueConstraint(columnNames = {"event_id", "consumer_name"})
    }
)
public class EventConsumerLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId; // Outbox ID

    @Column(name = "consumer_name", nullable = false, length = 50)
    private String consumerName; // "USER_PARTICIPATION_UPDATE", "WEBSOCKET_BROADCAST"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status;

    @Column(columnDefinition = "TEXT")
    private String errorMessage; // Exception 메시지 저장

    @Column(nullable = false)
    private int retryCount; // 재시도할 때마다 1씩 증가

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt; // PENDING 상태로 너무 오래 머물러 있는지(스레드 증발) 판단할 때 사용

    @Builder
    public EventConsumerLog(Long eventId, String consumerName) {
        this.eventId = eventId;
        this.consumerName = consumerName;
        this.status = EventStatus.PENDING; // 초기 상태는 무조건 PENDING
        this.retryCount = 0;
    }

    // 비즈니스 로직 성공 시 호출
    public void markAsSuccess() {
        this.status = EventStatus.SUCCESS;
        this.errorMessage = null; // 혹시 이전에 FAILED였다가 성공했다면 에러 메시지 초기화
    }

    // 비즈니스 로직 실패 시 호출
    public void markAsFailed(String errorMessage) {
        this.status = EventStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    // 스케줄러가 재시도할 때 호출
    public void increaseRetryCount() {
        this.retryCount++;
    }
}