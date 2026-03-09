package com.highlight.highlight_backend.bid.service;

import com.github.f4b6a3.tsid.TsidCreator;
import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.bid.domain.Bid;
import com.highlight.highlight_backend.bid.dto.AuctionMyResultResponseDto;
import com.highlight.highlight_backend.bid.event.BidCompleteEvent;
import com.highlight.highlight_backend.bid.event.BidNotificationEvent;
import com.highlight.highlight_backend.common.outbox.OutboxService;
import com.highlight.highlight_backend.user.domain.User;
import com.highlight.highlight_backend.bid.dto.BidCreateRequestDto;
import com.highlight.highlight_backend.bid.dto.BidResponseDto;
import com.highlight.highlight_backend.bid.dto.WinBidDetailResponseDto;
import com.highlight.highlight_backend.exception.BusinessException;
import com.highlight.highlight_backend.exception.BidErrorCode;
import com.highlight.highlight_backend.bid.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 입찰 관련 비즈니스 로직 서비스
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidService {

    private final BidRepository bidRepository;
    private final BidNotificationService bidNotificationService;
    private final ApplicationEventPublisher eventPublisher;  // spring container 에 넣어주는 인터페이스
    private final OutboxService outboxService;

    /**
     * 입찰 참여
     * 변경 전 lock 순서 : Auction lock -> User lock -> 둘 다 Unlock
     */
    @Transactional
    public BidResponseDto createBid(BidCreateRequestDto request, User user, Auction auction) {
        log.info("입찰 참여 요청: 사용자={}, 경매={}, 금액={}", user.getId(), request.getAuctionId(), request.getBidAmount());

        // 경매, 입찰이 가능한 상태인지 검증
        // 예외 발생 시 종료.
        auction.validateBid(request.getBidAmount());

        // 이전 최고 입찰자 찾기
        // (락이 걸려있으므로 가장 최신 데이터임이 보장됨)
        Bid previousTopBid = bidRepository.findTopByAuctionOrderByBidAmountDesc(auction)
                .orElse(null);

        // 새로운 입찰자인지 찾음
        boolean isNewBidder = !bidRepository.existsByAuctionAndUser(auction, user);

        // 입찰 엔티티 생성
        Bid newBid = Bid.createBid(request, auction, user);
        // 새 입찰 저장
        Bid savedBid = bidRepository.save(newBid);

        // 6. 기존 최고 입찰을 OUTBID로 변경 및 개인 알림
        if (previousTopBid != null) {
            previousTopBid.outBid();
        }

        // 경매 최고가 갱신 및 입찰 참여자 수 갱신
        auction.updateHighestBid(user, request.getBidAmount(), isNewBidder);

        // Listener 에게 던지기 전에 null 체크
        Long previousBidId = (previousTopBid != null) ? previousTopBid.getId() : null;

        saveOutBoxAndPublish(user.getId(), auction, savedBid, previousBidId, isNewBidder);

        log.info("입찰 참여 완료: 입찰ID={}, 사용자={}, 금액={}", savedBid.getId(), user.getId(), request.getBidAmount());

        return BidResponseDto.fromMyBid(savedBid);
    }

    private void saveOutBoxAndPublish(Long userId, Auction auction, Bid savedBid, Long previousBidId, boolean isNewBidder) {
        // outbox 에 저장하기

        // 1. App에서 ID 생성 (시간순 정렬된 Long 값)
        long userEventOutboxId = TsidCreator.getTsid().toLong();
        long bidNotOutbid = TsidCreator.getTsid().toLong();

        // user.participation_count++ 를 위한 event
        BidCompleteEvent userEvent = new BidCompleteEvent(userId, userEventOutboxId);
        // outbox에 저장
        outboxService.appendEvent(userEventOutboxId, "BID_USER_UPDATE", userId, userEvent);

        // 입찰 메시지를 위한 event
        BidNotificationEvent bidEvent = new BidNotificationEvent(userId, auction.getId(), savedBid.getId(), previousBidId,
                savedBid.getBidAmount(), isNewBidder, bidNotOutbid);
        // outbox에 저장
        outboxService.appendEvent(bidNotOutbid, "BID_NOTI", savedBid.getId(), bidEvent);

        // User.participationCount 증가 및 Websocket 메시지 전송은 EventListener 에게 비동기로 처리
        eventPublisher.publishEvent(userEvent);
        eventPublisher.publishEvent(bidEvent);
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