package com.highlight.highlight_backend.auction.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuctionStatsDto {
    private Long totalBidders;
    private Long totalBids;
}
