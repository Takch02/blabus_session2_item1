package com.highlight.highlight_backend.common.verification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 휴대폰 인증 코드 확인 DTO
 *
 * @author 전우선
 * @since 2025.08.16
 */
@Getter
@NoArgsConstructor
public class PhoneVerificationRequestDto {

    /**
     * 휴대폰 번호
     */
    @NotBlank(message = "휴대폰 번호는 필수입니다")
    @Pattern(regexp = "^010\\d{8}$", message = "휴대폰 번호 형식이 올바르지 않습니다.")
    private String phoneNumber;

    private String verificationCode;
}