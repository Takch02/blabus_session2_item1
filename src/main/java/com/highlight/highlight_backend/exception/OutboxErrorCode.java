package com.highlight.highlight_backend.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OutboxErrorCode implements ErrorCode {


    FAIL_CONVERT_JSON(HttpStatus.INTERNAL_SERVER_ERROR, "OUTBOX_001", "이벤트 JSON 변환 실패"),
    NOT_FOUND_OUTBOX(HttpStatus.INTERNAL_SERVER_ERROR, "OUTBOX_002", "Outbox 이벤트를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
