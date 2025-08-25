package com.highlight.highlight_backend.service;

import com.highlight.highlight_backend.domain.Auction;
import com.highlight.highlight_backend.dto.UserAuctionDetailResponseDto;
import com.highlight.highlight_backend.dto.UserAuctionResponseDto;
import com.highlight.highlight_backend.repository.user.UserAuctionRepository;
import com.highlight.highlight_backend.repository.BidRepository;
import com.highlight.highlight_backend.repository.spec.AuctionSpecs; // import 추가
import lombok.RequiredArgsConstructor; // AllArgsConstructor 대신 사용
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification; // import 추가
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 일반 User 인증없이 보여줄 정보를 처리하는 Service
 *
 *
 * @author 탁찬홍
 * @since 2025.08.14
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAuctionSearchService {

    private final UserAuctionRepository userAuctionRepository;
    private final BidRepository bidRepository;

    /**
     * 필터링, 정렬할 값을 가져오고 정렬한다.
     * JapRepository 에서 Specification 을 이용하여 필터링
     * @return UserAuctionResponseDto 반환
     */

    public Page<UserAuctionResponseDto> getProductsFiltered(
            String category, Long minPrice, Long maxPrice, String brand, String eventName,
            Boolean isPremium, String status, String sortCode, Pageable pageable) {

        // 1. Specification 조합 -> Where 문을 동적으로 만듦
        Specification<Auction> spec = Specification.where(null);

        if (StringUtils.hasText(category)) {
            spec = spec.and(AuctionSpecs.hasCategory(category));
        }
        if (StringUtils.hasText(brand)) {
            spec = spec.and(AuctionSpecs.hasBrand(brand));
        }
        if (StringUtils.hasText(eventName)) {
            spec = spec.and(AuctionSpecs.hasEventName(eventName));
        }
        if (minPrice != null || maxPrice != null) {
            spec = spec.and(AuctionSpecs.betweenPrice(minPrice, maxPrice));
        }
        if (isPremium != null) {
            spec = spec.and(AuctionSpecs.isPremium(isPremium));
        }
        if (status != null) {
            spec = spec.and(AuctionSpecs.hasAuctionStatus(status));
        }

        // 2. 정렬(Sort) 조건 적용
        Sort sort = getSort(sortCode);
        Pageable newPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        // 3. Repository 호출
        Page<Auction> auctionPage = userAuctionRepository.findAll(spec, newPageable);

        // 4. DTO로 변환하여 반환 (사용자별 최신 입찰 기준 통계 적용)
        return auctionPage.map(auction -> {
            // 각 경매의 실제 입찰 수를 계산 (사용자별 최신 기준)
            Long bidCount = bidRepository.countBidsByAuction(auction);
            return UserAuctionResponseDto.fromWithCalculatedCount(auction, bidCount.intValue());
        });
    }

    private Sort getSort(String sortCode) {
        if (!StringUtils.hasText(sortCode)) {
            return Sort.by(Sort.Direction.DESC, "createdAt"); // 기본 정렬: 최신순
        }

        switch (sortCode.toLowerCase()) {
            case "ending": // 마감임박순
                return Sort.by(Sort.Direction.ASC, "endTime");
            case "popular": // 인기순 (예: 입찰 수 기준)
                return Sort.by(Sort.Direction.DESC, "totalBids");
            case "newest": // 신규순
            default:
                return Sort.by(Sort.Direction.DESC, "createdAt");
        }
    }

    public UserAuctionDetailResponseDto getProductsDetail(Long auctionId) {
        Auction auction = userAuctionRepository.findOne(auctionId);
        BigDecimal currentPrice = auction.getCurrentHighestBid();
        // 3. 적립될 포인트를 계산합니다. (기본값은 0으로 설정)
        BigDecimal pointReward = BigDecimal.ZERO;

        UserAuctionDetailResponseDto userAuctionDetailResponseDto = UserAuctionDetailResponseDto.from(auction);

        // 현재 입찰가가 존재할 경우에만 계산을 수행합니다.
        if (currentPrice != null) {
            pointReward = currentPrice
                    .multiply(new BigDecimal("0.01"))  // 1% 계산
                    .setScale(0, RoundingMode.DOWN);   // 소수점 버림

        }
        userAuctionDetailResponseDto.setPoint(pointReward);
        return userAuctionDetailResponseDto;
    }
}