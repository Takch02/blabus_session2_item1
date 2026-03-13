package com.highlight.highlight_backend.auction.service;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.auction.repository.AuctionRepository;
import com.highlight.highlight_backend.product.domian.Product;
import com.highlight.highlight_backend.auction.dto.AuctionScheduleRequestDto;
import com.highlight.highlight_backend.auction.dto.AuctionStartRequestDto;
import com.highlight.highlight_backend.auction.dto.AuctionUpdateRequestDto;
import com.highlight.highlight_backend.auction.validator.AuctionValidator;
import com.highlight.highlight_backend.common.util.TimeUtils;
import com.highlight.highlight_backend.exception.BusinessException;
import com.highlight.highlight_backend.exception.AuctionErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 관리자 경매 관리 서비스 (내부 로직 위주)
 *
 * Facade 에서 호출하여 도메인 간 결합도를 낮춤
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAuctionService {

    private final AuctionRepository auctionRepository;
    private final AuctionSchedulerService auctionSchedulerService;
    private final AuctionValidator auctionValidator;

    /**
     * 경매 엔티티 생성 및 저장 (내부용)
     */
    @Transactional
    public Auction createAuction(AuctionScheduleRequestDto request, Long adminId, Product product) {
        // 1. 상품이 이미 경매에 등록되어 있는지 확인
        if (auctionRepository.existsByProductId(product.getId())) {
            throw new BusinessException(AuctionErrorCode.PRODUCT_ALREADY_IN_AUCTION);
        }

        // 2. UTC 시간을 한국 시간으로 변환
        LocalDateTime kstStartTime = TimeUtils.convertUTCToKST(request.getScheduledStartTime());
        LocalDateTime kstEndTime = TimeUtils.convertUTCToKST(request.getScheduledEndTime());

        // 3. 상품 상태, 경매 시간, 재고 1개 검증
        auctionValidator.validateAuctionCreation(product, kstStartTime, kstEndTime, request.getBuyItNowPrice());

        // 4. 경매 엔티티 생성
        Auction auction = new Auction();
        auction.addDetail(product, adminId, kstStartTime, kstEndTime, request);
        
        log.info("경매 생성 성공 : {}", product.getProductName());
        return auctionRepository.save(auction);
    }

    /**
     * 경매 시작 처리 (내부용)
     */
    @Transactional
    public Auction startAuctionInternal(Long auctionId, AuctionStartRequestDto request, Long adminId) {
        Auction auction = auctionRepository.getOrThrow(auctionId);
        auctionValidator.validateAuctionStart(auction);

        auctionSchedulerService.cancelScheduledStart(auctionId);

        if (request.isImmediateStart()) {
            auction.startAuction(adminId);
            if (auction.getScheduledEndTime().isBefore(LocalDateTime.now())) {
                auction.setScheduledEndTime(LocalDateTime.now().plusHours(1));
            }
        } else {
            LocalDateTime kstStartTime = TimeUtils.convertUTCToKST(request.getScheduledStartTime());
            LocalDateTime kstEndTime = TimeUtils.convertUTCToKST(request.getScheduledEndTime());
            auctionValidator.validateAuctionTime(kstStartTime, kstEndTime);
            auction.setScheduledStartTime(kstStartTime);
            auction.setScheduledEndTime(kstEndTime);
            auction.startAuction(adminId);
        }

        return auction;
    }

    /**
     * 경매 취소 처리 (내부용)
     */
    @Transactional
    public Auction cancelAuctionInternal(Long auctionId, Long adminId) {
        Auction auction = auctionRepository.getOrThrow(auctionId);
        auctionValidator.validateAuctionEnd(auction);
        auctionSchedulerService.cancelScheduledStart(auctionId);
        auction.cancelAuction(adminId, "관리자 강제 중단");
        return auction;
    }

    /**
     * 경매 종료 처리 (내부용)
     */
    @Transactional
    public Auction endAuctionInternal(Long auctionId, Long adminId, String endReason) {
        Auction auction = auctionRepository.getOrThrow(auctionId);
        auctionValidator.validateAuctionEnd(auction);
        auctionSchedulerService.cancelScheduledStart(auctionId);
        auction.endAuction(adminId, endReason);
        return auction;
    }

    /**
     * 경매 수정 처리 (내부용)
     */
    @Transactional
    public Auction updateAuctionInternal(Long auctionId, AuctionUpdateRequestDto request, Product newProduct, Long adminId) {
        Auction auction = auctionRepository.getOrThrow(auctionId);
        auctionValidator.validateAuctionProgress(auction);
        auctionValidator.validateAuctionCompleteOrFailed(auction);

        if (newProduct != null && !newProduct.getId().equals(auction.getProduct().getId())) {
            if (auctionRepository.existsByProductId(newProduct.getId())) {
                throw new BusinessException(AuctionErrorCode.PRODUCT_ALREADY_IN_AUCTION);
            }
            auctionValidator.validateProductStatus(newProduct);
            auction.setProduct(newProduct);
        }

        LocalDateTime kstStartTime = TimeUtils.convertUTCToKST(request.getScheduledStartTime());
        LocalDateTime kstEndTime = TimeUtils.convertUTCToKST(request.getScheduledEndTime());

        if (request.getBuyItNowPrice() != null) {
            auctionValidator.validateSingleItemForBuyItNow(auction.getProduct());
            auction.setBuyItNowPrice(request.getBuyItNowPrice());
        }

        auction.updateDetail(request, kstStartTime, kstEndTime);
        return auction;
    }

    public Auction getAuctionOrThrow(Long auctionId) {
        return auctionRepository.getOrThrow(auctionId);
    }
}
