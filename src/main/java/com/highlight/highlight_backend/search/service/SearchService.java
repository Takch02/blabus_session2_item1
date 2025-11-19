package com.highlight.highlight_backend.search.service;


import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.admin.auction.repository.AuctionRepository;
import com.highlight.highlight_backend.admin.product.domian.Product;
import com.highlight.highlight_backend.admin.product.dto.ProductResponseDto;
import com.highlight.highlight_backend.dto.ViewTogetherProductResponseDto;
import com.highlight.highlight_backend.exception.BusinessException;
import com.highlight.highlight_backend.exception.ProductErrorCode;
import com.highlight.highlight_backend.admin.product.repository.ProductRepository;
import com.highlight.highlight_backend.search.dto.UserAuctionResponseDto;
import com.highlight.highlight_backend.repository.BidRepository;
import com.highlight.highlight_backend.repository.spec.AuctionSpecs;
import com.highlight.highlight_backend.search.dto.UserAuctionDetailResponseDto;
import com.highlight.highlight_backend.search.repository.SearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 일반 User 인증없이 보여줄 정보를 처리하는 Service
 *
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SearchService {

    private final SearchRepository searchRepository;
    private final BidRepository bidRepository;
    private final ProductRepository productRepository;
    private final AuctionRepository auctionRepository;

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
        Page<Auction> auctionPage = searchRepository.findAll(spec, newPageable);

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

    /**
     * 경매 ID를 통해 상품의 상세 정보를 가져옴
     */
    public UserAuctionDetailResponseDto getProductsDetail(Long auctionId) {
        Auction auction = searchRepository.findOne(auctionId);
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





    /**
     * 관련 상품 추천 조회
     *
     * @param productId 기준 상품 ID
     * @param size 추천 상품 개수
     * @return 추천 상품 목록
     */
    public Page<ProductResponseDto> getRecommendedProducts(Long productId, int size) {
        log.info("관련 상품 추천 조회: {} (개수: {})", productId, size);

        // 1. 기준 상품 조회
        Product baseProduct = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        // 2. 추천 로직: 동일 카테고리 또는 동일 브랜드 상품 조회
        List<Product> recommendedProducts = productRepository.findRecommendedProducts(
                productId,
                baseProduct.getCategory(),
                baseProduct.getBrand(),
                PageRequest.of(0, size)
        );

        // 3. DTO 변환
        List<ProductResponseDto> responseDtos = recommendedProducts.stream()
                .map(ProductResponseDto::from)
                .toList();

        // 4. Page 객체로 변환
        return new PageImpl<>(responseDtos, PageRequest.of(0, size), responseDtos.size());
    }


    /**
     * 함께 본 상품 추천 조회
     *
     * 카테고리 & 브렌드가 유사한 상품을 조회함
     **/
    public Page<ViewTogetherProductResponseDto> getViewedTogetherProducts(Long productId, int size) {
        // 1. 기준 상품 조회
        Product baseProduct = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        List<Product> similarProducts = productRepository.findRecommendedProductsWithAuction(
                baseProduct.getId(),
                baseProduct.getCategory(),
                baseProduct.getBrand(),
                PageRequest.of(0, size)
        );

        // DTO 변환
        List<ViewTogetherProductResponseDto> result = similarProducts.stream()
                .map(product -> {
                    // 활성 경매 혹은 예약 경매 찾기
                    Auction auction = auctionRepository.findActiveOrScheduledAuctionByProductId(product.getId())
                            .orElse(null);

                    // 경매가 없으면 추천에서 제외할지, 아니면 그냥 보여줄지 정책 결정 (여기선 보여준다고 가정)
                    int bidCount = (auction != null) ? bidRepository.countBidsByAuction(auction).intValue() : 0;

                    return ViewTogetherProductResponseDto.fromProductWithCalculatedCount(
                            product, auction, BigDecimal.ZERO, bidCount
                    );
                })
                .toList();

        return new PageImpl<>(result, PageRequest.of(0, size), result.size());
    }


}
