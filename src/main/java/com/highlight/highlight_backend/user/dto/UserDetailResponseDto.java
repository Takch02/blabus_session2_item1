package com.highlight.highlight_backend.user.dto;

import com.highlight.highlight_backend.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class UserDetailResponseDto {

    private Long id;
    private String userId;
    private String nickname;
    private String phoneNumber;
    private boolean isPhoneVerified;
    private boolean isOver14;
    private boolean agreedToTerms;
    private boolean marketingEnabled;
    private boolean eventSnsEnabled;

    public static UserDetailResponseDto from(User user) {
        return UserDetailResponseDto.builder()
                .id(user.getId())
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .phoneNumber(user.getPhoneNumber())
                .isPhoneVerified(user.isPhoneVerified())
                .isOver14(user.isOver14())
                .agreedToTerms(user.isAgreedToTerms())
                .marketingEnabled(user.isMarketingEnabled())
                .eventSnsEnabled(user.isEventSnsEnabled())
                .build();
    }
}