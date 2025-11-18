package com.highlight.highlight_backend.common.password;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 비밀번호 유효성 검증 어노테이션
 * 
 * 실제 서비스 수준의 비밀번호 정책을 적용합니다:
 * - 8자 이상 128자 이하
 * - 영문 대문자 최소 1개
 * - 영문 소문자 최소 1개  
 * - 숫자 최소 1개
 * - 특수문자 최소 1개
 * 
 * @author 전우선
 * @since 2025.08.13
 */
@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {
    String message() default "비밀번호는 8자 이상이며, 영문 대소문자, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}