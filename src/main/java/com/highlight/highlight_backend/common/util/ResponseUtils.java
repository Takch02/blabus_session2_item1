package com.highlight.highlight_backend.common.util;

import com.highlight.highlight_backend.dto.ResponseDto;
import org.springframework.http.ResponseEntity;

/**
 * HTTP 응답 관련 유틸리티 클래스
 * 
 * Controller에서 공통적으로 사용되는 ResponseEntity 생성 작업을 처리합니다.
 * 
 * @author 전우선
 * @since 2025.08.16
 */
public class ResponseUtils {
    
    /**
     * 성공 응답을 생성합니다.
     * 
     * @param data 응답 데이터
     * @param message 성공 메시지
     * @param <T> 응답 데이터 타입
     * @return ResponseEntity<ResponseDto<T>>
     */
    public static <T> ResponseEntity<ResponseDto<T>> success(T data, String message) {
        return ResponseEntity.ok(ResponseDto.success(data, message));
    }
    
    /**
     * 데이터 없이 메시지만 포함하는 성공 응답을 생성합니다.
     * 
     * @param message 성공 메시지
     * @return ResponseEntity<ResponseDto<String>>
     */
    public static ResponseEntity<ResponseDto<String>> successWithMessage(String message) {
        return ResponseEntity.ok(ResponseDto.success("SUCCESS", message));
    }
    
    /**
     * 데이터와 함께 성공 응답을 생성합니다 (기본 성공 메시지 사용).
     * 
     * @param data 응답 데이터
     * @param <T> 응답 데이터 타입
     * @return ResponseEntity<ResponseDto<T>>
     */
    public static <T> ResponseEntity<ResponseDto<T>> success(T data) {
        return ResponseEntity.ok(ResponseDto.success(data, "요청이 성공적으로 처리되었습니다."));
    }
}