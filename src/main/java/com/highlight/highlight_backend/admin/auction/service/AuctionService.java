package com.highlight.highlight_backend.admin.auction.service;

import com.highlight.highlight_backend.admin.user.domain.Admin;
import com.highlight.highlight_backend.admin.auction.domain.Auction;
import com.highlight.highlight_backend.domain.Product;
import com.highlight.highlight_backend.admin.auction.dto.AuctionEndRequestDto;
import com.highlight.highlight_backend.admin.auction.dto.AuctionResponseDto;
import com.highlight.highlight_backend.admin.auction.dto.AuctionScheduleRequestDto;
import com.highlight.highlight_backend.admin.auction.dto.AuctionStartRequestDto;
import com.highlight.highlight_backend.admin.auction.dto.AuctionUpdateRequestDto;
import com.highlight.highlight_backend.dto.BuyItNowRequestDto;
import com.highlight.highlight_backend.dto.BuyItNowResponseDto;
import com.highlight.highlight_backend.exception.BusinessException;
import com.highlight.highlight_backend.exception.AuctionErrorCode;
import com.highlight.highlight_backend.exception.AdminErrorCode;
import com.highlight.highlight_backend.exception.ProductErrorCode;
import com.highlight.highlight_backend.exception.UserErrorCode;
import com.highlight.highlight_backend.admin.user.repository.AdminRepository;
import com.highlight.highlight_backend.admin.auction.repository.AuctionRepository;
import com.highlight.highlight_backend.repository.BidRepository;
import com.highlight.highlight_backend.repository.ProductRepository;
import com.highlight.highlight_backend.domain.Bid;
import com.highlight.highlight_backend.service.AuctionSchedulerService;
import com.highlight.highlight_backend.service.WebSocketService;
import com.highlight.highlight_backend.user.domain.User;
import com.highlight.highlight_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 경매 관리 서비스
 * 
 * 경매 예약, 시작, 종료, 중단 기능을 제공합니다.
 * 
 * @author 전우선
 * @since 2025.08.13
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionService {
    
    private final AuctionRepository auctionRepository;
    private final ProductRepository productRepository;
    private final AdminRepository adminRepository;
    private final WebSocketService webSocketService;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;
    private final AuctionSchedulerService auctionSchedulerService;
    
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
        validateAuctionManagePermission(adminId);
        
        // 2. 상품 조회 및 검증
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
        
        // 3. 상품이 이미 경매에 등록되어 있는지 확인
        if (auctionRepository.existsByProductId(request.getProductId())) {
            throw new BusinessException(AuctionErrorCode.PRODUCT_ALREADY_IN_AUCTION);
        }
        
        // 4. 상품 상태 확인 (ACTIVE 상태만 경매 가능)
        if (product.getStatus() != Product.ProductStatus.ACTIVE) {
            throw new BusinessException(AuctionErrorCode.INVALID_PRODUCT_STATUS_FOR_AUCTION);
        }
        
        // 5. UTC 시간을 한국 시간으로 변환
        LocalDateTime kstStartTime = convertUTCToKST(request.getScheduledStartTime());
        LocalDateTime kstEndTime = convertUTCToKST(request.getScheduledEndTime());
        
        // 6. 경매 시간 검증
        validateAuctionTime(kstStartTime, kstEndTime);
        
        // 7. 즉시구매가 설정 시 재고 1개 검증
        validateBuyItNowProductCount(product, request.getBuyItNowPrice());
        
        // 8. 경매 엔티티 생성
        Auction auction = new Auction();

        auction.setProduct(product);
        auction.setStatus(Auction.AuctionStatus.SCHEDULED); // 초기 상태는 '예약됨'
        auction.setScheduledStartTime(kstStartTime);
        auction.setScheduledEndTime(kstEndTime);
        auction.setDescription(request.getDescription());
        auction.setBuyItNowPrice(request.getBuyItNowPrice());
        auction.setCreatedBy(adminId);
        auction.setBidUnit(request.getBidUnit());
        auction.setStartPrice(request.getStartPrice());         // 경매 시작가 설정
        auction.setCurrentHighestBid(request.getStartPrice());
        auction.setMinimumBid(request.getMinimumBid());       // 최소 인상폭 설정
        auction.setMaxBid(request.getMaxBid());               // 최대 인상폭 설정
        auction.setShippingFee(request.getShippingFee());       // 배송비 설정
        auction.setIsPickupAvailable(request.getIsPickupAvailable()); // 직접 픽업 가능 여부
        
        // 8. 상품 상태를 경매대기로 변경
        product.setStatus(Product.ProductStatus.AUCTION_READY);
        productRepository.save(product);
        
        Auction savedAuction = auctionRepository.save(auction);

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
        validateAuctionManagePermission(adminId);
        
        // 2. 경매 조회
        Auction auction = auctionRepository.findByIdWithProduct(auctionId)
            .orElseThrow(() -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));
        
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
            LocalDateTime kstStartTime = convertUTCToKST(request.getScheduledStartTime());
            LocalDateTime kstEndTime = convertUTCToKST(request.getScheduledEndTime());
            validateAuctionTime(kstStartTime, kstEndTime);
            auction.setScheduledStartTime(kstStartTime);
            auction.setScheduledEndTime(kstEndTime);
            auction.startAuction(adminId);
        }
        
        // 5. 상품 상태를 경매중으로 변경
        auction.getProduct().setStatus(Product.ProductStatus.IN_AUCTION);
        
        Auction updatedAuction = auctionRepository.save(auction);
        
        // 6. WebSocket으로 경매 시작 알림 전송
        webSocketService.sendAuctionStartedNotification(updatedAuction);

        // 7. 관리자 경매 상태 카운트 업데이트 (pending -> inProgress)
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(AdminErrorCode.ADMIN_NOT_FOUND));
        adminRepository.save(admin);
        
        log.info("경매 시작 완료: {} (ID: {})", 
                auction.getProduct().getProductName(), updatedAuction.getId());
        
        return AuctionResponseDto.from(updatedAuction);
    }
    
    /**
     * 경매 종료/중단
     * 
     * @param auctionId 종료할 경매 ID
     * @param request 경매 종료 요청 데이터
     * @param adminId 종료하는 관리자 ID
     * @return 종료된 경매 정보
     */
    @Transactional
    public AuctionResponseDto endAuction(Long auctionId, AuctionEndRequestDto request, Long adminId) {
        log.info("경매 종료/중단 요청: {} (관리자: {}, 중단: {})", 
                auctionId, adminId, request.isCancel());
        
        // 1. 관리자 권한 확인
        validateAuctionManagePermission(adminId);
        
        // 2. 경매 조회
        Auction auction = auctionRepository.findByIdWithProduct(auctionId)
            .orElseThrow(() -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));
        
        // 3. 경매 종료 가능 여부 확인
        if (!auction.canEnd()) {
            throw new BusinessException(AuctionErrorCode.CANNOT_END_AUCTION);
        }

        // 스케줄된 시작 작업이 있다면 취소
        auctionSchedulerService.cancelScheduledStart(auctionId);
        
        // 4. 낙찰자 조회 (정상 종료인 경우)
        Bid winnerBid = null;
        if (!request.isCancel()) {
            winnerBid = bidRepository.findCurrentHighestBidByAuction(auction).orElse(null);
            if (winnerBid != null) {
                winnerBid.setAsWon(); // 낙찰 상태로 변경
                bidRepository.save(winnerBid);
            }
        }
        
        // 5. 종료 처리
        Auction updatedAuction;
        if (request.isCancel()) {
            // 경매 중단
            auction.cancelAuction(adminId, request.getEndReason());
            auction.getProduct().setStatus(Product.ProductStatus.ACTIVE); // 상품 상태를 다시 활성으로
            
            updatedAuction = auctionRepository.save(auction);
            
            // 경매 취소 알림 전송
            webSocketService.sendAuctionCancelledNotification(updatedAuction);
        } else {
            // 경매 정상 종료
            auction.endAuction(adminId, request.getEndReason());
            auction.getProduct().setStatus(Product.ProductStatus.AUCTION_COMPLETED);
            
            updatedAuction = auctionRepository.save(auction);
            
            // 6. WebSocket으로 경매 종료 알림 전송
            webSocketService.sendAuctionEndedNotification(updatedAuction, winnerBid);
            
            // 7. 낙찰자에게 결제 필요 알림 전송
            if (winnerBid != null) {
                webSocketService.sendPaymentRequiredNotification(auctionId, winnerBid.getBidAmount());
            }
        }

        // 관리자 경매 상태 카운트 업데이트 (inProgress -> completed)
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(AdminErrorCode.ADMIN_NOT_FOUND));
        adminRepository.save(admin);
        
        log.info("경매 {}완료: {} (ID: {})", 
                request.isCancel() ? "중단 " : "", 
                auction.getProduct().getProductName(), updatedAuction.getId());
        
        return AuctionResponseDto.from(updatedAuction);
    }
    
    /**
     * 경매 목록 조회
     * 
     * @param pageable 페이징 정보
     * @param adminId 조회하는 관리자 ID
     * @return 경매 목록
     */
    public Page<AuctionResponseDto> getAdminAuctionList(Pageable pageable, Long adminId) {
        log.info("경매 목록 조회 요청 (관리자: {})", adminId);
        
        validateAuctionManagePermission(adminId);
        
        return auctionRepository.findByCreatedByOrderByCreatedAtDesc(adminId, pageable)
            .map(AuctionResponseDto::from);
    }
    
    /**
     * 경매 상세 조회
     * 
     * @param auctionId 조회할 경매 ID
     * @param adminId 조회하는 관리자 ID
     * @return 경매 상세 정보
     */
    public AuctionResponseDto getAuction(Long auctionId, Long adminId) {
        log.info("경매 상세 조회 요청: {} (관리자: {})", auctionId, adminId);
        
        validateAuctionManagePermission(adminId);
        
        Auction auction = auctionRepository.findByIdWithProduct(auctionId)
            .orElseThrow(() -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));
        
        return AuctionResponseDto.from(auction);
    }
    
    /**
     * 관리자의 전체 경매 목록 조회
     * 
     * @param pageable 페이징 정보
     * @param adminId 조회하는 관리자 ID
     * @return 진행 중인 경매 목록
     */
    public Page<AuctionResponseDto> getActiveAuctions(Pageable pageable, Long adminId) {
        log.info("관리자 전체 경매 목록 조회 요청 (관리자: {})", adminId);
        
        validateAuctionManagePermission(adminId);
        
        return auctionRepository.findByCreatedByOrderByCreatedAtDesc(adminId, pageable)
            .map(AuctionResponseDto::from);
    }
    
    /**
     * 경매 관리 권한 검증
     * 
     * @param adminId 검증할 관리자 ID
     * @return 검증된 관리자 정보
     */
    private Admin validateAuctionManagePermission(Long adminId) {
        Admin admin = adminRepository.findById(adminId)
            .orElseThrow(() -> new BusinessException(AdminErrorCode.ADMIN_NOT_FOUND));
        
        // 기획 변경: 모든 관리자가 경매 관리 가능, SUPER_ADMIN 체크만 유지
        if (admin.getRole() != Admin.AdminRole.SUPER_ADMIN && admin.getRole() != Admin.AdminRole.ADMIN) {
            throw new BusinessException(AdminErrorCode.INSUFFICIENT_PERMISSION);
        }
        
        return admin;
    }
    
    /**
     * UTC 시간을 한국 시간(KST)으로 변환
     * 
     * @param utcTime UTC 시간 (예: 2025-08-22T06:30:00.000Z)
     * @return 한국 시간
     */
    private LocalDateTime convertUTCToKST(LocalDateTime utcTime) {
        if (utcTime == null) {
            return null;
        }
        
        // UTC로 가정하고 ZonedDateTime으로 변환
        ZonedDateTime utcZoned = utcTime.atZone(ZoneId.of("UTC"));
        
        // 한국 시간대로 변환 (UTC+9)
        ZonedDateTime kstZoned = utcZoned.withZoneSameInstant(ZoneId.of("Asia/Seoul"));
        
        return kstZoned.toLocalDateTime();
    }
    
    /**
     * 경매 시간 검증
     * 
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     */
    private void validateAuctionTime(LocalDateTime startTime, LocalDateTime endTime) {
        // 한국 시간 기준으로 현재 시간 가져오기
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        
        // 시작 시간이 현재 시간보다 이전일 수 없음 (5분 여유)
        if (startTime.isBefore(now.minus(5, ChronoUnit.MINUTES))) {
            throw new BusinessException(AuctionErrorCode.INVALID_AUCTION_START_TIME);
        }
        
        // 종료 시간이 시작 시간보다 이전일 수 없음
        if (endTime.isBefore(startTime)) {
            throw new BusinessException(AuctionErrorCode.INVALID_AUCTION_END_TIME);
        }
        
        // 경매 시간이 너무 짧으면 안됨 (최소 10분)
        if (ChronoUnit.MINUTES.between(startTime, endTime) < 10) {
            throw new BusinessException(AuctionErrorCode.AUCTION_DURATION_TOO_SHORT);
        }
    }
    
    /**
     * 즉시구매 처리
     * 
     * @param auctionId 즉시구매할 경매 ID
     * @param request 즉시구매 요청 데이터
     * @param userId 구매자 ID
     * @return 즉시구매 완료 정보
     */
    @Transactional
    public BuyItNowResponseDto buyItNow(Long auctionId, BuyItNowRequestDto request, Long userId) {
        log.info("즉시구매 요청: 경매 {} (사용자: {})", auctionId, userId);
        
        // 1. 경매 조회
        Auction auction = auctionRepository.findByIdWithProduct(auctionId)
            .orElseThrow(() -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));
        
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
        bidRepository.save(buyItNowBid);
        
        Auction completedAuction = auctionRepository.save(auction);
        
        // 6. WebSocket 알림 전송
        webSocketService.sendAuctionEndedNotification(completedAuction, buyItNowBid);
        
        log.info("즉시구매 완료: 경매 {} (사용자: {}, 가격: {})", 
                auctionId, userId, auction.getBuyItNowPrice());
        
        return BuyItNowResponseDto.from(completedAuction, userId);
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
        validateUserExists(userId);
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
    
    /**
     * 사용자 존재 여부 확인
     */
    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }
    }
    
    /**
     * 즉시구매가 설정 시 상품 개수 검증
     * 
     * @param product 상품 정보
     * @param buyItNowPrice 즉시구매가
     */
    private void validateBuyItNowProductCount(Product product, java.math.BigDecimal buyItNowPrice) {
        // 즉시구매가가 설정된 경우에만 검증
        if (buyItNowPrice != null && buyItNowPrice.compareTo(java.math.BigDecimal.ZERO) > 0) {
            if (product.getProductCount() != 1) {
                throw new BusinessException(AuctionErrorCode.BUY_IT_NOW_ONLY_FOR_SINGLE_ITEM);
            }
        }
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
        validateAuctionManagePermission(adminId);
        
        // 2. 경매 조회 및 검증
        Auction auction = auctionRepository.findByIdWithProduct(auctionId)
            .orElseThrow(() -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));
        
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
        
        // 5. 시간 정보 수정
        if (request.getScheduledStartTime() != null) {
            LocalDateTime kstStartTime = convertUTCToKST(request.getScheduledStartTime());
            auction.setScheduledStartTime(kstStartTime);
        }
        
        if (request.getScheduledEndTime() != null) {
            LocalDateTime kstEndTime = convertUTCToKST(request.getScheduledEndTime());
            auction.setScheduledEndTime(kstEndTime);
        }
        
        // 6. 시작/종료 시간이 모두 설정된 경우 시간 검증
        if (auction.getScheduledStartTime() != null && auction.getScheduledEndTime() != null) {
            validateAuctionTime(auction.getScheduledStartTime(), auction.getScheduledEndTime());
        }
        
        // 7. 상품 변경 (선택사항)
        if (request.getProductId() != null && !request.getProductId().equals(auction.getProduct().getId())) {
            // 새로운 상품 조회 및 검증
            Product newProduct = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
            
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
        
        // 8. 가격 정보 수정
        if (request.getStartPrice() != null) {
            auction.setStartPrice(request.getStartPrice());
        }
        
        if (request.getBidUnit() != null) {
            auction.setBidUnit(request.getBidUnit());
        }
        
        if (request.getMaxBid() != null) {
            auction.setMaxBid(request.getMaxBid());
        }
        
        if (request.getMinimumBid() != null) {
            auction.setMinimumBid(request.getMinimumBid());
        }
        
        if (request.getBuyItNowPrice() != null) {
            // 즉시구매가 설정 시 재고 1개 검증
            validateBuyItNowProductCount(auction.getProduct(), request.getBuyItNowPrice());
            auction.setBuyItNowPrice(request.getBuyItNowPrice());
        }
        
        if (request.getShippingFee() != null) {
            auction.setShippingFee(request.getShippingFee());
        }
        
        if (request.getIsPickupAvailable() != null) {
            auction.setIsPickupAvailable(request.getIsPickupAvailable());
        }
        
        // 9. 설명 수정
        if (request.getDescription() != null) {
            auction.setDescription(request.getDescription());
        }
        
        // 9. 경매 저장
        Auction updatedAuction = auctionRepository.save(auction);
        
        log.info("경매 수정 완료: 경매 {} (관리자: {})", auctionId, adminId);
        
        return AuctionResponseDto.from(updatedAuction);
    }
}
