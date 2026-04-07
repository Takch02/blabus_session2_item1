package com.highlight.highlight_backend.auction.service;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.auction.dto.AuctionPageResponse;
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

import java.util.List;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserAuctionService {

    private final AuctionRepository auctionRepository;
    private final AuctionCountService auctionCountService;

    /**
     * 필터링, 정렬할 값을 가져오고 정렬한다.
     * queryDSL 로 리팩토링.
     */
    public AuctionPageResponse getProductsFiltered(
            AuctionSearchConditionDto conditionDto, Pageable pageable) {

        // DB 조회
        Slice<Auction> slice = auctionRepository.searchAuctions(conditionDto, pageable);

        // Redis에서 count 조회
        /*long totalCount = auctionCountService.getCount(
                conditionDto.getStatus(),
                conditionDto.getCategory()
        );*/
        long totalCount = auctionRepository.getCount();

        // DTO 변환
        List<UserAuctionResponseDto> content = slice.getContent()
                .stream()
                .map(auction -> UserAuctionResponseDto.fromWithCalculatedCount(
                        auction, auction.getTotalBids()))
                .toList();

        return new AuctionPageResponse(
                content,
                slice.hasNext(),
                pageable.getPageNumber(),
                totalCount
        );
    }

    /**
     * 입찰 시 비관 락을 얻기 위한 service
     */
    public Auction getAuctionWithLockOrThrow(Long auctionId) {
        return auctionRepository.findByIdWithLock(auctionId)
                .orElseThrow(() -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));
    }

    /**
     * Fetch join으로 Product도 가져옴
     */
    public Auction getAuctionOrThrow(Long auctionId) {
        return auctionRepository.findByIdWithProduct(auctionId)
                .orElseThrow(() -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));
    }

    public Auction findAuctionOrThrow(Long auctionId) {
        return auctionRepository.findById(auctionId)
                .orElseThrow(() -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public AuctionStatsDto getAuctionStats(Long auctionId) {
        Long totalBidders = auctionRepository.findTotalBiddersByAuctionId(auctionId);
        Long totalBids = auctionRepository.findTotalBidsByAuctionId(auctionId);
        return new AuctionStatsDto(totalBidders, totalBids);
    }
}
