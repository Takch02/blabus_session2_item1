package com.highlight.highlight_backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import lombok.extern.slf4j.Slf4j;

import jakarta.validation.ConstraintViolationException;

/**
 * 전역 예외 처리 핸들러
 * 
 * 애플리케이션 전체에서 발생하는 예외를 중앙에서 처리하여
 * 일관된 응답 형식을 제공합니다.
 * 
 * @author 전우선
 * @since 2025.08.08
 */
@Slf4j
@RestControllerAdvice
public class
GlobalExceptionHandler {

    /**
     * 비즈니스 예외 처리
     * 
     * 비즈니스 로직에서 발생하는 예외를 처리합니다.
     * 클라이언트에게 명확한 에러 정보를 제공합니다.
     * 
     * @param e 비즈니스 예외
     * @return 에러 응답
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.error("Business exception occurred: {}", e.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(e.getErrorCode().getCode())
                .message(e.getErrorCode().getMessage())
                .build();
        
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(errorResponse);
    }

    /**
     * @Valid 어노테이션으로 유효성 검사 실패 시 발생하는 예외 처리
     * 
     * @param e MethodArgumentNotValidException
     * @return 에러 응답
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("Validation failed: {}", e.getMessage());
        
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("입력값이 올바르지 않습니다.");
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("VALIDATION_FAILED")
                .message(errorMessage)
                .build();
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }
    
    /**
     * @ModelAttribute 바인딩 시 유효성 검사 실패 예외 처리
     * 
     * @param e BindException
     * @return 에러 응답
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException e) {
        log.warn("Bind exception occurred: {}", e.getMessage());
        
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("입력값이 올바르지 않습니다.");
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("BIND_ERROR")
                .message(errorMessage)
                .build();
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }
    
    /**
     * Bean Validation 제약 조건 위반 예외 처리
     * 
     * @param e ConstraintViolationException
     * @return 에러 응답
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("Constraint violation: {}", e.getMessage());
        
        String errorMessage = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .findFirst()
                .orElse("제약 조건을 위반했습니다.");
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("CONSTRAINT_VIOLATION")
                .message(errorMessage)
                .build();
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }
    
    /**
     * HTTP 메서드가 지원되지 않을 때 발생하는 예외 처리
     * 
     * @param e HttpRequestMethodNotSupportedException
     * @return 에러 응답
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("Method not supported: {}", e.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("METHOD_NOT_ALLOWED")
                .message("지원하지 않는 HTTP 메서드입니다.")
                .build();
        
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(errorResponse);
    }
    
    /**
     * 필수 요청 파라미터가 누락되었을 때 발생하는 예외 처리
     * 
     * @param e MissingServletRequestParameterException
     * @return 에러 응답
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e) {
        log.warn("Missing parameter: {}", e.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("MISSING_PARAMETER")
                .message("필수 파라미터가 누락되었습니다: " + e.getParameterName())
                .build();
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }
    
    /**
     * 메서드 인수 타입이 맞지 않을 때 발생하는 예외 처리
     * 
     * @param e MethodArgumentTypeMismatchException
     * @return 에러 응답
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("Type mismatch: {}", e.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("TYPE_MISMATCH")
                .message("파라미터 타입이 올바르지 않습니다: " + e.getName())
                .build();
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }
    
    /**
     * JSON 파싱 오류 등 HTTP 메시지를 읽을 수 없을 때 발생하는 예외 처리
     * 
     * @param e HttpMessageNotReadableException
     * @return 에러 응답
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("HTTP message not readable: {}", e.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("MALFORMED_JSON")
                .message("잘못된 JSON 형식입니다.")
                .build();
        
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }
    
    /**
     * 핸들러를 찾을 수 없을 때 발생하는 예외 처리 (404 에러)
     * 
     * @param e NoHandlerFoundException
     * @return 에러 응답
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(NoHandlerFoundException e) {
        log.warn("No handler found: {}", e.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("NOT_FOUND")
                .message("요청한 리소스를 찾을 수 없습니다.")
                .build();
        
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }
    
    /**
     * 인증 실패 예외 처리
     * 
     * @param e AuthenticationException
     * @return 에러 응답
     */
    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException e) {
        log.warn("Authentication failed: {}", e.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("AUTHENTICATION_FAILED")
                .message("인증에 실패했습니다.")
                .build();
        
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(errorResponse);
    }
    
    /**
     * 권한 부족 예외 처리
     * 
     * @param e AccessDeniedException
     * @return 에러 응답
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("ACCESS_DENIED")
                .message("접근 권한이 없습니다.")
                .build();
        
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(errorResponse);
    }
    
    /**
     * 예상하지 못한 예외 처리
     * 
     * 시스템에서 예상하지 못한 예외가 발생했을 때 처리합니다.
     * 상세 에러 정보는 로그에 기록하고, 클라이언트에게는 일반적인 메시지를 제공합니다.
     * 
     * @param e 예외
     * @return 에러 응답
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected exception occurred", e);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("INTERNAL_SERVER_ERROR")
                .message("서버 내부 오류가 발생했습니다.")
                .build();
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }
}