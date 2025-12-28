package com.highlight.highlight_backend.bid.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * EventListener 로 유저 정보 갱신 및 Websocket 알림 기능을 비동기로 해결하며 Deadlock 및 성능 개선
 */
@Getter
public class BidNotificationEvent {
    Long userId;
    Long auctionId;
    Long newBidId;
    Long previousBidId;
    BigDecimal bidAmount;
    boolean isNewBidder;

    @Setter
    Long outboxId;

    public BidNotificationEvent (Long userId, Long auctionId, Long newBidId, Long previousBidId,
                                 BigDecimal bidAmount, boolean isNewBidder) {
        this.userId = userId;
        this.auctionId = auctionId;
        this.newBidId = newBidId;
        this.previousBidId = previousBidId;
        this.bidAmount = bidAmount;
        this.isNewBidder = isNewBidder;
    }
}
