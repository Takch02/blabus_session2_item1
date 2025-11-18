package com.highlight.highlight_backend.admin.user.dto;

import com.highlight.highlight_backend.common.password.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 관리자 계정 생성 요청 DTO
 * 
 * @author 전우선
 * @since 2025.08.13
 */
@Getter
@NoArgsConstructor
@ToString(exclude = "password")  // 비밀번호는 로그에서 제외
public class AdminCreateRequestDto {
    
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
    @ValidPassword
    private String password;
    
    /**
     * 관리자 이름
     */
    @NotBlank(message = "관리자 이름은 필수입니다")
    @Size(max = 30, message = "관리자 이름은 30자를 초과할 수 없습니다")
    private String adminName;
    
    /**
     * 관리자 이메일
     */
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Size(max = 100, message = "이메일은 100자를 초과할 수 없습니다")
    private String email;
    
}