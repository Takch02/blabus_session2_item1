package com.highlight.highlight_backend.bid.event;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.auction.repository.AuctionRepository;
import com.highlight.highlight_backend.bid.application.BidFacade;
import com.highlight.highlight_backend.bid.dto.BidCreateRequestDto;
import com.highlight.highlight_backend.bid.service.BidService;
import com.highlight.highlight_backend.common.outbox.OutboxEvent;
import com.highlight.highlight_backend.common.outbox.OutboxRepository;
import com.highlight.highlight_backend.common.outbox.OutboxResiliencyScheduler;
import com.highlight.highlight_backend.user.domain.User;
import com.highlight.highlight_backend.user.repository.UserRepository;
import com.highlight.highlight_backend.user.service.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
public class EventLossWithoutOutboxTest {

    @Autowired
    private BidFacade bidFacade;
    @Autowired private UserRepository userRepository;
    @Autowired private AuctionRepository auctionRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private OutboxResiliencyScheduler scheduler;

    @MockitoSpyBean
    private UserService userService;


    @Test
    @DisplayName("위험 증명: Outbox 없이 리스너가 실패하면, 입찰은 성공하지만 유저 카운트는 누락된다 (데이터 불일치)")
    void testLogicEventLoss() {
        // 1. [Given] 초기 상태
        Long userId = 1L;
        Long auctionId = 2L;
        BigDecimal bidAmount = BigDecimal.valueOf(110000); // 새로운 입찰가

        User userBefore = userRepository.findById(userId).orElseThrow();
        Long initialCount = userBefore.getParticipationCount();

        // 2. 네트워크 유실, 서버 오류를 user.count++ 로직에서 오류로 가정
        doThrow(new RuntimeException("🔥 1차 시도: 네트워크 불안정!"))
                .doCallRealMethod()// ★ 두 번째 호출부터는 성공(에러 안 던짐)
                .when(userService).increaseParticipationCount(any());

        // 3. [When] 입찰 시도 (메인 트랜잭션)
        try {
            bidFacade.createBidFacade(new BidCreateRequestDto(
                    auctionId, bidAmount, false, BigDecimal.valueOf(1000)
            ), userId);
        } catch (Exception e) {
            System.out.println("비즈니스 로직 오류 : " + e.getMessage());
        }

        OutboxEvent event = outboxRepository.findAll().get(0); // 방금 넣은거 가져옴
        // event의 시간을 6분 전이라 가정
        outboxRepository.forceUpdateCreatedAt(event.getId(), LocalDateTime.now().minusMinutes(6));
        // 스케줄러는 5분마다 실행되지만 테스트를 위해 바로 실행됐다 가정.
        scheduler.resendMissingEvents();

        // 5. [Then] 결과 확인

        // (1) 입찰은 성공했는가? -> YES
        Auction auction = auctionRepository.findById(auctionId).orElseThrow();
        assertThat(auction.getCurrentHighestBid()).isEqualByComparingTo(bidAmount);

        User userAfter = userRepository.findById(userId).orElseThrow();

        // Outbox가 없다면, 이 값은 initialCount와 같음 (증가 X)
        assertThat(userAfter.getParticipationCount()).isEqualTo(initialCount);
    }
}