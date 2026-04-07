package com.highlight.highlight_backend.auction.service;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.auction.repository.AuctionRepository;
import com.highlight.highlight_backend.product.domian.Product;
import com.highlight.highlight_backend.bid.service.BidService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionSchedulerService {

    @Qualifier("taskScheduler")
    private final TaskScheduler taskScheduler;

    private final BidService bidService;
    private final AuctionNotificationService auctionNotificationService;
    private final AuctionCountService auctionCountService;
    private final AuctionStartService auctionStartService;

    private final AuctionRepository auctionRepository;

    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public void scheduleAuctionStart(Auction auction) {
        cancelScheduledStart(auction.getId()); // 기존 작업이 있다면 취소

        Instant startTime = auction.getScheduledStartTime().atZone(ZoneId.systemDefault()).toInstant();
        
        Runnable task = () -> {
            auctionStartService.startAuction(auction.getId());
            scheduledTasks.remove(auction.getId());
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

    @PostConstruct
    public void restoreScheduledAuctions() {
        List<Auction> scheduledAuctions = auctionRepository
                .findByStatus(Auction.AuctionStatus.SCHEDULED);

        for (Auction auction : scheduledAuctions) {
            if (auction.getScheduledStartTime().isAfter(LocalDateTime.now())) {
                scheduleAuctionStart(auction);  // 재등록
                log.info("경매 재스케줄 완료. ID: {}", auction.getId());
            }
        }
    }

    /**
     * 경매가 스케줄
     */



    @Scheduled(fixedRate = 60000) // 1분마다 실행
    @Transactional
    public void checkMissedScheduledAuctions() {
        log.debug("놓친 경매가 있는지 확인합니다...");
        List<Auction> missedAuctions = auctionRepository.findByStatusAndScheduledStartTimeBefore(Auction.AuctionStatus.SCHEDULED, LocalDateTime.now());
        
        if (!missedAuctions.isEmpty()) {
            log.info("{}개의 놓친 경매를 발견했습니다. 지금 시작합니다.", missedAuctions.size());
            for (Auction auction : missedAuctions) {
                auctionStartService.startAuction(auction.getId());
                scheduledTasks.remove(auction.getId());
            }
        }
    }

    @Scheduled(fixedRate = 60000) // 1분마다 실행
    @Transactional
    public void checkExpiredAuctions() {
        List<Auction> expiredAuctions = auctionRepository.findInProgressAuctionsReadyToEnd(LocalDateTime.now());
        for (Auction auction : expiredAuctions) {

            Product.Category category = auction.getProduct().getCategory();

            // In_Progress count 감소
            auctionCountService.decrement(Auction.AuctionStatus.IN_PROGRESS, category);

            // 낙찰자 찾기
            var winnerBid = bidService.findCurrentHighestBid(auction).orElse(null);
            if (winnerBid != null) {
                auctionNotificationService.notifyAuctionEnded(auction, winnerBid.getUser().getNickname());
            }

            log.info("경매가 자동으로 종료되었습니다. 경매 ID: {}", auction.getId());
        }
    }

}
