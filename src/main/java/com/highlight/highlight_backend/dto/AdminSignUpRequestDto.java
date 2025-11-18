package com.highlight.highlight_backend.dto;

import com.highlight.highlight_backend.common.password.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 관리자 간단 회원가입 요청 DTO
 * 
 * @author 전우선
 * @since 2025.08.15
 */
@Getter
@NoArgsConstructor
@ToString(exclude = "password")
public class AdminSignUpRequestDto {
    
    /**
     * 관리자 ID (로그인용)
     */
    @NotBlank(message = "관리자 ID는 필수입니다")
    @Size(min = 3, max = 50, message = "관리자 ID는 3자 이상 50자 이하여야 합니다")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "관리자 ID는 영문자, 숫자, 언더스코어(_), 하이픈(-)만 사용 가능합니다")
    private String adminId;
    
    /**
     * 관리자 비밀번호
     */
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 128, message = "비밀번호는 8자 이상 128자 이하여야 합니다")
    @ValidPassword
    private String password;
    
}