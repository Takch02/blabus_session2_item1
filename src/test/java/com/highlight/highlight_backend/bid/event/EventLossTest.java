package com.highlight.highlight_backend.bid.event;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.auction.repository.AuctionRepository;
import com.highlight.highlight_backend.bid.dto.BidCreateRequestDto;
import com.highlight.highlight_backend.bid.listener.BidEventListener;
import com.highlight.highlight_backend.bid.service.BidService;
import com.highlight.highlight_backend.user.domain.User;
import com.highlight.highlight_backend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
public class EventLossTest {

    @Autowired
    private BidService bidService;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private UserRepository userRepository;

    // [핵심] 리스너를 조작하기 위해 MockBean으로 선언 (혹은 SpyBean)
    @Mock
    private BidEventListener bidEventListener;

    @Test
    @DisplayName("실패 시뮬레이션: 리스너가 실패(서버 다운 등)해도 입찰은 롤백되지 않아 데이터 정합성이 깨진다")
    void testEventLossConsistency() throws InterruptedException {
        // 1. [Given] 상황 세팅
        Long auctionId = 2L; // 테스트 DB에 있는 ID
        Long userId = 1L;    // 테스트 DB에 있는 ID
        
        // 유저의 초기 참여 횟수 저장
        User userBefore = userRepository.findById(userId).orElseThrow();
        Long initialCount = userBefore.getParticipationCount();

        // 2. [Simulate Failure] 리스너가 이벤트를 받으면 '강제로 에러'를 뿜게 조작
        // "서버가 죽었다"고 가정하고 예외를 던짐
        doThrow(new RuntimeException("🔥 서버 폭발! 이벤트 증발! 🔥"))
                .when(bidEventListener).handleBidNotification(any());

        // 3. [When] 입찰 시도 (메인 트랜잭션)
        try {
            bidService.createBid(new BidCreateRequestDto(
                    auctionId, BigDecimal.valueOf(122000), false, BigDecimal.valueOf(1000)
            ), userId);
        } catch (Exception e) {
            System.out.println("[실패] : 비즈니스 로직 오류. " + e.getMessage());
        }



        // 4. [Then] 검증
        
        // (1) 입찰(Bid)은 성공해서 DB에 저장되었는가? -> YES
        // (입찰가가 갱신되었는지 확인)
        Auction auction = auctionRepository.findById(auctionId).orElseThrow();
        assertThat(auction.getCurrentHighestBid())
                .isEqualByComparingTo(BigDecimal.valueOf(122000));
        System.out.println(">>> 입찰은 성공해서 돈은 나갔음.");

        // (2) 유저 참여 횟수(User)는 증가했는가? -> NO (증발했으니까!)
        User userAfter = userRepository.findById(userId).orElseThrow();
        
        // (3) 돈은 냈는데 참여 횟수는 그대로임.
        assertThat(userAfter.getParticipationCount()).isEqualTo(initialCount);
        System.out.println(">>> 하지만 유저 기록은 갱신되지 않았음 (데이터 불일치 발생).");

    }
}