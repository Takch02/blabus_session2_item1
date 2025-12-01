package com.highlight.highlight_backend.bid;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.auction.repository.AuctionRepository;
import com.highlight.highlight_backend.bid.domain.Bid;
import com.highlight.highlight_backend.bid.dto.BidCreateRequestDto;
import com.highlight.highlight_backend.bid.dto.BidResponseDto;
import com.highlight.highlight_backend.bid.repository.BidRepository;
import com.highlight.highlight_backend.bid.service.BidNotificationService;
import com.highlight.highlight_backend.bid.service.BidService;
import com.highlight.highlight_backend.product.domian.Product;
import com.highlight.highlight_backend.user.domain.User;
import com.highlight.highlight_backend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

// 1. Mockito 확장팩을 쓴다고 선언 (이게 있어야 @Mock이 동작해)
@ExtendWith(MockitoExtension.class)
class BidCreateService {


    @Mock private UserRepository userRepository;
    @Mock private AuctionRepository auctionRepository;
    @Mock private BidRepository bidRepository;
    @Mock private BidNotificationService bidNotificationService;

    // 가짜들을 주입받을 진짜 서비스 (System Under Test)
    @InjectMocks
    private BidService bidService;

    @Test
    @DisplayName("첫 입찰 성공 시나리오: 참여 횟수 증가 및 알림 발송 확인")
    void createBid_FirstBidder_Success() {
        // --- [Given]: 상황 설정 (가짜들이 어떻게 행동할지 대본을 짜는 곳) ---

        Long userId = 1L;
        Long auctionId = 100L;
        BigDecimal bidAmount = BigDecimal.valueOf(10000);

        // 1. 데이터 객체는 Mock 쓰지 말고 진짜(Real)를 써! (제발!)
        User user = new User();
        user.setId(userId);
        user.setNickname("하루노");

        Product product = new Product();
        product.setProductName("테스트상품");

        Auction auction = new Auction(); // Builder가 없으면 이렇게라도
        auction.setId(auctionId);
        auction.setStatus(Auction.AuctionStatus.IN_PROGRESS);
        auction.setStartPrice(BigDecimal.valueOf(5000));
        auction.setMinimumBid(BigDecimal.valueOf(1000));
        auction.setProduct(product);

        BidCreateRequestDto request = new BidCreateRequestDto(auctionId, bidAmount, false, null);

        // 2. Mock 행동 정의 (Stubbing)
        // "유저 찾아줘" 하면 -> "옛다, 아까 만든 유저"
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // "경매 찾아줘(Lock)" 하면 -> "옛다, 경매"
        given(auctionRepository.findByIdWithLock(auctionId)).willReturn(Optional.of(auction));

        // "이전 1등 있어?" -> "아니, 없어 (첫 입찰이야)"
        given(bidRepository.findTopByAuctionOrderByBidAmountDesc(auction)).willReturn(Optional.empty());

        // "이 사람 신규야?" -> "응 (true)"
        given(bidRepository.existsByAuctionAndUser(auction, user)).willReturn(false);

        // "저장해줘" -> "저장된 척하고, ID 박힌 Bid 돌려줄게"
        given(bidRepository.save(any())).willAnswer(invocation -> {
            Bid bid = invocation.getArgument(0);
            // setId는 보통 protected라 테스트에선 reflection이나 setter 필요할 수 있음
            // 여기선 그냥 들어온 객체 그대로 리턴한다고 쳐
            return bid;
        });

        // --- [When]: 진짜 로직 실행 ---
        BidResponseDto response = bidService.createBid(request, userId);

        // --- [Then]: 결과 검증 ---

        // 1. 리턴값 검증
        assertThat(response.getBidAmount()).isEqualTo(bidAmount);

        // 2. ★ 상태 변화 검증 (도메인 로직이 잘 돌았나?)
        // Auction의 최고가가 갱신되었나?
        assertThat(auction.getCurrentHighestBid()).isEqualTo(bidAmount);
        assertThat(auction.getTotalBids()).isEqualTo(1);

        // User의 참여 횟수가 증가했나? (첫 입찰이니까!)
        assertThat(user.getParticipationCount()).isEqualTo(1);

        // 3. ★ 행위 검증 (Mock이 제대로 호출되었나?)
        // 알림 서비스가 호출되었는지 확인 (이게 핵심)
        verify(bidNotificationService).sendNewBidNotification(any(Bid.class));

        // DB 저장이 호출되었는지 확인
        verify(bidRepository).save(any(Bid.class));
    }
}
