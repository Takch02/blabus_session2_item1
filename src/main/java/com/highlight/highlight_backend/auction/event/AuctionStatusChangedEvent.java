package com.highlight.highlight_backend.auction.event;

import com.highlight.highlight_backend.auction.domain.Auction.AuctionStatus;
import com.highlight.highlight_backend.product.domian.Product.Category;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuctionStatusChangedEvent {
    private Long auctionId;
    private AuctionStatus previousStatus;
    private AuctionStatus newStatus;
    private Category category;
}
