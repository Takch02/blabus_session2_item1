package com.highlight.highlight_backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserUpdateRequestDto {


    @NotBlank(message = "닉네임을 입력해주세요.")
    @Size(min = 2, max = 8, message = "닉네임은 2자 이상 8자 이하로 입력해주세요.")
    private String nickname;

    @NotBlank(message = "휴대폰 번호를 입력해주세요.")
    @Pattern(regexp = "^010\\d{8}$", message = "휴대폰 번호 형식에 맞지 않습니다.")
    private String phoneNumber;

    @NotNull(message = "마케팅 이용약관 동의 여부를 입력해주세요.")
    private Boolean marketingEnabled;

    @NotNull(message = "SNS 광고 동의 여부를 입력해주세요.")
    private Boolean eventSnsEnabled;
}
