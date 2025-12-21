package com.highlight.highlight_backend.auction.service;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.auction.dto.AuctionSearchConditionDto;
import com.highlight.highlight_backend.auction.repository.AuctionRepository;
import com.highlight.highlight_backend.auction.repository.AuctionRepositoryCustom;
import com.highlight.highlight_backend.auction.repository.AuctionRepositoryImpl;
import com.highlight.highlight_backend.product.dto.UserAuctionResponseDto;
import com.querydsl.core.QueryFactory;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserAuctionSearchService {

    private final AuctionRepository auctionRepository;

    /**
     * 필터링, 정렬할 값을 가져오고 정렬한다.
     * queryDSL 로 리팩토링.
     */
    public Slice<UserAuctionResponseDto> getProductsFiltered(
            AuctionSearchConditionDto conditionDto, Pageable pageable) {

        Slice<Auction> slices = auctionRepository.searchAuctions(conditionDto, pageable);
        // DTO로 변환하여 반환
        return slices.map(auction -> UserAuctionResponseDto.fromWithCalculatedCount(auction, auction.getTotalBids()));
    }
}
