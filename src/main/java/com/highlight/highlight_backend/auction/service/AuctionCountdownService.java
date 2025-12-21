package com.highlight.highlight_backend.auction.service;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.auction.repository.AuctionRepository;
import com.highlight.highlight_backend.bid.service.BidNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 경매 카운트다운 실시간 전송 서비스
 * 
 * 진행 중인 경매의 남은 시간을 주기적으로 계산하여 WebSocket으로 전송합니다.
 * 
 * @author 전우선
 * @since 2025.08.16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionCountdownService {

    private final AuctionRepository auctionRepository;
    private final AuctionNotificationService auctionNotificationService;
    private final BidNotificationService bidNotificationService;
    
    /**
     * 진행 중인 경매의 남은 시간을 주기적으로 전송 (1초마다)
     */
    @Transactional(readOnly = true)
    //@Scheduled(fixedRate = 1000) // 1초마다 실행
    public void sendCountdownUpdates() {
        try {
            // 진행 중인 경매만 조회
            List<Auction> inProgressAuctions = auctionRepository.findByStatus(Auction.AuctionStatus.IN_PROGRESS);
            
            for (Auction auction : inProgressAuctions) {
                // 종료 시간이 지났는지 확인 (한국 시간 기준)
                if (LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul")).isAfter(auction.getScheduledEndTime())) {
                    log.info("경매 종료 시간 도달: 경매ID={}", auction.getId());
                    continue; // 종료된 경매는 스킵 (AuctionService에서 별도 처리)
                }
                
                // WebSocket으로 경매 상태 업데이트 전송 (남은 시간 포함)
                bidNotificationService.sendAuctionStatusUpdate(auction);
            }
            
            if (!inProgressAuctions.isEmpty()) {
                log.debug("카운트다운 업데이트 전송 완료: {} 개 경매", inProgressAuctions.size());
            }
            
        } catch (Exception e) {
            log.error("카운트다운 업데이트 전송 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 경매 종료 임박 알림 (마지막 1분)
     */
    //@Scheduled(fixedRate = 10000) // 10초마다 실행
    public void sendEndingSoonAlerts() {
        try {
            LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
            LocalDateTime oneMinuteLater = now.plusMinutes(1);
            
            // 1분 내로 종료 예정인 경매 조회
            List<Auction> endingSoonAuctions = auctionRepository.findByStatusAndScheduledEndTimeBetween(
                Auction.AuctionStatus.IN_PROGRESS, now, oneMinuteLater);
            
            for (Auction auction : endingSoonAuctions) {
                long remainingSeconds = java.time.Duration.between(now, auction.getScheduledEndTime()).getSeconds();
                
                if (remainingSeconds <= 60 && remainingSeconds > 0) {
                    log.info("경매 종료 임박 알림: 경매ID={}, 남은시간={}초", auction.getId(), remainingSeconds);
                    auctionNotificationService.sendEndingSoonAlert(auction, remainingSeconds);
                }
            }
            
        } catch (Exception e) {
            log.error("종료 임박 알림 전송 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}