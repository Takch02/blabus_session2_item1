package com.highlight.highlight_backend.mypage.service;

import com.highlight.highlight_backend.domain.Bid;
import com.highlight.highlight_backend.domain.Product;
import com.highlight.highlight_backend.user.domain.User;
import com.highlight.highlight_backend.mypage.dto.MyPagePremiumImageResponseDto;
import com.highlight.highlight_backend.mypage.dto.MyPageResponseDto;
import com.highlight.highlight_backend.exception.BusinessException;
import com.highlight.highlight_backend.exception.UserErrorCode;
import com.highlight.highlight_backend.admin.auction.repository.AuctionRepository;
import com.highlight.highlight_backend.repository.BidRepository;
import com.highlight.highlight_backend.repository.ProductImageRepository;
import com.highlight.highlight_backend.repository.ProductRepository;
import com.highlight.highlight_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MypageService {


    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;


    /**
     * 마이페이지 정보 조회
     *
     * @param userId 사용자 ID
     * @return 마이페이지 정보
     */
    @Transactional(readOnly = true)
    public MyPageResponseDto getMyPageInfo(Long userId) {
        log.info("마이페이지 정보 조회: 사용자ID={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        return MyPageResponseDto.from(user);
    }

    /**
     * 사용자가 낙찰한 프리미엄 상품들의 정보 조회
     *
     * @param userId 사용자 ID
     * @return 프리미엄 상품 정보 목록
     */
    @Transactional(readOnly = true)
    public List<MyPagePremiumImageResponseDto> getMyPagePremiumImages(Long userId) {
        log.info("마이페이지 프리미엄 상품 정보 조회: 사용자ID={}", userId);

        // 1. 사용자 존재 확인
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        // 2. 사용자가 낙찰한 경매들 중 프리미엄 상품들 조회
        List<Long> premiumProductIds = auctionRepository.findPremiumProductIdsByUserId(userId);

        // 없을 경우 빈 리스트 반환
        if (premiumProductIds.isEmpty()) {
            log.info("사용자가 낙찰한 프리미엄 상품이 없습니다: 사용자ID={}", userId);
            return List.of();
        }

        // 3. 프리미엄 상품들의 정보 조회 (한 번에 조회하여 N+1 문제 방지)
        List<MyPagePremiumImageResponseDto> list = new ArrayList<>();

        for (Long productId : premiumProductIds) {
            try {
                // 상품 정보 조회
                Product product = productRepository.findById(productId).orElse(null);

                if (product == null) {
                    log.warn("상품을 찾을 수 없습니다: productId={}", productId);
                    continue;
                }

                // 낙찰 정보 조회 (해당 상품의 경매에서 사용자가 낙찰한 입찰)
                Bid winningBid = bidRepository.findWinningBidByProductIdAndUserId(productId, userId)
                        .orElse(null);

                if (winningBid == null) {
                    log.warn("낙찰 정보를 찾을 수 없습니다: productId={}, userId={}", productId, userId);
                    continue;
                }

                // 대표 이미지 URL 조회
                String imageURL = productImageRepository.findPrimaryImageUrlByProductId(productId);

                // DTO 생성 및 추가
                MyPagePremiumImageResponseDto dto = new MyPagePremiumImageResponseDto(
                        winningBid.getBidAmount(),
                        product.getProductName(),
                        imageURL != null ? imageURL : ""
                );

                list.add(dto);

            } catch (Exception e) {
                log.error("상품 정보 조회 중 오류 발생: productId={}, error={}", productId, e.getMessage());
            }
        }

        log.info("프리미엄 상품 정보 조회 완료: 사용자ID={}, 상품수={}, 조회성공수={}",
                userId, premiumProductIds.size(), list.size());

        return list;
    }
}
