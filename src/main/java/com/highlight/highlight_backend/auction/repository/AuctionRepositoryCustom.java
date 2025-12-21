package com.highlight.highlight_backend.auction.repository;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.auction.dto.AuctionSearchConditionDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface AuctionRepositoryCustom {
    Slice<Auction> searchAuctions(AuctionSearchConditionDto condition, Pageable pageable);
}
