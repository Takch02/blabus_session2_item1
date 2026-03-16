package com.highlight.highlight_backend.bid.event;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.auction.notification.AuctionWebSocketNotifier;
import com.highlight.highlight_backend.auction.repository.AuctionRepository;
import com.highlight.highlight_backend.bid.application.BidFacade;
import com.highlight.highlight_backend.bid.dto.BidCreateRequestDto;
import com.highlight.highlight_backend.bid.listener.BidEventListener;
import com.highlight.highlight_backend.user.domain.User;
import com.highlight.highlight_backend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
public class EventSequentialTest {

    @Autowired
    private BidFacade bidFacade;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private UserRepository userRepository;

    // [핵심] 리스너를 조작하기 위해 MockBean으로 선언 (혹은 SpyBean)
    @Autowired
    private BidEventListener bidEventListener;

    @MockitoSpyBean
    private AuctionWebSocketNotifier auctionWebSocketNotifier;

    @Test
    @DisplayName("실패 시뮬레이션: 리스너가 실패(서버 다운 등)해도 입찰은 롤백되지 않아 데이터 정합성이 깨진다")
    void testEventLossConsistency() throws InterruptedException {
        // 1. [Given] 상황 세팅
        Long auctionId = 2L; // 테스트 DB에 있는 ID
        Long userId = 1L;    // 테스트 DB에 있는 ID
        Long test = 139000L;
        
        // 유저의 초기 참여 횟수 저장
        User userBefore = userRepository.findById(userId).orElseThrow();
        Long initialCount = userBefore.getParticipationCount();

        // 2. 서버가 터진 상황을 알림 서비스 오류로 가정
        doThrow(new RuntimeException("🔥 서버 폭발! 이벤트 증발! 🔥"))
                .when(auctionWebSocketNotifier).sendNewBidNotification(any());

        // 3. [When] 입찰 시도 (메인 트랜잭션)
        try {
            bidFacade.createBidFacade(new BidCreateRequestDto(
                    auctionId, BigDecimal.valueOf(test), false, BigDecimal.valueOf(1000)
            ), userId);
        } catch (Exception e) {
            System.out.println("[실패] : 비즈니스 로직 오류. " + e.getMessage());
        }

        Thread.sleep(1000);

        // 4. [Then] 검증
        
        // (1) 입찰(Bid)은 성공해서 DB에 저장되었는가? -> YES
        // (입찰가가 갱신되었는지 확인)
        Auction auction = auctionRepository.findById(auctionId).orElseThrow();
        assertThat(auction.getCurrentHighestBid())
                .isEqualByComparingTo(BigDecimal.valueOf(test));
        System.out.println(">>> 입찰은 성공해서 돈은 나갔음.");

        // (2) 유저 참여 횟수 탐색
        User userAfter = userRepository.findById(userId).orElseThrow();
        
        // (3) 알림 서버가 망가져도 user.participation_count++ 은 실행되야함.
        assertThat(userAfter.getParticipationCount()).isEqualTo(initialCount + 1);

    }
}