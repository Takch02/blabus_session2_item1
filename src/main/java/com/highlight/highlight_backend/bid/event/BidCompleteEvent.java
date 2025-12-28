package com.highlight.highlight_backend.bid.event;

import lombok.Getter;
import lombok.Setter;

@Getter
public class BidCompleteEvent {
    Long userId;

    @Setter
    Long outboxId;

    public BidCompleteEvent(Long userId) {
        this.userId = userId;
    }

}
