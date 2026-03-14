package com.highlight.highlight_backend.bid.service;

import com.github.f4b6a3.tsid.TsidCreator;
import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.auction.service.UserAuctionService;
import com.highlight.highlight_backend.bid.domain.Bid;
import com.highlight.highlight_backend.bid.dto.AuctionMyResultResponseDto;
import com.highlight.highlight_backend.bid.event.BidCreatedEvent;
import com.highlight.highlight_backend.common.outbox.OutboxService;
import com.highlight.highlight_backend.exception.AuctionErrorCode;
import com.highlight.highlight_backend.user.domain.User;
import com.highlight.highlight_backend.bid.dto.BidCreateRequestDto;
import com.highlight.highlight_backend.bid.dto.BidResponseDto;
import com.highlight.highlight_backend.bid.dto.WinBidDetailResponseDto;
import com.highlight.highlight_backend.exception.BusinessException;
import com.highlight.highlight_backend.exception.BidErrorCode;
import com.highlight.highlight_backend.bid.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.highlight.highlight_backend.exception.AuctionErrorCode.ALREADY_HAVE_LOCK;

/**
 * 입찰 관련 비즈니스 로직 서비스
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidService {

    private final UserAuctionService userAuctionService;
    private final BidRepository bidRepository;
    private final ApplicationEventPublisher eventPublisher;  // spring container 에 넣어주는 인터페이스
    private final OutboxService outboxService;
    /**
     * 특정 경매의 최고 입찰자 조회
     */
    public Optional<Bid> findCurrentHighestBid(Auction auction) {
        return bidRepository.findCurrentHighestBidByAuction(auction);
    }

    /**
     * 입찰을 낙찰 상태로 변경
     */
    @Transactional
    public void setBidAsWon(Bid bid) {
        log.info("입찰 낙찰 처리: 입찰ID={}, 사용자={}", bid.getId(), bid.getUser().getId());
        bid.setAsWon();
    }

    /**
     * 즉시구매 입찰 생성
     */
    @Transactional
    public Bid createBuyItNowBid(Auction auction, User user) {
        Bid buyItNowBid = new Bid();
        buyItNowBid.setAuction(auction);
        buyItNowBid.setUser(user);
        buyItNowBid.setBidAmount(auction.getBuyItNowPrice());
        buyItNowBid.setCreatedAt(LocalDateTime.now());
        buyItNowBid.setIsBuyItNow(true);

        return bidRepository.save(buyItNowBid);
    }

    /**
     * 입찰 참여
     * 변경 전 lock 순서 : Auction lock -> User lock -> 둘 다 Unlock
     */
    @Transactional
    public BidResponseDto createBid(BidCreateRequestDto request, User user) {

        log.info("입찰 참여 요청: 사용자={}, 경매={}, 금액={}", user.getId(), request.getAuctionId(), request.getBidAmount());
        // 락 없는 일반 조회
        Auction auction = userAuctionService.getAuctionOrThrow(request.getAuctionId());
        auction.validateBid(request.getBidAmount());

        Bid previousTopBid = bidRepository.findTopByAuctionOrderByBidAmountDesc(auction)
                .orElse(null);

        boolean isNewBidder = !bidRepository.existsByAuctionAndUser(auction, user);

        Bid newBid = Bid.createBid(request, auction, user);
        Bid savedBid = bidRepository.save(newBid);

        Long previousBidId = null;
        if (previousTopBid != null) {
            previousTopBid.outBid();
            previousBidId = previousTopBid.getId();
        }

        // === 락은 풀렸지만 아직 같은 트랜잭션 안 ===
        saveOutBoxAndPublish(
                user.getId(),
                auction.getId(),
                savedBid.getId(),
                savedBid.getBidAmount(),
                previousBidId,
                isNewBidder,
                user.getNickname()
        );

        log.info("입찰 참여 완료: 입찰ID={}, 사용자={}, 금액={}", savedBid.getId(), user.getId(), request.getBidAmount());
        return BidResponseDto.fromMyBid(savedBid);
    }

    private void saveOutBoxAndPublish(Long userId, Long auctionId, Long bidId, BigDecimal bidAmount, Long previousBidId, boolean isNewBidder, String userNickname) {

        // 1. Outbox ID 생성
        long outboxId = TsidCreator.getTsid().toLong();

        // 2. 모든 도메인(유저, 경매, 알림)이 필요로 하는 정보를 모두 담은 '단일 이벤트' 생성
        BidCreatedEvent bidCreatedEvent = new BidCreatedEvent(
                outboxId, userId, auctionId, bidId, previousBidId, bidAmount, isNewBidder, userNickname
        );

        // 3. 이벤트 주체는 BID
        outboxService.appendEvent(
                outboxId,
                "BID",     // aggregateType: 이벤트 발생 주체 도메인
                bidId,     // aggregateId: 발생 주체의 식별자
                bidCreatedEvent
        );

        // 4. 이벤트 발행
        eventPublisher.publishEvent(bidCreatedEvent);
    }


    /**
     * 낙찰 상세 정보 조회
     *
     * @param bidId  입찰 ID
     * @param userId 사용자 ID
     * @return 낙찰 상세 정보
     */
    public WinBidDetailResponseDto getWinBidDetail(Long bidId, Long userId) {
        log.info("낙찰 상세 정보 조회: 입찰ID={}, 사용자ID={}", bidId, userId);

        // 1. 입찰 조회
        Bid bid = bidRepository.findById(bidId)
                .orElseThrow(() -> new BusinessException(BidErrorCode.BID_NOT_FOUND));

        // 2. 입찰 검증
        bid.validateWinBid(userId, bid);

        // 4. 상세 정보 반환 (사용자별 최신 입찰 기준 통계 적용)
        Auction auction = bid.getAuction();
        Long totalBidders = auction.getTotalBidders();
        Long totalBids = auction.getTotalBids();

        return WinBidDetailResponseDto.fromWithCalculatedStats(bid, totalBids, totalBidders);
    }

    public Page<Bid> findWonBidsByUser(User user, Pageable pageable) {
        return bidRepository.findWonBidsByUser(user, pageable);
    }

    public Page<Bid> findBidsByUserOrderByCreatedAtDesc(User user, Pageable pageable) {
        return bidRepository.findBidsByUserOrderByCreatedAtDesc(user, pageable);
    }

    public Page<Bid> findAllBidsByAuctionOrderByBidAmountDesc(Auction auction, Pageable pageable) {
        return bidRepository.findAllBidsByAuctionOrderByBidAmountDesc(auction, pageable);
    }

    public Page<Bid> findBidsByAuctionOrderByBidAmountDesc(Auction auction, Pageable pageable) {
        return bidRepository.findBidsByAuctionOrderByBidAmountDesc(auction, pageable);
    }

    public AuctionMyResultResponseDto calculateMyAuctionResult(Long auctionId, Long userId, Auction auction) {
        // 사용자의 해당 경매 입찰 내역 조회
        Optional<Bid> userBidOpt = bidRepository.findTopBidByAuction_IdAndUser_IdOrderByBidAmountDesc(auctionId, userId);

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

        // 낙찰 여부 확인
        if (auction.getWinnerId().equals(userId)) {
            // 낙찰
            return AuctionMyResultResponseDto.createWonResult(auction, userBid);
        } else {
            // 유찰
            return AuctionMyResultResponseDto.createLostResult(auction, userBid, auction.getCurrentHighestBid());
        }
    }
}