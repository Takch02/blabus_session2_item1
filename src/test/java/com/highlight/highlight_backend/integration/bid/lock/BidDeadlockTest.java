package com.highlight.highlight_backend.integration.bid.lock;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.auction.repository.AuctionRepository;
import com.highlight.highlight_backend.bid.application.BidFacade;
import com.highlight.highlight_backend.bid.dto.BidCreateRequestDto;
import com.highlight.highlight_backend.user.dto.UserUpdateRequestDto;
import com.highlight.highlight_backend.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class BidDeadlockTest {

    @Autowired
    private BidFacade bidFacade;
    @Autowired
    private UserService userService;
    @Autowired
    private AuctionRepository auctionRepository;

    private static final Long AUCTION_ID = 2L;
    private static final BigDecimal BID_UNIT = BigDecimal.valueOf(1100);

    @Test
    @DisplayName("동시성 테스트: 입찰과 유저 수정이 동시에 몰려도 데드락이 안 터져야 한다")
    void deadlockCheck() throws InterruptedException {
        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        // DB에서 현재가 조회 후 스레드마다 고유한 유효 입찰가 배정
        Auction auction = auctionRepository.findById(AUCTION_ID).orElseThrow();
        BigDecimal base = auction.getCurrentHighestBid() != null
                ? auction.getCurrentHighestBid() : auction.getStartPrice();

        AtomicInteger bidSuccessCount = new AtomicInteger();    // 실제 입찰 성공
        AtomicInteger userUpdateSuccessCount = new AtomicInteger(); // user update
        AtomicInteger lockTimeoutCount = new AtomicInteger();   // 분산락 경합 타임아웃 (정상)
        AtomicInteger failCount = new AtomicInteger();          // 진짜 에러 (데드락 등)
        AtomicInteger bidPriceFailCount = new AtomicInteger();  // 순서역전 입찰가 미달 (정상 실패)
        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            String safePhoneNumber = "0109999888" + index;
            // 짝수 스레드(입찰)에만 순번별 가격 배정 (index/2 번째 입찰가)
            BigDecimal currentPrice = base.add(BID_UNIT.multiply(BigDecimal.valueOf(index / 2 + 1)));
            UserUpdateRequestDto dynamicUpdateDto = new UserUpdateRequestDto(
                    "닉네임" + index,
                    safePhoneNumber,
                    true,
                    true
            );
            executorService.execute(() -> {
                try {
                    // 짝수 스레드: 입찰 시도 (Auction Lock -> User Update Event)
                    if (index % 2 == 0) {
                        bidFacade.createBidFacade(new BidCreateRequestDto(AUCTION_ID, currentPrice, false,
                                BID_UNIT), 1L);
                        bidSuccessCount.getAndIncrement();
                    }
                    // 홀수 스레드: 유저 정보 수정 (User Lock -> Auction Lock)
                    else {
                        userService.updateUser(1L, dynamicUpdateDto);
                        userUpdateSuccessCount.getAndIncrement();
                    }
                } catch (Exception e) {
                    String msg = e.getMessage() == null ? "" : e.getMessage();
                    if (msg.contains("입찰가")) {
                        // 스레드 순서 역전으로 인한 입찰가 미달 — 데드락 아님
                        System.out.println("정상 실패(순서역전): " + e.getMessage());
                        bidPriceFailCount.getAndIncrement();
                    } else if (msg.contains("지연")) {
                        // 분산락 경합 타임아웃 — 데드락 아님, 별도 집계
                        System.out.println("정상 실패(락경합): " + e.getMessage());
                        lockTimeoutCount.getAndIncrement();
                    } else {
                        // 진짜 데드락이나 시스템 에러만 실패로 간주
                        System.out.println("심각한 에러 발생: " + e.getMessage() + ", index : " + index);
                        failCount.getAndIncrement();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드가 끝날 때까지 대기

        // 데드락이 없어야 함
        assertThat(failCount.get()).isEqualTo(0);
        // 전체 스레드가 빠짐없이 처리되어야 함
        assertThat(bidSuccessCount.get() + userUpdateSuccessCount.get() + lockTimeoutCount.get() + bidPriceFailCount.get()).isEqualTo(numberOfThreads);
        // 최소 1건은 실제로 입찰이 진행되었어야 함 (아무것도 안 된 상태가 아님을 보장)
        assertThat(bidSuccessCount.get()).isGreaterThan(0);
    }
}