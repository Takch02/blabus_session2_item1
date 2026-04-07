package com.highlight.highlight_backend.auction.dto;

import com.highlight.highlight_backend.product.dto.UserAuctionResponseDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AuctionPageResponse {
    private List<UserAuctionResponseDto> content;
    private boolean hasNext;
    private int currentPage;
    private long totalCount; // Redis에서 가져온 count
}