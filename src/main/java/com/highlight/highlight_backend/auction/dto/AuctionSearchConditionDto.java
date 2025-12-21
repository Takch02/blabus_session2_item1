package com.highlight.highlight_backend.auction.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AuctionSearchConditionDto {
    private String category;
    private String brand;
    private Long minPrice;
    private Long maxPrice;
    private Boolean isPremium;
    private String status;
}