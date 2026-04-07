package com.highlight.highlight_backend.auction.service;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.auction.repository.AuctionRepository;
import com.highlight.highlight_backend.product.domian.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionStartService {

    private final AuctionRepository auctionRepository;
    private final AuctionCountService auctionCountService;
    private final AuctionNotificationService auctionNotificationService;

    @Transactional
    public void startAuction(Long auctionId) {
        Auction auction = auctionRepository.findByIdWithLock(auctionId).orElse(null);
        if (auction == null) {
            log.warn("경매를 찾을 수 없습니다. ID: {}", auctionId);
            return;
        }
        // 멱등성 보장
        if (auction.getStatus() != Auction.AuctionStatus.SCHEDULED) {
            log.info("이미 처리된 경매입니다. ID: {}", auctionId);
            return;
        }

        // 경매 상태를 IN_PROGRESS로 변경
        auction.setStatus(Auction.AuctionStatus.IN_PROGRESS);
        // 경매 시작 시간을 현재 시간으로 수정
        auction.setActualStartTime(LocalDateTime.now());

        Product.Category category = auction.getProduct().getCategory();

        // Redis 의 SCHEDULED 상태의 count 감소
        auctionCountService.decrement(
                Auction.AuctionStatus.SCHEDULED, category);

        // Redis 의 IN_PROGRESS 상태의 count 증가
        auctionCountService.increment(
                Auction.AuctionStatus.IN_PROGRESS, category);

        // 상품 상태를 IN_AUCTION으로 변경
        auction.getProduct().setStatus(Product.ProductStatus.IN_AUCTION);

        auctionNotificationService.sendAuctionStartedNotification(auction);

        log.info("스케줄된 경매가 시작되었습니다. 경매 ID: {}, 상품 상태 변경: IN_AUCTION", auctionId);

    }
}