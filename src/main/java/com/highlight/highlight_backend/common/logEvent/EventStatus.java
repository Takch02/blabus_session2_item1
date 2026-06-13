package com.highlight.highlight_backend.common.logEvent;

public enum EventStatus {
    PENDING,  // 처리 대기 중 (초기 상태)
    RUNNING,  // claimRunning()으로 처리 권한 획득 후 실행 중
    SUCCESS,  // 처리 완료
    FAILED,   // 처리 실패 (재시도 대상)
    DEAD      // 재시도 횟수 초과, 영구 실패
}