package com.highlight.highlight_backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 로그인 요청 DTO
 * 
 * 관리자 로그인을 위한 ID/PW 입력 데이터를 담는 클래스입니다.
 * 
 * @author 전우선
 * @since 2025.08.13
 */
@Getter
@NoArgsConstructor
@ToString(exclude = "password")  // 비밀번호는 로그에서 제외
public class LoginRequestDto {
    
    /**
     * 관리자 ID (이메일 또는 아이디)
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
    private String password;
}