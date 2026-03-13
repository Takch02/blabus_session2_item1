package com.highlight.highlight_backend.common.logEvent;

public enum EventStatus {
    PENDING,  // 동기 로직에서 대기표를 뽑은 직후의 상태 (스레드 실행 전)
    SUCCESS,  // 비동기 로직이 에러 없이 완벽하게 처리된 상태
    FAILED    // 비동기 로직 실행 중 에러가 발생한 상태 (재시도 대상)
}