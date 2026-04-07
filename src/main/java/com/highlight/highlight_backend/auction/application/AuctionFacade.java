package com.highlight.highlight_backend.auction.application;

import com.highlight.highlight_backend.admin.service.AdminAuthService;
import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.auction.dto.*;
import com.highlight.highlight_backend.auction.service.*;
import com.highlight.highlight_backend.auction.validator.AuctionValidator;
import com.highlight.highlight_backend.bid.domain.Bid;
import com.highlight.highlight_backend.bid.service.BidNotificationService;
import com.highlight.highlight_backend.bid.service.BidService;
import com.highlight.highlight_backend.product.domian.Product;
import com.highlight.highlight_backend.product.service.AdminProductService;
import com.highlight.highlight_backend.user.domain.User;
import com.highlight.highlight_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionFacade {

    private final AdminAuctionService adminAuctionService;
    private final UserService userService;
    private final AdminProductService adminProductService;
    private final BidService bidService;
    private final BidNotificationService bidNotificationService;
    private final AuctionNotificationService auctionNotificationService;
    private final AdminAuthService adminAuthService;
    private final AuctionValidator auctionValidator;


    /**
     * 경매 예약 UseCase
     */
    @Transactional
    public AuctionResponseDto scheduleAuction(AuctionScheduleRequestDto request, Long adminId) {
        log.info("경매 예약 요청 (Facade): 상품 {} (관리자: {})", request.getProductId(), adminId);

        adminAuthService.validateManagePermission(adminId);
        Product product = adminProductService.getProductOrThrow(request.getProductId());
        Auction savedAuction = adminAuctionService.createAuction(request, adminId, product);
        adminProductService.updateProductStatus(product.getId(), Product.ProductStatus.AUCTION_READY);

        return AuctionResponseDto.from(savedAuction);
    }

    /**
     * 경매 시작 UseCase
     */
    @Transactional
    public AuctionResponseDto startAuction(Long auctionId, AuctionStartRequestDto request, Long adminId) {
        log.info("경매 시작 요청 (Facade): {} (관리자: {})", auctionId, adminId);

        adminAuthService.validateManagePermission(adminId);
        Auction auction = adminAuctionService.startAuctionInternal(auctionId, request, adminId);
        adminProductService.updateProductStatus(auction.getProduct().getId(), Product.ProductStatus.IN_AUCTION);
        auctionNotificationService.sendAuctionStartedNotification(auction);

        return AuctionResponseDto.from(auction);
    }

    /**
     * 경매 취소 UseCase
     */
    @Transactional
    public AuctionResponseDto cancelAuction(Long auctionId, Long adminId) {
        log.info("경매 취소 요청 (Facade): {} (관리자: {})", auctionId, adminId);

        adminAuthService.validateManagePermission(adminId);
        Auction auction = adminAuctionService.cancelAuctionInternal(auctionId, adminId);
        adminProductService.updateProductStatus(auction.getProduct().getId(), Product.ProductStatus.ACTIVE);
        auctionNotificationService.sendAuctionCancelledNotification(auction);

        return AuctionResponseDto.from(auction);
    }

    /**
     * 경매 종료 UseCase
     */
    @Transactional
    public AuctionResponseDto endAuction(Long auctionId, Long adminId, String endReason) {
        log.info("경매 종료 요청 (Facade): {} (관리자: {})", auctionId, adminId);

        adminAuthService.validateManagePermission(adminId);
        Auction auction = adminAuctionService.endAuctionInternal(auctionId, adminId, endReason);

        // 3. 낙찰자 조회 및 처리 (엔티티 직접 조작 대신 Service 메서드 호출)
        Bid winnerBid = bidService.findCurrentHighestBid(auction).orElse(null);
        String winnerNickname = "없음";

        if (winnerBid != null) {
            bidService.setBidAsWon(winnerBid); // 명확한 행위 지시 (서비스에서 처리)
            winnerNickname = winnerBid.getUser().getNickname();
            bidNotificationService.notifyWin(winnerBid);
        }

        adminProductService.updateProductStatus(auction.getProduct().getId(), Product.ProductStatus.AUCTION_COMPLETED);
        auctionNotificationService.notifyAuctionEnded(auction, winnerNickname);

        return AuctionResponseDto.from(auction);
    }

    /**
     * 즉시구매 UseCase
     */
    @Transactional
    public BuyItNowResponseDto buyItNow(BuyItNowRequestDto request, Long userId) {
        Long auctionId = request.getAuctionId();
        log.info("즉시구매 요청 (Facade): 경매 {} (사용자: {})", auctionId, userId);

        User user = userService.getUserOrThrow(userId);
        Auction auction = adminAuctionService.getAuctionOrThrow(auctionId);
        auctionValidator.validateBuyItNowEligibility(auction);

        // 즉시구매 입찰 생성 및 낙찰 처리
        Bid buyItNowBid = bidService.createBuyItNowBid(auction, user);
        bidService.setBidAsWon(buyItNowBid);

        adminAuctionService.endAuctionInternal(auctionId, null, "즉시구매로 인한 경매 종료");
        adminProductService.updateProductStatus(auction.getProduct().getId(), Product.ProductStatus.AUCTION_COMPLETED);
        auctionNotificationService.notifyAuctionEnded(auction, user.getNickname());

        return BuyItNowResponseDto.from(auction, userId);
    }

    /**
     * 경매 수정 UseCase
     */
    @Transactional
    public AuctionResponseDto updateAuction(Long auctionId, AuctionUpdateRequestDto request, Long adminId) {
        log.info("경매 수정 요청 (Facade): 경매 {} (관리자: {})", auctionId, adminId);

        adminAuthService.validateManagePermission(adminId);
        Product newProduct = null;
        if (request.getProductId() != null) {
            newProduct = adminProductService.getProductOrThrow(request.getProductId());
        }

        Auction updatedAuction = adminAuctionService.updateAuctionInternal(auctionId, request, newProduct, adminId);

        return AuctionResponseDto.from(updatedAuction);
    }
}
