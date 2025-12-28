package com.highlight.highlight_backend.bid.event;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.auction.repository.AuctionRepository;
import com.highlight.highlight_backend.bid.dto.BidCreateRequestDto;
import com.highlight.highlight_backend.bid.service.BidService;
import com.highlight.highlight_backend.user.domain.User;
import com.highlight.highlight_backend.user.listener.UserEventListener;
import com.highlight.highlight_backend.user.repository.UserRepository;
import com.highlight.highlight_backend.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
public class EventLossWithoutOutboxTest {

    @Autowired
    private BidService bidService;
    @Autowired private UserRepository userRepository;
    @Autowired private AuctionRepository auctionRepository;

    // ★ 이벤트를 쪼갰으니, 이제 유저 로직 담당 리스너만 Spy로 감싼다
    @Autowired
    private UserEventListener userEventListener;

    @MockitoBean
    private UserService userService;


    @Test
    @DisplayName("위험 증명: Outbox 없이 리스너가 실패하면, 입찰은 성공하지만 유저 카운트는 누락된다 (데이터 불일치)")
    void testLogicEventLoss() throws InterruptedException {
        // 1. [Given] 초기 상태
        Long userId = 1L;
        Long auctionId = 2L;
        BigDecimal bidAmount = BigDecimal.valueOf(147000); // 새로운 입찰가

        User userBefore = userRepository.findById(userId).orElseThrow();
        Long initialCount = userBefore.getParticipationCount();

        // 2. 네트워크 유실, 서버 오류를 user.count++ 로직에서 오류로 가정
        doThrow(new RuntimeException("🔥 카운트 증가하다가 오류 발생! 🔥"))
                .when(userService).increaseParticipationCount(any());

        // 3. [When] 입찰 시도 (메인 트랜잭션)
        try {
            bidService.createBid(new BidCreateRequestDto(
                    auctionId, bidAmount, false, BigDecimal.valueOf(1000)
            ), userId);
        } catch (Exception e) {
            // 메인 로직은 성공해야 하므로 에러를 먹어버림 (Async 실패는 메인에 영향 안 줌)
        }

        // 4. [Wait] 비동기 리스너가 실행되고 에러가 터질 시간을 줌
        Thread.sleep(1000);

        // 5. [Then] 결과 확인

        // (1) 입찰은 성공했는가? -> YES
        Auction auction = auctionRepository.findById(auctionId).orElseThrow();
        assertThat(auction.getCurrentHighestBid()).isEqualByComparingTo(bidAmount);
        System.out.println(">>> 입찰(돈)은 정상적으로 처리되었습니다.");

        // (2) 유저 카운트는 증가했는가? -> NO (이벤트 유실!)
        User userAfter = userRepository.findById(userId).orElseThrow();

        // Outbox가 없다면, 이 값은 영원히 initialCount와 같음 (증가 X)
        assertThat(userAfter.getParticipationCount()).isEqualTo(initialCount);
    }
}