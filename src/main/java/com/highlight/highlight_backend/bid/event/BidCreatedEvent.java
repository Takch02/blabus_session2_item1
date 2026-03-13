package com.highlight.highlight_backend.bid.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class BidCreatedEvent {
    private long outboxId;
    private long userId;
    private Long auctionId;
    private Long bidId;
    private Long previousBidId;
    private BigDecimal bidAmount;
    private boolean isNewBidder;
    private String userNickname;
}