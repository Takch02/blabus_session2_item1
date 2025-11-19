package com.highlight.highlight_backend.admin.auction.service;

import com.highlight.highlight_backend.admin.user.domain.Admin;
import com.highlight.highlight_backend.admin.vaildator.AuctionValidator;
import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.admin.product.domian.Product;
import com.highlight.highlight_backend.admin.auction.dto.AuctionResponseDto;
import com.highlight.highlight_backend.admin.auction.dto.AuctionScheduleRequestDto;
import com.highlight.highlight_backend.admin.auction.dto.AuctionStartRequestDto;
import com.highlight.highlight_backend.admin.auction.dto.AuctionUpdateRequestDto;
import com.highlight.highlight_backend.dto.BuyItNowRequestDto;
import com.highlight.highlight_backend.dto.BuyItNowResponseDto;
import com.highlight.highlight_backend.exception.BusinessException;
import com.highlight.highlight_backend.exception.AuctionErrorCode;
import com.highlight.highlight_backend.exception.AdminErrorCode;
import com.highlight.highlight_backend.exception.UserErrorCode;
import com.highlight.highlight_backend.admin.user.repository.AdminRepository;
import com.highlight.highlight_backend.admin.auction.repository.AuctionRepository;
import com.highlight.highlight_backend.repository.BidRepository;
import com.highlight.highlight_backend.admin.product.repository.ProductRepository;
import com.highlight.highlight_backend.domain.Bid;
import com.highlight.highlight_backend.service.AuctionSchedulerService;
import com.highlight.highlight_backend.service.WebSocketService;
import com.highlight.highlight_backend.user.domain.User;
import com.highlight.highlight_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 경매 관리 서비스
 * 
 * 경매 예약, 시작, 종료, 중단 기능을 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAuctionService {
    
    private final AuctionRepository auctionRepository;
    private final ProductRepository productRepository;
    private final AdminRepository adminRepository;

    private final WebSocketService webSocketService;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;
    private final AuctionSchedulerService auctionSchedulerService;

    private final AuctionValidator auctionValidator;
    
    /**
     * 경매 예약
     * 
     * @param request 경매 예약 요청 데이터
     * @param adminId 예약하는 관리자 ID
     * @return 예약된 경매 정보
     */
    @Transactional
    public AuctionResponseDto scheduleAuction(AuctionScheduleRequestDto request, Long adminId) {
        log.info("경매 예약 요청: 상품 {} (관리자: {})", request.getProductId(), adminId);
        
        // 1. 관리자 권한 확인
        auctionValidator.validateAuctionManagePermission(adminId);
        
        // 2. 상품 조회 및 검증
        Product product = productRepository.getOrThrow(request.getProductId());

        // 3. 상품이 이미 경매에 등록되어 있는지 확인
        if (auctionRepository.existsByProductId(request.getProductId())) {
            throw new BusinessException(AuctionErrorCode.PRODUCT_ALREADY_IN_AUCTION);
        }
        
        // 4. 상품 상태 확인 (ACTIVE 상태만 경매 가능)
        if (product.getStatus() != Product.ProductStatus.ACTIVE) {
            throw new BusinessException(AuctionErrorCode.INVALID_PRODUCT_STATUS_FOR_AUCTION);
        }
        
        // 5. UTC 시간을 한국 시간으로 변환
        LocalDateTime kstStartTime = auctionValidator.convertUTCToKST(request.getScheduledStartTime());
        LocalDateTime kstEndTime = auctionValidator.convertUTCToKST(request.getScheduledEndTime());
        
        // 6. 경매 시간 검증
        auctionValidator.validateAuctionTime(kstStartTime, kstEndTime);
        
        // 7. 즉시구매가 설정 시 재고 1개 검증
        auctionValidator.validateBuyItNowProductCount(product, request.getBuyItNowPrice());
        
        // 8. 경매 엔티티 생성
        Auction auction = new Auction();
        auction.addDetail(product, adminId, kstStartTime, kstEndTime, request);
        Auction savedAuction = auctionRepository.save(auction);

        // 8. 상품 상태를 경매대기로 변경
        product.setStatus(Product.ProductStatus.AUCTION_READY);

        // 9. 경매 시작 스케줄링 설정
        auctionSchedulerService.scheduleAuctionStart(savedAuction);

        // 10. 관리자 경매 보류 건수 증가
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(AdminErrorCode.ADMIN_NOT_FOUND));
        adminRepository.save(admin);

        log.info("경매 예약 완료: {} (ID: {}), 스케줄링 설정됨", product.getProductName(), savedAuction.getId());
        
        return AuctionResponseDto.from(savedAuction);
    }
    
    /**
     * 경매 시작
     * 
     * @param auctionId 시작할 경매 ID
     * @param request 경매 시작 요청 데이터
     * @param adminId 시작하는 관리자 ID
     * @return 시작된 경매 정보
     */
    @Transactional
    public AuctionResponseDto startAuction(Long auctionId, AuctionStartRequestDto request, Long adminId) {
        log.info("경매 시작 요청: {} (관리자: {}, 즉시시작: {})", 
                auctionId, adminId, request.isImmediateStart());
        
        // 1. 관리자 권한 확인
        auctionValidator.validateAuctionManagePermission(adminId);
        
        // 2. 경매 조회
        Auction auction = auctionRepository.getOrThrow(auctionId);

        // 3. 경매 시작 가능 여부 확인
        if (!auction.canStart()) {
            throw new BusinessException(AuctionErrorCode.CANNOT_START_AUCTION);
        }

        // 스케줄된 시작 작업이 있다면 취소
        auctionSchedulerService.cancelScheduledStart(auctionId);
        
        // 4. 즉시 시작 vs 시간 입력 처리
        if (request.isImmediateStart()) {
            // 즉시 시작: 현재 시간으로 시작, 기존 종료 시간 유지 또는 1시간 후로 설정
            auction.startAuction(adminId);
            if (auction.getScheduledEndTime().isBefore(LocalDateTime.now())) {
                auction.setScheduledEndTime(LocalDateTime.now().plusHours(1));
            }
        } else {
            // 시간 입력: UTC 시간을 한국 시간으로 변환하여 설정
            LocalDateTime kstStartTime = auctionValidator.convertUTCToKST(request.getScheduledStartTime());
            LocalDateTime kstEndTime = auctionValidator.convertUTCToKST(request.getScheduledEndTime());
            auctionValidator.validateAuctionTime(kstStartTime, kstEndTime);
            auction.setScheduledStartTime(kstStartTime);
            auction.setScheduledEndTime(kstEndTime);
            auction.startAuction(adminId);
        }
        
        // 5. 상품 상태를 경매중으로 변경
        auction.getProduct().setStatus(Product.ProductStatus.IN_AUCTION);
        
        // 6. WebSocket으로 경매 시작 알림 전송
        webSocketService.sendAuctionStartedNotification(auction);

        // 7. 관리자 경매 상태 카운트 업데이트 (pending -> inProgress)
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(AdminErrorCode.ADMIN_NOT_FOUND));
        adminRepository.save(admin);
        
        log.info("경매 시작 완료: {} (ID: {})", 
                auction.getProduct().getProductName(), auction.getId());
        
        return AuctionResponseDto.from(auction);
    }


    /**
     * 경매 강제 중단 (낙찰자 없음, 상품 재판매 가능 상태로 복구)
     */
    @Transactional
    public AuctionResponseDto cancelAuction(Long auctionId, Long adminId) {
        // 1. 검증
        auctionValidator.validateAuctionManagePermission(adminId);
        Auction auction = auctionRepository.getOrThrow(auctionId);

        if (!auction.canEnd()) { // 혹은 canCancel() 별도 구현 추천
            throw new BusinessException(AuctionErrorCode.CANNOT_END_AUCTION);
        }

        // 2. 스케줄 취소
        auctionSchedulerService.cancelScheduledStart(auctionId);

        // 3. 상태 변경 (중단)
        auction.cancelAuction(adminId, "관리자 강제 중단");

        // [중요] 상품을 다시 판매 가능한 상태(ACTIVE)로 되돌림
        auction.getProduct().setStatus(Product.ProductStatus.ACTIVE);

        // 4. 알림 전송 (중단 알림만 전송)
        webSocketService.sendAuctionCancelledNotification(auction);

        log.info("경매 중단 완료: {} (ID: {})", auction.getProduct().getProductName(), auctionId);
        return AuctionResponseDto.from(auction);
    }

    /**
     * 경매 정상 종료
     */
    @Transactional
    public AuctionResponseDto endAuction(Long auctionId, Long adminId,  String endReason) {
        log.info("경매 종료/중단 요청: {} (관리자: {}",
                auctionId, adminId);

        // 1. 관리자 권한 확인
        auctionValidator.validateAuctionManagePermission(adminId);

        // 2. 경매 조회
        Auction auction = auctionRepository.getOrThrow(auctionId);

        // 3. 경매 종료 가능 여부 확인
        if (!auction.canEnd()) {
            throw new BusinessException(AuctionErrorCode.CANNOT_END_AUCTION);
        }

        // 스케줄된 시작 작업이 있다면 취소
        auctionSchedulerService.cancelScheduledStart(auctionId);


        // 4. 낙찰자 조회 (정상 종료인 경우)
        Bid winnerBid = bidRepository.findCurrentHighestBidByAuction(auction).orElse(null);
        if (winnerBid != null) {
            winnerBid.setAsWon(); // 낙찰 상태로 변경
            bidRepository.save(winnerBid);
        }

        // 5. 정상 종료 처리
        auction.endAuction(adminId, endReason);
        auction.getProduct().setStatus(Product.ProductStatus.AUCTION_COMPLETED);

        // 6. WebSocket으로 경매 종료 알림 전송
        webSocketService.sendAuctionEndedNotification(auction, winnerBid);

        // 7. 낙찰자에게 결제 필요 알림 전송
        if (winnerBid != null) {
            webSocketService.sendPaymentRequiredNotification(auctionId, winnerBid.getBidAmount());
        }

        return AuctionResponseDto.from(auction);
    }

    /**
     * 경매 즉시 정상 종료 (현재 최고 입찰자를 낙찰자로 선정)
     */
    @Transactional
    public AuctionResponseDto endAuctionImmediately(Long auctionId, Long adminId) {
        return endAuction(auctionId, adminId, "관리자 즉시 종료");
    }

    
    /**
     * 즉시구매 처리 -> 뭔가 이상해 포인트를 사용하지 않고있어. 나중에 반드시 다시 수정할 것
     *
     * @param request 즉시구매 요청 데이터
     * @param userId 구매자 ID
     * @return 즉시구매 완료 정보
     */
    @Transactional
    public BuyItNowResponseDto buyItNow(BuyItNowRequestDto request, Long userId) {
        Long auctionId = request.getAuctionId();
        log.info("즉시구매 요청: 경매 {} (사용자: {})", auctionId, userId);
        // 1. 경매 조회
        Auction auction = auctionRepository.getOrThrow(auctionId);

        // 2. 즉시구매 가능 여부 검증
        validateBuyItNowEligibility(auction, userId);

        // 스케줄된 시작 작업이 있다면 취소
        auctionSchedulerService.cancelScheduledStart(auctionId);
        
        // 3. 즉시구매 처리
        Bid buyItNowBid = createBuyItNowBid(auction, userId);
        
        // 4. 경매 즉시 종료
        auction.endAuction(null, "즉시구매로 인한 경매 종료");
        auction.getProduct().setStatus(Product.ProductStatus.AUCTION_COMPLETED);
        
        // 5. 낙찰 처리
        buyItNowBid.setAsWon();
        // 6. WebSocket 알림 전송
        webSocketService.sendAuctionEndedNotification(auction, buyItNowBid);
        
        log.info("즉시구매 완료: 경매 {} (사용자: {}, 가격: {})", 
                auctionId, userId, auction.getBuyItNowPrice());
        
        return BuyItNowResponseDto.from(auction, userId);
    }


    /**
     * 경매 수정
     *
     * @param auctionId 수정할 경매 ID
     * @param request 경매 수정 요청 데이터
     * @param adminId 수정하는 관리자 ID
     * @return 수정된 경매 정보
     */
    @Transactional
    public AuctionResponseDto updateAuction(Long auctionId, AuctionUpdateRequestDto request, Long adminId) {
        log.info("경매 수정 요청: 경매 {} (관리자: {})", auctionId, adminId);

        // 1. 관리자 권한 확인
        auctionValidator.validateAuctionManagePermission(adminId);

        // 2. 경매 조회 및 검증
        Auction auction = auctionRepository.getOrThrow(auctionId);

        // 3. 경매 상태 검증 (진행 중인 경매는 수정 불가)
        if (auction.getStatus() == Auction.AuctionStatus.IN_PROGRESS) {
            throw new BusinessException(AuctionErrorCode.CANNOT_MODIFY_IN_PROGRESS_AUCTION);
        }

        // 4. 경매가 완료되었거나 취소된 경우 수정 불가
        if (auction.getStatus() == Auction.AuctionStatus.COMPLETED ||
            auction.getStatus() == Auction.AuctionStatus.CANCELLED ||
            auction.getStatus() == Auction.AuctionStatus.FAILED) {
            throw new BusinessException(AuctionErrorCode.CANNOT_MODIFY_ENDED_AUCTION);
        }

        // 6. 시작/종료 시간이 모두 설정된 경우 시간 검증
        if (auction.getScheduledStartTime() != null && auction.getScheduledEndTime() != null) {
            auctionValidator.validateAuctionTime(auction.getScheduledStartTime(), auction.getScheduledEndTime());
        }

        // 7. 상품 변경
        if (request.getProductId() != null && !request.getProductId().equals(auction.getProduct().getId())) {
            // 새로운 상품 조회 및 검증
            Product newProduct = productRepository.getOrThrow(request.getProductId());

            // 새 상품이 이미 경매에 등록되어 있는지 확인
            if (auctionRepository.existsByProductId(request.getProductId())) {
                throw new BusinessException(AuctionErrorCode.PRODUCT_ALREADY_IN_AUCTION);
            }

            // 새 상품 상태 확인 (ACTIVE 상태만 경매 가능)
            if (newProduct.getStatus() != Product.ProductStatus.ACTIVE) {
                throw new BusinessException(AuctionErrorCode.INVALID_PRODUCT_STATUS_FOR_AUCTION);
            }

            auction.setProduct(newProduct);
        }

        LocalDateTime kstEndTime = auctionValidator.convertUTCToKST(request.getScheduledEndTime());
        LocalDateTime kstStartTime = auctionValidator.convertUTCToKST(request.getScheduledStartTime());

        if (request.getBuyItNowPrice() != null) {
            // 즉시구매가 설정 시 재고 1개 검증
            auctionValidator.validateBuyItNowProductCount(auction.getProduct(), request.getBuyItNowPrice());
            auction.setBuyItNowPrice(request.getBuyItNowPrice());
        }
        // updateDetail 을 통해 한번에 update
        auction.updateDetail(request, kstStartTime, kstEndTime);

        log.info("경매 수정 완료: 경매 {} (관리자: {})", auctionId, adminId);

        return AuctionResponseDto.from(auction);
    }

    /**
     * 즉시구매 가능 여부 검증
     */
    private void validateBuyItNowEligibility(Auction auction, Long userId) {
        // 경매가 진행중인지 확인
        if (!auction.isInProgress()) {
            throw new BusinessException(AuctionErrorCode.AUCTION_NOT_IN_PROGRESS);
        }

        // 즉시구매가가 설정되어 있는지 확인
        if (auction.getBuyItNowPrice() == null || auction.getBuyItNowPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BusinessException(AuctionErrorCode.BUY_IT_NOW_NOT_AVAILABLE);
        }

        // 재고가 1개인지 확인
        if (auction.getProduct().getProductCount() != 1) {
            throw new BusinessException(AuctionErrorCode.BUY_IT_NOW_ONLY_FOR_SINGLE_ITEM);
        }

        // 사용자 존재 여부 확인
        auctionValidator.validateUserExists(userId);
    }

    /**
     * 즉시구매 입찰 생성
     */
    private Bid createBuyItNowBid(Auction auction, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        Bid buyItNowBid = new Bid();
        buyItNowBid.setAuction(auction);
        buyItNowBid.setUser(user);
        buyItNowBid.setBidAmount(auction.getBuyItNowPrice());
        buyItNowBid.setCreatedAt(LocalDateTime.now());
        buyItNowBid.setIsBuyItNow(true);

        return bidRepository.save(buyItNowBid);
    }
}
