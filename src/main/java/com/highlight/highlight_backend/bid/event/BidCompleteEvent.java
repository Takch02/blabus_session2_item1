package com.highlight.highlight_backend.bid.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BidCompleteEvent {
    Long userId;
    Long outboxId;

}
