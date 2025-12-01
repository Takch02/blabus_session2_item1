package com.highlight.highlight_backend.payment.service;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.auction.repository.AuctionRepository;
import com.highlight.highlight_backend.auction.service.AuctionNotificationService;
import com.highlight.highlight_backend.bid.domain.Bid;
import com.highlight.highlight_backend.user.domain.User;
import com.highlight.highlight_backend.auction.dto.BuyItNowRequestDto;
import com.highlight.highlight_backend.auction.dto.BuyItNowResponseDto;
import com.highlight.highlight_backend.payment.dto.PaymentPreviewDto;
import com.highlight.highlight_backend.payment.dto.PaymentResponseDto;
import com.highlight.highlight_backend.exception.AuctionErrorCode;
import com.highlight.highlight_backend.exception.BusinessException;
import com.highlight.highlight_backend.exception.PaymentErrorCode;
import com.highlight.highlight_backend.exception.UserErrorCode;
import com.highlight.highlight_backend.bid.repository.BidRepository;
import com.highlight.highlight_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 결제 서비스
 *
 * 경매 낙찰 후 결제 처리를 담당합니다.
 * 포인트를 최대한 활용하여 결제 금액을 최소화합니다.
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;
    private final AuctionNotificationService auctionNotificationService;
    
    /**
     * 결제 미리보기 조회
     * 
     * @param auctionId 경매 ID
     * @param userId 사용자 ID
     * @return 결제 미리보기 정보
     */
    @Transactional(readOnly = true)
    public PaymentPreviewDto getPaymentPreview(Long auctionId, Long userId) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        
        // 2. 경매 조회
        Auction auction = auctionRepository.findById(auctionId)
            .orElseThrow(() -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));
        
        // 3. 경매 종료 여부 확인
        if (auction.getStatus() != Auction.AuctionStatus.COMPLETED) {
            throw new BusinessException(PaymentErrorCode.AUCTION_NOT_ENDED);
        }
        
        // 4. 낙찰자 확인
        Bid winningBid = bidRepository.findCurrentHighestBidByAuction(auction)
            .orElseThrow(() -> new BusinessException(PaymentErrorCode.AUCTION_NOT_WON));
        
        if (!winningBid.getUser().getId().equals(userId)) {
            throw new BusinessException(PaymentErrorCode.AUCTION_NOT_WON);
        }
        
        // 5. 즉시 구매인지 일반 낙찰인지 확인
        boolean isBuyItNow = winningBid.isBuyItNow();
        
        // 6. 이미 결제 완료된 경매인지 확인 (실제 구현에서는 Payment 엔티티 확인)
        // TODO: Payment 엔티티가 있다면 여기서 확인
        
        // 7. 포인트 및 배송비 계산
        BigDecimal winningBidAmount = winningBid.getBidAmount();
        BigDecimal shippingFee = auction.getShippingFee() != null ? auction.getShippingFee() : BigDecimal.ZERO;
        BigDecimal totalAmount = winningBidAmount.add(shippingFee); // 낙찰가 + 배송비
        BigDecimal userPoint = user.getPoint();
        BigDecimal maxUsablePoint = totalAmount.min(userPoint); // 총 금액과 보유 포인트 중 작은 값
        BigDecimal actualPaymentAmount = totalAmount.subtract(maxUsablePoint);
        BigDecimal remainingPointAfterPayment = userPoint.subtract(maxUsablePoint);
        
        return PaymentPreviewDto.builder()
            .auctionId(auctionId)
            .productName(auction.getProduct().getProductName())
            .winningBidAmount(winningBidAmount)
            .shippingFee(shippingFee)
            .totalAmount(totalAmount)
            .userPoint(userPoint)
            .maxUsablePoint(maxUsablePoint)
            .actualPaymentAmount(actualPaymentAmount)
            .remainingPointAfterPayment(remainingPointAfterPayment)
            .productImageUrl(auction.getProduct().getMainImageUrl())
            .build();
    }
    
    /**
     * 결제 처리  포인트 전부 사용 (일반 낙찰)
     * 
     * @param auctionId 경매 ID
     * @param userId 사용자 ID
     * @return 결제 결과
     */
    @Transactional
    public PaymentResponseDto processPayment(Long auctionId, Long userId) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        
        // 2. 경매 조회
        Auction auction = auctionRepository.findById(auctionId)
            .orElseThrow(() -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));
        
        // 3. 경매 종료 여부 확인
        if (auction.getStatus() != Auction.AuctionStatus.COMPLETED) {
            throw new BusinessException(PaymentErrorCode.AUCTION_NOT_ENDED);
        }
        
        // 4. 낙찰자 확인
        Bid winningBid = bidRepository.findCurrentHighestBidByAuction(auction)
            .orElseThrow(() -> new BusinessException(PaymentErrorCode.AUCTION_NOT_WON));
        
        if (!winningBid.getUser().getId().equals(userId)) {
            throw new BusinessException(PaymentErrorCode.AUCTION_NOT_WON);
        }
        
        // 5. 즉시 구매인지 일반 낙찰인지 확인
        boolean isBuyItNow = winningBid.isBuyItNow();
        
        // 6. 이미 결제 완료된 경매인지 확인 (실제 구현에서는 Payment 엔티티 확인)
        // TODO: Payment 엔티티가 있다면 여기서 확인
        
        // 7. 결제 금액 검증
        BigDecimal winningBidAmount = winningBid.getBidAmount();
        BigDecimal shippingFee = auction.getShippingFee() != null ? auction.getShippingFee() : BigDecimal.ZERO;
        BigDecimal totalAmount = winningBidAmount.add(shippingFee); // 낙찰가 + 배송비
        
        // 포인트 사용 금액 계산 (사용자가 보유한 포인트 전부 사용)
        BigDecimal usePointAmount;
        if (user.getPoint().compareTo(BigDecimal.ZERO) <= 0) {
            // 포인트가 0이면 포인트 사용하지 않음
            usePointAmount = BigDecimal.ZERO;
        } else {
            // 포인트를 보유하고 있으면 전부 사용 (총 금액을 초과하지 않도록)
            usePointAmount = user.getPoint().min(totalAmount);
        }
        
        // 실제 결제 금액 계산
        BigDecimal actualPaymentAmount = totalAmount.subtract(usePointAmount);
        
        // 7. 포인트 차감
        BigDecimal remainingPoint = user.getPoint().subtract(usePointAmount);
        user.setPoint(remainingPoint);
        
        // 8. 결제 처리 (실제 결제 API 호출은 여기서 구현)
        // TODO: 실제 결제 API 호출 로직 구현
        log.info("일반 낙찰 결제 처리 시작: 경매ID={}, 사용자ID={}, 낙찰가={}, 사용포인트={}, 실제결제={}", 
                auctionId, userId, winningBidAmount, usePointAmount, actualPaymentAmount);
        
        // 9. 결제 완료 처리
        // TODO: Payment 엔티티에 결제 정보 저장
        
        // 10. 결제 금액의 1% 포인트 적립
        User.Rank rank  = user.getRank();
        User.RankPercent rankPercent = User.RankPercent.findByUserRank(rank);
        String percent = rankPercent.getDescription();
        BigDecimal userPercent = new BigDecimal(percent);
        BigDecimal pointReward = actualPaymentAmount.multiply(userPercent);
        BigDecimal truncatedPoint = pointReward.setScale(0, RoundingMode.DOWN);
        BigDecimal finalPoint = remainingPoint.add(truncatedPoint);

        user.setPoint(finalPoint);

        // 11. 사용자 참여 횟수 증가 및 랭크 업데이트 (결제 완료 시)
        user.participateInAuction();
        userRepository.save(user);
        
        log.info("일반 낙찰 결제 처리 완료: 경매ID={}, 사용자ID={}, 포인트 적립={}, 참여횟수={}", 
                auctionId, userId, pointReward, user.getParticipationCount());
        
        // 12. WebSocket으로 결제 완료 알림 전송
        auctionNotificationService.sendPaymentCompletedNotification(auctionId, actualPaymentAmount);
        
        return PaymentResponseDto.builder()
            .paymentId(1L) // TODO: 실제 Payment ID로 변경
            .auctionId(auctionId)
            .productName(auction.getProduct().getProductName())
            .winningBidAmount(winningBidAmount)
            .shippingFee(shippingFee)
            .totalAmount(totalAmount)
            .usedPointAmount(usePointAmount)
            .actualPaymentAmount(actualPaymentAmount)
            .remainingPoint(finalPoint)
            .pointReward(pointReward)
            .paymentStatus("COMPLETED")
            .completedAt(LocalDateTime.now())
            .build();
    }

    
    /**
     * 포인트를 최대한 활용한 결제 처리
     * 
     * @param auctionId 경매 ID
     * @param userId 사용자 ID
     * @return 결제 결과
     */
    @Transactional
    public PaymentResponseDto processPaymentWithMaxPoint(Long auctionId, Long userId) {
        // 결제 처리 (자동으로 최대 포인트 사용)
        return processPayment(auctionId, userId);
    }
    
    /**
     * 즉시 구매 처리 (경매 종료만)
     * 
     * @param request 즉시 구매 요청
     * @param userId 사용자 ID
     * @return 즉시 구매 결과
     */
    @Transactional
    public BuyItNowResponseDto processBuyItNow(BuyItNowRequestDto request, Long userId) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        
        // 2. 경매 조회
        Auction auction = auctionRepository.findById(request.getAuctionId())
            .orElseThrow(() -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));
        
        // 3. 경매 상태 확인 (진행 중이어야 함)
        if (auction.getStatus() != Auction.AuctionStatus.IN_PROGRESS) {
            throw new BusinessException(AuctionErrorCode.AUCTION_NOT_IN_PROGRESS);
        }
        
        // 4. 즉시 구매가 설정 여부 확인
        if (auction.getBuyItNowPrice() == null || auction.getBuyItNowPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(AuctionErrorCode.BUY_IT_NOW_NOT_AVAILABLE);
        }

        // 5. 즉시 구매를 위한 입찰 생성 (낙찰자로 설정)
 
        Bid buyItNowBid = Bid.builder()
            .auction(auction)
            .user(user)
            .bidAmount(auction.getBuyItNowPrice())
            .status(Bid.BidStatus.WINNING)
            .isBuyItNow(true)
            .build();
        bidRepository.save(buyItNowBid);
        
        // 7. 경매 종료 처리
        auction.setStatus(Auction.AuctionStatus.COMPLETED);
        auction.setActualEndTime(LocalDateTime.now());
        auction.setEndReason("즉시 구매로 인한 경매 종료");
        auction.setEndedBy(userId);
        auctionRepository.save(auction);
        
        log.info("즉시 구매 경매 종료 완료: 경매ID={}, 사용자ID={}, 즉시구매가={}, 참여횟수={}", 
                request.getAuctionId(), userId, auction.getBuyItNowPrice(), user.getParticipationCount());
        
        // 8. WebSocket으로 즉시 구매 완료 알림 전송
        auctionNotificationService.sendBuyItNowCompletedNotification(request.getAuctionId(), auction.getBuyItNowPrice());
        
        // 9. 결제 미리보기용 정보 반환 (실제 결제는 별도 API에서 처리)
        BigDecimal shippingFee = auction.getShippingFee() != null ? auction.getShippingFee() : BigDecimal.ZERO;
        BigDecimal totalAmount = auction.getBuyItNowPrice().add(shippingFee);
        
        return BuyItNowResponseDto.builder()
            .auctionId(request.getAuctionId())
            .productName(auction.getProduct().getProductName())
            .buyItNowPrice(auction.getBuyItNowPrice())
            .shippingFee(shippingFee)
            .totalAmount(totalAmount)
            .usedPointAmount(BigDecimal.ZERO) // 결제 시점에 계산
            .actualPaymentAmount(totalAmount) // 결제 시점에 계산
            .pointReward(BigDecimal.ZERO) // 결제 시점에 계산
            .remainingPoint(user.getPoint()) // 현재 포인트
            .completedAt(LocalDateTime.now())
            .build();
    }
}
