package com.highlight.highlight_backend.service;

import com.highlight.highlight_backend.domain.Auction;
import com.highlight.highlight_backend.domain.Bid;
import com.highlight.highlight_backend.domain.User;
import com.highlight.highlight_backend.dto.AuctionStatusResponseDto;
import com.highlight.highlight_backend.dto.BidCreateRequestDto;
import com.highlight.highlight_backend.dto.BidResponseDto;
import com.highlight.highlight_backend.dto.WinBidDetailResponseDto;
import com.highlight.highlight_backend.dto.AuctionMyResultResponseDto;
import com.highlight.highlight_backend.exception.BusinessException;
import com.highlight.highlight_backend.exception.AuctionErrorCode;
import com.highlight.highlight_backend.exception.BidErrorCode;
import com.highlight.highlight_backend.exception.UserErrorCode;
import com.highlight.highlight_backend.exception.AuthErrorCode;
import com.highlight.highlight_backend.exception.CommonErrorCode;
import com.highlight.highlight_backend.repository.AuctionRepository;
import com.highlight.highlight_backend.repository.BidRepository;
import com.highlight.highlight_backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 입찰 관련 비즈니스 로직 서비스
 * 
 * @author 전우선
 * @since 2025.08.15
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidService {
    
    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final WebSocketService webSocketService;
    
    /**
     * 입찰 참여
     * 
     * @param request 입찰 요청 정보
     * @param userId 입찰하는 사용자 ID
     * @return 입찰 결과 정보
     */
    @Transactional
    public BidResponseDto createBid(BidCreateRequestDto request, Long userId) {
        log.info("입찰 참여 요청: 사용자={}, 경매={}, 금액={}", userId, request.getAuctionId(), request.getBidAmount());
        
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        
        // 2. 경매 조회 (비관적 락으로 동시 입찰 방지)
        Auction auction = auctionRepository.findByIdWithLock(request.getAuctionId())
            .orElseThrow(() -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));
        
        // 3. 입찰 가능 상태 검증
        validateBidRequest(auction, request, user);
        
        // 4. 동일 금액 입찰 체크 및 처리
        Optional<Bid> existingSamePriceBid = bidRepository.findBidByAuctionAndBidAmount(auction, request.getBidAmount());
        if (existingSamePriceBid.isPresent()) {
            // 동일한 금액으로 이미 입찰이 있는 경우 거부 (선도착 우선)
            throw new BusinessException(AuctionErrorCode.BID_AMOUNT_TOO_LOW);
        }
        
        // 5. 기존 최고 입찰 조회
        Optional<Bid> currentHighestBid = bidRepository.findCurrentHighestBidByAuction(auction);
        
        // 5. 입찰 엔티티 생성
        Bid newBid = new Bid();
        newBid.setAuction(auction);
        newBid.setUser(user);
        newBid.setBidAmount(request.getBidAmount());
        newBid.setIsAutoBid(request.getIsAutoBid() != null ? request.getIsAutoBid() : false);
        newBid.setMaxAutoBidAmount(request.getMaxAutoBidAmount());
        newBid.setStatus(Bid.BidStatus.WINNING);
        
        // 6. 기존 최고 입찰을 OUTBID로 변경 및 개인 알림
        Bid previousWinner = null;
        if (currentHighestBid.isPresent()) {
            previousWinner = currentHighestBid.get();
            previousWinner.setAsOutbid();
            bidRepository.save(previousWinner);
        }
        
        // 7. 새 입찰 저장
        Bid savedBid = bidRepository.save(newBid);
        
        // 8. 사용자가 해당 경매에 처음 입찰하는 경우 참여 횟수 증가
        if (currentHighestBid.isEmpty() || !currentHighestBid.get().getUser().getId().equals(userId)) {
            user.participateInAuction();
            userRepository.save(user);
            log.info("경매 참여 횟수 증가: 사용자ID={}, 새로운 참여횟수={}, 등급={}", 
                    userId, user.getParticipationCount(), user.getRank());
        }
        
        // 9. 경매 정보 업데이트
        updateAuctionInfo(auction, request.getBidAmount());
        
        // 9. WebSocket으로 실시간 알림 전송
        webSocketService.sendNewBidNotification(savedBid);
        
        // 10. 이전 최고 입찰자에게 개인 알림 (다른 사용자인 경우)
        if (previousWinner != null && !previousWinner.getUser().getId().equals(userId)) {
            webSocketService.sendBidOutbidNotification(previousWinner, savedBid);
        }
        
        log.info("입찰 참여 완료: 입찰ID={}, 사용자={}, 금액={}", savedBid.getId(), userId, request.getBidAmount());
        
        return BidResponseDto.fromMyBid(savedBid);
    }
    
    /**
     * 경매 입찰 내역 조회 (익명 처리) - 사용자별 최신 입찰만 반환
     * 
     * @param auctionId 경매 ID
     * @param pageable 페이징 정보
     * @return 입찰 내역 목록 (사용자별 최신 입찰)
     */
    public Page<BidResponseDto> getAuctionBids(Long auctionId, Pageable pageable) {
        log.info("경매 입찰 내역 조회 (익명, 사용자별 최신): 경매ID={}", auctionId);
        
        Auction auction = auctionRepository.findById(auctionId)
            .orElseThrow(() -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));
        
        Page<Bid> bids = bidRepository.findBidsByAuctionOrderByBidAmountDesc(auction, pageable);
        
        return bids.map(BidResponseDto::from);
    }
    
    /**
     * 경매 전체 입찰 내역 조회 (관리자용)
     * 
     * @param auctionId 경매 ID
     * @param pageable 페이징 정보
     * @return 모든 입찰 내역 목록
     */
    public Page<BidResponseDto> getAllAuctionBids(Long auctionId, Pageable pageable) {
        log.info("경매 전체 입찰 내역 조회 (관리자): 경매ID={}", auctionId);
        
        Auction auction = auctionRepository.findById(auctionId)
            .orElseThrow(() -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));
        
        Page<Bid> bids = bidRepository.findAllBidsByAuctionOrderByBidAmountDesc(auction, pageable);
        
        return bids.map(BidResponseDto::from);
    }
    
    /**
     * 경매 입찰 내역 조회 (본인 입찰 강조) - 사용자별 최신 입찰만 반환
     * 
     * @param auctionId 경매 ID
     * @param userId 현재 사용자 ID
     * @param pageable 페이징 정보
     * @return 입찰 내역 목록 (사용자별 최신 입찰, 본인 입찰 강조)
     */
    public Page<BidResponseDto> getAuctionBidsWithUser(Long auctionId, Long userId, Pageable pageable) {
        log.info("경매 입찰 내역 조회 (본인 강조, 사용자별 최신): 경매ID={}, 사용자ID={}", auctionId, userId);
        
        Auction auction = auctionRepository.findById(auctionId)
            .orElseThrow(() -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));
        
        Page<Bid> bids = bidRepository.findBidsByAuctionOrderByBidAmountDesc(auction, pageable);
        
        return bids.map(bid -> BidResponseDto.fromWithUserInfo(bid, userId));
    }
    
    /**
     * 실시간 경매 상태 조회
     * 
     * @param auctionId 경매 ID
     * @return 경매 상태 정보
     */
    public AuctionStatusResponseDto getAuctionStatus(Long auctionId) {
        log.info("실시간 경매 상태 조회: 경매ID={}", auctionId);
        
        Auction auction = auctionRepository.findById(auctionId)
            .orElseThrow(() -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));
        
        // 입찰 통계 조회
        Long totalBidders = bidRepository.countDistinctBiddersByAuction(auction);
        Long totalBids = bidRepository.countBidsByAuction(auction);
        
        // 현재 최고 입찰자 조회
        String winnerNickname = null;
        Optional<Bid> currentWinner = bidRepository.findCurrentHighestBidByAuction(auction);
        if (currentWinner.isPresent()) {
            winnerNickname = currentWinner.get().getUser().getNickname();
        }
        
        return AuctionStatusResponseDto.from(auction, totalBidders, totalBids, winnerNickname);
    }
    
    /**
     * 사용자의 입찰 내역 조회
     * 
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 사용자 입찰 내역
     */
    public Page<BidResponseDto> getUserBids(Long userId, Pageable pageable) {
        log.info("사용자 입찰 내역 조회: 사용자ID={}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        
        Page<Bid> bids = bidRepository.findBidsByUserOrderByCreatedAtDesc(user, pageable);
        
        return bids.map(BidResponseDto::fromMyBid);
    }
    
    /**
     * 사용자의 낙찰 내역 조회
     * 
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 낙찰 내역
     */
    public Page<BidResponseDto> getUserWonBids(Long userId, Pageable pageable) {
        log.info("사용자 낙찰 내역 조회: 사용자ID={}", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        
        Page<Bid> wonBids = bidRepository.findWonBidsByUser(user, pageable);
        
        return wonBids.map(BidResponseDto::fromMyBid);
    }
    
    /**
     * 낙찰 상세 정보 조회
     * 
     * @param bidId 입찰 ID
     * @param userId 사용자 ID
     * @return 낙찰 상세 정보
     */
    public WinBidDetailResponseDto getWinBidDetail(Long bidId, Long userId) {
        log.info("낙찰 상세 정보 조회: 입찰ID={}, 사용자ID={}", bidId, userId);
        
        // 1. 입찰 조회
        Bid bid = bidRepository.findById(bidId)
            .orElseThrow(() -> new BusinessException(BidErrorCode.BID_NOT_FOUND));
        
        // 2. 낙찰된 입찰인지 확인
        if (bid.getStatus() != Bid.BidStatus.WON) {
            throw new BusinessException(BidErrorCode.BID_NOT_FOUND);
        }
        
        // 3. 본인의 입찰인지 확인
        if (!bid.getUser().getId().equals(userId)) {
            throw new BusinessException(AuthErrorCode.ACCESS_DENIED);
        }
        
        // 4. 상세 정보 반환 (사용자별 최신 입찰 기준 통계 적용)
        Auction auction = bid.getAuction();
        Integer calculatedTotalBids = bidRepository.countBidsByAuction(auction).intValue();
        Integer calculatedTotalBidders = bidRepository.countDistinctBiddersByAuction(auction).intValue();
        
        return WinBidDetailResponseDto.fromWithCalculatedStats(bid, calculatedTotalBids, calculatedTotalBidders);
    }
    
    /**
     * 입찰 요청 유효성 검증
     */
    private void validateBidRequest(Auction auction, BidCreateRequestDto request, User user) {
        // 경매 진행 중 여부 확인
        if (!auction.isInProgress()) {
            throw new BusinessException(AuctionErrorCode.CANNOT_START_AUCTION);
        }
        
        // 입찰 금액 검증
        if (auction.getCurrentHighestBid() != null) {
            // 기존 입찰이 있는 경우: 현재 최고가 + 최소 인상폭 이상
            BigDecimal minimumRequiredBid = auction.getCurrentHighestBid().add(auction.getMinimumBid());
            if (request.getBidAmount().compareTo(minimumRequiredBid) < 0) {
                throw new BusinessException(AuctionErrorCode.INVALID_MINIMUM_BID);
            }
        } else {
            // 첫 입찰인 경우: 시작가 이상
            if (request.getBidAmount().compareTo(auction.getStartPrice()) < 0) {
                throw new BusinessException(AuctionErrorCode.INVALID_MINIMUM_BID);
            }
        }
        
        // 입찰 단위 확인
        if (!isValidBidUnit(request.getBidAmount(), auction.getBidUnit())) {
            throw new BusinessException(AuctionErrorCode.BID_UNIT_MISMATCH);
        }
        
        // 자동 입찰인 경우 추가 검증
        if (request.getIsAutoBid() != null && request.getIsAutoBid()) {
            if (request.getMaxAutoBidAmount() == null) {
                throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE);
            }
            if (request.getMaxAutoBidAmount().compareTo(request.getBidAmount()) < 0) {
                throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE);
            }
        }
    }
    
    /**
     * 입찰 단위 유효성 검증
     */
    private boolean isValidBidUnit(BigDecimal bidAmount, BigDecimal bidUnit) {
        if (bidUnit == null || bidUnit.compareTo(BigDecimal.ZERO) <= 0) {
            return true; // 입찰 단위가 설정되지 않은 경우 통과
        }
        
        return bidAmount.remainder(bidUnit).compareTo(BigDecimal.ZERO) == 0;
    }
    
    /**
     * 경매 정보 업데이트
     */
    private void updateAuctionInfo(Auction auction, BigDecimal newBidAmount) {
        auction.setCurrentHighestBid(newBidAmount);
        
        // 사용자별 최신 입찰 기준으로 정확한 통계 계산
        Long totalBidders = bidRepository.countDistinctBiddersByAuction(auction);
        Long totalBids = bidRepository.countBidsByAuction(auction);
        
        auction.setTotalBidders(totalBidders.intValue());
        auction.setTotalBids(totalBids.intValue());
        
        auctionRepository.save(auction);
    }
    
    /**
     * 경매에서 내 결과 조회
     * 
     * @param auctionId 경매 ID
     * @param userId 사용자 ID
     * @return 경매 내 결과 정보
     */
    public AuctionMyResultResponseDto getMyAuctionResult(Long auctionId, Long userId) {
        log.info("경매 내 결과 조회: 경매ID={}, 사용자ID={}", auctionId, userId);
        
        // 경매 조회
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));
        
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        
        // 사용자의 해당 경매 입찰 내역 조회
        Optional<Bid> userBidOpt = bidRepository.findTopBidByAuctionAndUserOrderByBidAmountDesc(auction, user);
        
        // 미참여한 경우
        if (userBidOpt.isEmpty()) {
            return AuctionMyResultResponseDto.createNoParticipationResult(auction);
        }
        
        Bid userBid = userBidOpt.get();
        
        // 경매 취소된 경우
        if (auction.getStatus() == Auction.AuctionStatus.CANCELLED) {
            return AuctionMyResultResponseDto.createCancelledResult(auction, userBid);
        }
        
        // 종료되지 않은 경매인 경우 에러
        if (auction.getStatus() != Auction.AuctionStatus.COMPLETED && 
            auction.getStatus() != Auction.AuctionStatus.FAILED) {
            throw new BusinessException(BidErrorCode.AUCTION_NOT_ENDED);
        }
        
        // 낙찰자 조회
        Optional<Bid> winnerBidOpt = bidRepository.findCurrentHighestBidByAuction(auction);
        
        if (winnerBidOpt.isEmpty()) {
            // 입찰 없이 종료된 경우 (이론적으로 불가능하지만 안전장치)
            return AuctionMyResultResponseDto.createNoParticipationResult(auction);
        }
        
        Bid winnerBid = winnerBidOpt.get();
        
        // 낙찰 여부 확인
        if (winnerBid.getUser().getId().equals(userId)) {
            // 낙찰
            return AuctionMyResultResponseDto.createWonResult(auction, winnerBid);
        } else {
            // 유찰
            return AuctionMyResultResponseDto.createLostResult(auction, userBid, winnerBid.getBidAmount());
        }
    }
}