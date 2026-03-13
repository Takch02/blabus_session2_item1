package com.highlight.highlight_backend.common.logEvent;

public record RetryTargetDto(
        Long eventId,
        String consumerName,
        int retryCount
) {
}