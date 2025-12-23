package com.highlight.highlight_backend.bid.lock;

import com.highlight.highlight_backend.auction.repository.AuctionRepository;
import com.highlight.highlight_backend.bid.dto.BidCreateRequestDto;
import com.highlight.highlight_backend.bid.service.BidService;
import com.highlight.highlight_backend.user.dto.UserUpdateRequestDto;
import com.highlight.highlight_backend.user.repository.UserRepository;
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
    private BidService bidService;
    @Autowired
    private UserService userService;


    private AtomicLong bidPrice = new AtomicLong(110000);

    @Test
    @DisplayName("동시성 테스트: 입찰과 유저 수정이 동시에 몰려도 데드락이 안 터져야 한다")
    void deadlockCheck() throws InterruptedException {
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (int i = 0; i < numberOfThreads; i++) {
            final int index = i;
            BigDecimal currentPrice = BigDecimal.valueOf(bidPrice.addAndGet(1100));
            UserUpdateRequestDto dynamicUpdateDto = new UserUpdateRequestDto(
                    "닉네임" + index,          // 닉네임0, 닉네임1... 중복 안 됨
                    "0100000000" + index,     // 전화번호도 중복 안 됨
                    true,
                    true
            );
            executorService.execute(() -> {
                try {
                    // 짝수 스레드: 입찰 시도 (Auction Lock -> User Update Event)
                    if (index % 2 == 0) {
                        bidService.createBid(new BidCreateRequestDto(2L, currentPrice, false,
                                BigDecimal.valueOf(1100)), 1L);
                    } 
                    // 홀수 스레드: 유저 정보 수정 (User Lock)
                    else {
                        userService.updateUser(1L, dynamicUpdateDto);
                    }
                    successCount.getAndIncrement();
                } catch (Exception e) {

                    if (e.getMessage().contains("입찰가")) {
                        // 이 경우는 스레드 순서 문제이므로 데드락이 아님.
                        System.out.println("스레드 순서 역전으로 인한 입찰 실패(정상): " + e.getMessage());
                        successCount.getAndIncrement(); // 쓰레드가 꼬이며 비즈니로 로직이 오류는 괜찮음.
                    } else {
                        // 진짜 데드락이나 시스템 에러만 실패로 간주
                        System.out.println("심각한 에러 발생: " + e.getMessage());
                        failCount.getAndIncrement();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드가 끝날 때까지 대기

        // 검증: 실패(데드락)가 하나도 없어야 함
        assertThat(failCount.get()).isEqualTo(0); 
        assertThat(successCount.get()).isEqualTo(numberOfThreads);
    }
}