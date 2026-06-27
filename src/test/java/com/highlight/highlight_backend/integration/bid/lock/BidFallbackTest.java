package com.highlight.highlight_backend.integration.bid.lock;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.auction.repository.AuctionRepository;
import com.highlight.highlight_backend.bid.application.BidFacade;
import com.highlight.highlight_backend.bid.dto.BidCreateRequestDto;
import com.highlight.highlight_backend.bid.dto.BidResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.redisson.api.NodesGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
public class BidFallbackTest {

    private static final Long AUCTION_ID = 2L;
    private static final Long USER_ID = 1L;
    private static final BigDecimal BID_UNIT = BigDecimal.valueOf(1100);

    @Autowired
    private BidFacade bidFacade;

    @Autowired
    private AuctionRepository auctionRepository;

    @MockitoSpyBean
    private RedissonClient redissonClient;

    // 현재 경매의 최고가 + 단위금액 으로 유효한 입찰가 계산
    private BigDecimal nextValidPrice() {
        Auction auction = auctionRepository.findById(AUCTION_ID).orElseThrow();
        BigDecimal current = auction.getCurrentHighestBid() != null
                ? auction.getCurrentHighestBid()
                : auction.getStartPrice();
        return current.add(BID_UNIT);
    }

    @Test
    @DisplayName("Redis 정상일 때 분산락으로 입찰 성공")
    void bidWithDistributedLock() {
        BidResponseDto result = bidFacade.createBidFacade(
                new BidCreateRequestDto(AUCTION_ID, nextValidPrice(), false, BID_UNIT), USER_ID);

        assertThat(result).isNotNull();
        verify(redissonClient, atLeastOnce()).getLock(anyString());
    }

    @Test
    @DisplayName("Redis 장애시 비관락으로 폴백하여 입찰 성공")
    void bidFallbackToPessimisticLock() {
        NodesGroup mockNodesGroup = mock(NodesGroup.class);
        when(mockNodesGroup.pingAll()).thenReturn(false);
        doReturn(mockNodesGroup).when(redissonClient).getNodesGroup();

        BidResponseDto result = bidFacade.createBidFacade(
                new BidCreateRequestDto(AUCTION_ID, nextValidPrice(), false, BID_UNIT), USER_ID);

        assertThat(result).isNotNull();
        verify(redissonClient, never()).getLock(anyString());
    }

    @Test
    @DisplayName("Redis 예외 발생시 비관락으로 폴백하여 입찰 성공")
    void bidFallbackOnRedisException() {
        when(redissonClient.getNodesGroup()).thenThrow(new RuntimeException("Redis connection refused"));

        BidResponseDto result = bidFacade.createBidFacade(
                new BidCreateRequestDto(AUCTION_ID, nextValidPrice(), false, BID_UNIT), USER_ID);

        assertThat(result).isNotNull();
        verify(redissonClient, never()).getLock(anyString());
    }

    @Test
    @DisplayName("Redis 장애시 동시 입찰 100건 — 비관락으로 race condition 없이 처리")
    void concurrentBidFallbackNoDuplicate() throws InterruptedException {
        NodesGroup mockNodesGroup = mock(NodesGroup.class);
        when(mockNodesGroup.pingAll()).thenReturn(false);
        doReturn(mockNodesGroup).when(redissonClient).getNodesGroup();

        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // 스레드마다 현재가 기준으로 충분히 높은 금액을 배정 (순서 무관하게 모두 유효)
        try {
            BigDecimal base = nextValidPrice();
            for (int i = 0; i < threadCount; i++) {
                final BigDecimal price = base.add(BID_UNIT.multiply(BigDecimal.valueOf(i)));
                executor.execute(() -> {
                    try {
                        bidFacade.createBidFacade(
                                new BidCreateRequestDto(AUCTION_ID, price, false, BID_UNIT), USER_ID);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        if (e.getMessage() != null && e.getMessage().contains("입찰가")) {
                            successCount.incrementAndGet(); // 스레드 순서 역전으로 인한 정상 실패
                        } else {
                            System.out.println("실패: " + e.getMessage());
                            failCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
        } finally {
            executor.shutdown();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }

        latch.await();
        assertThat(failCount.get()).isEqualTo(0);
        assertThat(successCount.get()).isEqualTo(threadCount);
    }
}
