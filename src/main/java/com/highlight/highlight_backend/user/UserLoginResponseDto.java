package com.highlight.highlight_backend.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class UserLoginResponseDto {


    private String userId;

    private String nickname;

    private String accessToken;

    private String refreshToken;

}
