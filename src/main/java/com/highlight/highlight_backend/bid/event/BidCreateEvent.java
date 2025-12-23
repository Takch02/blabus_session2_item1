package com.highlight.highlight_backend.bid.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * EventListener 로 유저 정보 갱신 및 Websocket 알림 기능을 비동기로 해결하며 Deadlock 및 성능 개선
 */
@Getter
@AllArgsConstructor
public class BidCreateEvent {
    Long userId;
    Long auctionId;
    Long newBidId;
    Long previousBidId;
    BigDecimal bidAmount;
    boolean isNewBidder;
}
