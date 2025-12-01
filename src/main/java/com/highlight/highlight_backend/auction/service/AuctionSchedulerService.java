
package com.highlight.highlight_backend.auction.service;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.auction.repository.AuctionRepository;
import com.highlight.highlight_backend.product.domian.Product;
import com.highlight.highlight_backend.bid.repository.BidRepository;
import com.highlight.highlight_backend.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionSchedulerService {

    private final TaskScheduler taskScheduler;

    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;

    private final AuctionNotificationService auctionNotificationService;

    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public void scheduleAuctionStart(Auction auction) {
        cancelScheduledStart(auction.getId()); // 기존 작업이 있다면 취소

        Instant startTime = auction.getScheduledStartTime().atZone(java.time.ZoneId.systemDefault()).toInstant();
        
        Runnable task = () -> {
            startAuction(auction.getId());
        };

        ScheduledFuture<?> scheduledTask = taskScheduler.schedule(task, startTime);
        scheduledTasks.put(auction.getId(), scheduledTask);
        log.info("경매 시작 작업이 스케줄되었습니다. 경매 ID: {}, 시작 시간: {}", auction.getId(), auction.getScheduledStartTime());
    }

    public void cancelScheduledStart(Long auctionId) {
        ScheduledFuture<?> scheduledTask = scheduledTasks.get(auctionId);
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTasks.remove(auctionId);
            log.info("경매 시작 스케줄이 취소되었습니다. 경매 ID: {}", auctionId);
        }
    }

    @Transactional
    public void startAuction(Long auctionId) {
        Auction auction = auctionRepository.getOrThrow(auctionId);
        if (auction != null && auction.getStatus() == Auction.AuctionStatus.SCHEDULED) {
            // 경매 상태를 IN_PROGRESS로 변경
            auction.setStatus(Auction.AuctionStatus.IN_PROGRESS);
            
            // 상품 상태를 IN_AUCTION으로 변경 (Product를 별도로 조회)
            if (auction.getProduct() != null) {
                auction.getProduct().setStatus(Product.ProductStatus.IN_AUCTION);
                log.info("상품 상태가 IN_AUCTION으로 변경되었습니다. 상품 ID: {}", auction.getProduct().getId());
                auctionNotificationService.sendAuctionStartedNotification(auction);
            }
        }

        log.info("스케줄된 경매가 시작되었습니다. 경매 ID: {}, 상품 상태 변경: IN_AUCTION", auctionId);
        scheduledTasks.remove(auctionId);
    }


    @Scheduled(fixedRate = 60000) // 1분마다 실행
    @Transactional
    public void checkMissedScheduledAuctions() {
        log.debug("놓친 경매가 있는지 확인합니다...");
        List<Auction> missedAuctions = auctionRepository.findByStatusAndScheduledStartTimeBefore(Auction.AuctionStatus.SCHEDULED, LocalDateTime.now());
        
        if (!missedAuctions.isEmpty()) {
            log.info("{}개의 놓친 경매를 발견했습니다. 지금 시작합니다.", missedAuctions.size());
            for (Auction auction : missedAuctions) {
                startAuction(auction.getId());
            }
        }
    }

    @Scheduled(fixedRate = 60000) // 1분마다 실행
    @Transactional
    public void checkExpiredAuctions() {
        log.debug("종료된 경매가 있는지 확인합니다...");
        List<Auction> expiredAuctions = auctionRepository.findInProgressAuctionsReadyToEnd(LocalDateTime.now());
        for (Auction auction : expiredAuctions) {
            try {
                // 경매 상태를 COMPLETED로 변경
                auction.endAuction(1L,"경매 시간 만료로 인한 자동 종료");

                if (auction.getProduct() != null) {
                    auction.getProduct().setStatus(Product.ProductStatus.AUCTION_COMPLETED);
                }
                // 낙찰자 찾기
                var winnerBid = bidRepository.findCurrentHighestBidByAuction(auction).orElse(null);
                if (winnerBid != null) {
                    auctionNotificationService.notifyAuctionEnded(auction, winnerBid.getUser().getNickname());
                }

                log.info("경매가 자동으로 종료되었습니다. 경매 ID: {}", auction.getId());
            } catch (Exception e) {
                log.error("경매 자동 종료 중 오류 발생. 경매 ID: {}, 오류: {}", auction.getId(), e.getMessage(), e);
            }
        }

    }
}
