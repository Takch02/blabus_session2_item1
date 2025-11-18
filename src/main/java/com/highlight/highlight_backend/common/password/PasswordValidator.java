package com.highlight.highlight_backend.common.password;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * 비밀번호 유효성 검증기
 * 
 * 실제 서비스 수준의 강력한 비밀번호 정책을 검증합니다.
 * 
 * @author 전우선
 * @since 2025.08.13
 */
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {
    
    // 영문 대문자 최소 1개
    private static final String UPPERCASE_PATTERN = "(?=.*[A-Z])";
    // 영문 소문자 최소 1개
    private static final String LOWERCASE_PATTERN = "(?=.*[a-z])";
    // 숫자 최소 1개
    private static final String DIGIT_PATTERN = "(?=.*\\d)";
    // 특수문자 최소 1개 (안전한 특수문자만)
    private static final String SPECIAL_CHAR_PATTERN = "(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?])";
    // 전체 길이 8-128자, 허용된 문자만
    private static final String LENGTH_AND_CHARS_PATTERN = "^[A-Za-z\\d!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]{8,128}$";
    
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        UPPERCASE_PATTERN + LOWERCASE_PATTERN + DIGIT_PATTERN + SPECIAL_CHAR_PATTERN + ".*" + LENGTH_AND_CHARS_PATTERN
    );
    
    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        // 초기화 작업이 필요한 경우 여기에 작성
    }
    
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }
        
        // 길이 검증
        if (password.length() < 8 || password.length() > 128) {
            return false;
        }
        
        // 각 조건별 개별 검증 (더 명확한 에러 처리)
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");
        boolean onlyAllowedChars = password.matches("^[A-Za-z\\d!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]+$");
        
        // 공백 문자 포함 여부 검증
        boolean hasSpace = password.contains(" ");
        
        if (!hasUpper) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("비밀번호에 영문 대문자가 최소 1개 포함되어야 합니다")
                   .addConstraintViolation();
            return false;
        }
        
        if (!hasLower) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("비밀번호에 영문 소문자가 최소 1개 포함되어야 합니다")
                   .addConstraintViolation();
            return false;
        }
        
        if (!hasDigit) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("비밀번호에 숫자가 최소 1개 포함되어야 합니다")
                   .addConstraintViolation();
            return false;
        }
        
        if (!hasSpecial) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("비밀번호에 특수문자가 최소 1개 포함되어야 합니다")
                   .addConstraintViolation();
            return false;
        }
        
        if (hasSpace) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("비밀번호에 공백을 포함할 수 없습니다")
                   .addConstraintViolation();
            return false;
        }
        
        if (!onlyAllowedChars) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("비밀번호에 허용되지 않은 문자가 포함되어 있습니다")
                   .addConstraintViolation();
            return false;
        }
        
        return true;
    }
}