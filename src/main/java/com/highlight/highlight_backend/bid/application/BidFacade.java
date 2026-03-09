package com.highlight.highlight_backend.bid.application;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.auction.service.UserAuctionService;
import com.highlight.highlight_backend.bid.domain.Bid;
import com.highlight.highlight_backend.bid.dto.AuctionMyResultResponseDto;
import com.highlight.highlight_backend.bid.dto.AuctionStatusResponseDto;
import com.highlight.highlight_backend.bid.dto.BidCreateRequestDto;
import com.highlight.highlight_backend.bid.dto.BidResponseDto;
import com.highlight.highlight_backend.bid.service.BidService;
import com.highlight.highlight_backend.user.domain.User;
import com.highlight.highlight_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BidFacade {

    private final UserService userService;
    private final UserAuctionService userAuctionService;
    private final BidService bidService;

    /**
     * 입찰 생성 UseCase
     * 트랜잭션을 여기서 시작하여, 락 획득부터 입찰 저장까지 하나의 작업 단위로 묶음
     */
    @Transactional
    public BidResponseDto createBidFacade(BidCreateRequestDto request, Long userId) {
        // 1. 타 도메인 데이터 조회 (User)
        User user = userService.getUserOrThrow(userId);

        // 2. 타 도메인 데이터 조회 및 락 획득 (Auction)
        // 주의: AuctionService 안에 findByIdWithLock 을 호출하는 메서드가 있어야 함
        Auction auction = userAuctionService.getAuctionWithLockOrThrow(request.getAuctionId());

        // 3. 핵심 비즈니스 로직 위임 (Bid)
        return bidService.createBid(request, user, auction);
    }

    /**
     * 경매 상태 조회 등 다른 읽기 작업도 여기서 조합
     */
    @Transactional(readOnly = true)
    public AuctionStatusResponseDto getAuctionStatus(Long auctionId) {
        log.info("실시간 경매 상태 조회: 경매ID={}", auctionId);

        Auction auction = userAuctionService.findAuctionOrThrow(auctionId);

        // 입찰 통계 조회
        Long totalBidders = auction.getTotalBidders();
        Long totalBids = auction.getTotalBids();

        // 현재 최고 입찰자 조회
        String winnerNickname = auction.getCurrentWinnerName();

        return AuctionStatusResponseDto.from(auction, totalBidders, totalBids, winnerNickname);
    }

    @Transactional(readOnly = true)
    public AuctionMyResultResponseDto getMyAuctionResult(Long auctionId, Long userId) {
        log.info("경매 내 결과 조회: 경매ID={}, 사용자ID={}", auctionId, userId);

        // 경매 조회
        Auction auction = userAuctionService.findAuctionOrThrow(auctionId);

        return bidService.calculateMyAuctionResult(auctionId, userId, auction);
    }



    /**
     * 사용자의 입찰 내역 조회
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 사용자 입찰 내역
     */
    @Transactional(readOnly = true)
    public Page<BidResponseDto> getUserBids(Long userId, Pageable pageable) {
        log.info("사용자 입찰 내역 조회: 사용자ID={}", userId);

        User user = userService.getUserOrThrow(userId);

        Page<Bid> bids = bidService.findBidsByUserOrderByCreatedAtDesc(user, pageable);

        return bids.map(BidResponseDto::fromMyBid);
    }

    /**
     * 사용자의 낙찰 내역 조회
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 낙찰 내역
     */
    @Transactional(readOnly = true)
    public Page<BidResponseDto> getUserWonBids(Long userId, Pageable pageable) {
        log.info("사용자 낙찰 내역 조회: 사용자ID={}", userId);

        User user = userService.getUserOrThrow(userId);

        Page<Bid> wonBids = bidService.findWonBidsByUser(user, pageable);

        return wonBids.map(BidResponseDto::fromMyBid);
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

        Auction auction = userAuctionService.findAuctionOrThrow(auctionId);

        Page<Bid> bids = bidService.findBidsByAuctionOrderByBidAmountDesc(auction, pageable);

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

        Auction auction = userAuctionService.findAuctionOrThrow(auctionId);

        Page<Bid> bids = bidService.findAllBidsByAuctionOrderByBidAmountDesc(auction, pageable);

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

        Auction auction = userAuctionService.findAuctionOrThrow(auctionId);

        Page<Bid> bids = bidService.findBidsByAuctionOrderByBidAmountDesc(auction, pageable);

        return bids.map(bid -> BidResponseDto.fromWithUserInfo(bid, userId));
    }


}