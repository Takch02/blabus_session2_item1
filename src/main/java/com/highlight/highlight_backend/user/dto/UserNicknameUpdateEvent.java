package com.highlight.highlight_backend.user.dto;

import lombok.Getter;

@Getter
public class UserNicknameUpdateEvent {

    private Long userId;
    private String nickname;
    private Long outboxId;

    public UserNicknameUpdateEvent (Long userId, String nickname, Long outboxId) {
        this.userId = userId;
        this.nickname = nickname;
        this.outboxId = outboxId;
    }
}
