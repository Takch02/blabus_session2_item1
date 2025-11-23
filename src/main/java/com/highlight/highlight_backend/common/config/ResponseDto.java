package com.highlight.highlight_backend.common.config;

import com.highlight.highlight_backend.common.jwt.RefreshTokenResponseDto;
import lombok.Getter;

/**
 * 표준 API 응답 DTO
 * 
 * 모든 API 응답에 일관된 형식을 제공하기 위한 래퍼 클래스입니다.
 * 성공/실패 여부, 데이터, 메시지를 포함합니다.
 * 
 * @param <T> 응답 데이터의 타입
 * @author 전우선
 * @since 2025.08.08
 */
@Getter
public class ResponseDto<T> {
    
    /**
     * API 호출 성공 여부
     */
    private boolean success;
    
    /**
     * 응답 데이터
     */
    private T data;
    
    /**
     * 응답 메시지
     */
    private String message;
    
    /**
     * ResponseDto 생성자
     * 
     * @param success 성공 여부
     * @param data 응답 데이터
     * @param message 응답 메시지
     */
    private ResponseDto(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }
    
    /**
     * 데이터와 함께 성공 응답을 생성합니다.
     * 
     * @param data 응답 데이터
     * @param <T> 데이터 타입
     * @return 성공 응답 DTO
     */
    public static <T> ResponseDto<T> success(T data) {
        return new ResponseDto<>(true, data, "성공");
    }
    
    /**
     * 데이터와 커스텀 메시지와 함께 성공 응답을 생성합니다.
     * 
     * @param data 응답 데이터
     * @param message 커스텀 메시지
     * @param <T> 데이터 타입
     * @return 성공 응답 DTO
     */
    public static <T> ResponseDto<T> success(T data, String message) {
        return new ResponseDto<>(true, data, message);
    }
    
    /**
     * 데이터 없이 성공 응답을 생성합니다.
     * 
     * @return 성공 응답 DTO
     */
    public static ResponseDto<Void> success() {
        return new ResponseDto<>(true, null, "성공");
    }
    
    /**
     * 커스텀 메시지와 함께 성공 응답을 생성합니다.
     * 
     * @param message 커스텀 메시지
     * @return 성공 응답 DTO
     */
    public static ResponseDto<Void> success(String message) {
        return new ResponseDto<>(true, null, message);
    }

    public static ResponseDto<RefreshTokenResponseDto> error(String s) {
        return new ResponseDto<>(false, null, s);
    }
}