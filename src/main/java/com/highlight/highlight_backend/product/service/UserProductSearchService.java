package com.highlight.highlight_backend.product.service;

import com.highlight.highlight_backend.product.dto.ProductResponseDto;
import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.auction.repository.AuctionRepository;
import com.highlight.highlight_backend.bid.repository.BidRepository;
import com.highlight.highlight_backend.exception.BusinessException;
import com.highlight.highlight_backend.exception.ProductErrorCode;
import com.highlight.highlight_backend.product.domian.Product;
import com.highlight.highlight_backend.product.repository.ProductRepository;
import com.highlight.highlight_backend.product.dto.UserAuctionDetailResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserProductSearchService {

    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final ProductRepository productRepository;

    /**
     * 경매 ID를 통해 상품의 상세 정보를 가져옴
     */
    public UserAuctionDetailResponseDto getProductsDetail(Long auctionId) {
        Optional<Auction> auction = auctionRepository.findById(auctionId);
        BigDecimal currentPrice = auction.get().getCurrentHighestBid();
        // 3. 적립될 포인트를 계산합니다. (기본값은 0으로 설정)
        BigDecimal pointReward = BigDecimal.ZERO;

        UserAuctionDetailResponseDto userAuctionDetailResponseDto = UserAuctionDetailResponseDto.from(auction.get());

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

}
