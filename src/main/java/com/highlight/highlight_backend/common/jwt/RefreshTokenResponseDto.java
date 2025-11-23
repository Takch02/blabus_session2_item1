package com.highlight.highlight_backend.common.jwt;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RefreshTokenResponseDto {

    private String adminId;

    private String adminName;

    private String accessToken;

    private String refreshToken;

    private String message;
}
