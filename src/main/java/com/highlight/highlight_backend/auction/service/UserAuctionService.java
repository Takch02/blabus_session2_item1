package com.highlight.highlight_backend.auction.service;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.auction.dto.AuctionSearchConditionDto;
import com.highlight.highlight_backend.auction.dto.AuctionStatsDto;
import com.highlight.highlight_backend.auction.repository.AuctionRepository;
import com.highlight.highlight_backend.exception.AuctionErrorCode;
import com.highlight.highlight_backend.exception.BusinessException;
import com.highlight.highlight_backend.product.dto.UserAuctionResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserAuctionService {

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

    /**
     * 입찰 시 비관 락을 얻기 위한 service
     */
    public Auction getAuctionWithLockOrThrow(Long auctionId) {
        return auctionRepository.findByIdWithLock(auctionId)
                .orElseThrow(() -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));
    }

    public Auction getAuctionOrThrow(Long auctionId) {
        return auctionRepository.findById(auctionId)
                .orElseThrow(() -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));
    }

    public Auction findAuctionOrThrow(Long auctionId) {
        return auctionRepository.findById(auctionId)
                .orElseThrow(() -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public AuctionStatsDto getAuctionStats(Long auctionId) {
        Long totalBidders = auctionRepository.findAuctionByTotalBidders(auctionId);
        Long totalBids = auctionRepository.findAuctionByTotalBids(auctionId);
        return new AuctionStatsDto(totalBidders, totalBids);
    }
}
