package com.highlight.highlight_backend.bid;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.auction.notification.AuctionWebSocketNotifier;
import com.highlight.highlight_backend.auction.repository.AuctionRepository;
import com.highlight.highlight_backend.bid.application.BidFacade;
import com.highlight.highlight_backend.bid.domain.Bid;
import com.highlight.highlight_backend.bid.dto.BidCreateRequestDto;
import com.highlight.highlight_backend.bid.dto.BidResponseDto;
import com.highlight.highlight_backend.bid.repository.BidRepository;
import com.highlight.highlight_backend.bid.service.BidNotificationService;
import com.highlight.highlight_backend.bid.service.BidService;
import com.highlight.highlight_backend.common.outbox.OutboxService;
import com.highlight.highlight_backend.product.domian.Product;
import com.highlight.highlight_backend.user.domain.User;
import com.highlight.highlight_backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BidCreateService {


    @InjectMocks
    private BidService bidService;

    @Mock private BidRepository bidRepository;

    @Mock
    private OutboxService outboxService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private User testUser;
    private Auction testAuction;
    private BidCreateRequestDto requestDto;

    @BeforeEach
    void setUp() {
        // 테스트에 사용할 공통 데이터 세팅 (Given)
        testUser = createDefaultUser();
        testAuction = createDefaultAuction(1L, BigDecimal.valueOf(20000));
        requestDto = new BidCreateRequestDto(1L, BigDecimal.valueOf(30000), false, BigDecimal.valueOf(1000)); // 100번 경매에 5만원 입찰
    }

    @Test
    @DisplayName("최초 입찰: 이전 입찰자가 없을 경우 정상적으로 입찰이 생성된다.")
    void createBid_FirstBid() {
        // 1. 준비 (Given)
        // 이전 최고 입찰자가 없다고 가정 (Optional.empty 반환)
        when(bidRepository.findTopByAuctionOrderByBidAmountDesc(testAuction))
                .thenReturn(Optional.empty());

        // 이 유저는 경매에 처음 참여한다고 가정
        when(bidRepository.existsByAuctionAndUser(testAuction, testUser))
                .thenReturn(false);

        // 저장소에 저장될 가짜 입찰 객체 생성 및 반환 설정
        Bid savedBid = Bid.builder().id(10L).bidAmount(BigDecimal.valueOf(30000)).user(testUser).auction(testAuction).build();
        when(bidRepository.save(any(Bid.class))).thenReturn(savedBid);

        // 2. 실행 (When)
        BidResponseDto response = bidService.createBid(requestDto, testUser, testAuction);

        // 3. 검증 (Then)
        assertThat(response).isNotNull();
        assertThat(response.getBidId()).isEqualTo(10L); // 저장된 ID가 잘 반환되었는지 확인
        assertThat(response.getBidAmount()).isEqualTo(BigDecimal.valueOf(30000)); // 금액 확인
        assertThat(response.getProductName()).isEqualTo("테스트용 맥북 프로");

        // 데이터베이스 조회 및 저장 메서드가 각각 1번씩 호출되었는지 행위 검증
        verify(bidRepository, times(1)).findTopByAuctionOrderByBidAmountDesc(testAuction);
        verify(bidRepository, times(1)).save(any(Bid.class));
    }

    @Test
    @DisplayName("기존 입찰 갱신: 이전 입찰자가 있을 경우, 이전 입찰은 유찰 처리되고 새 입찰이 생성된다.")
    void createBid_UpdateExistingBid() {
        // 1. 준비 (Given)
        // 기존 최고 입찰자 생성 (금액: 4만원)
        User previousUser = prevUser();
        Bid previousBid = Bid.builder().id(9L).bidAmount(BigDecimal.valueOf(40000)).user(previousUser).auction(testAuction).build();

        // 이전 최고 입찰자가 있다고 가정 (previousBid 반환)
        when(bidRepository.findTopByAuctionOrderByBidAmountDesc(testAuction))
                .thenReturn(Optional.of(previousBid));

        // 현재 유저는 경매에 처음 참여한다고 가정
        when(bidRepository.existsByAuctionAndUser(testAuction, testUser))
                .thenReturn(false);

        // 새롭게 저장될 가짜 입찰 객체 설정 (금액: 5만원)
        Bid savedBid = Bid.builder().id(10L).bidAmount(BigDecimal.valueOf(50000)).user(testUser).auction(testAuction).build();
        when(bidRepository.save(any(Bid.class))).thenReturn(savedBid);

        // 2. 실행 (When)
        BidResponseDto response = bidService.createBid(requestDto, testUser, testAuction);

        // 3. 검증 (Then)
        // 응답 객체 검증
        assertThat(response).isNotNull();
        assertThat(response.getBidId()).isEqualTo(10L);
        assertThat(response.getBidAmount()).isEqualTo(BigDecimal.valueOf(50000));

        // ★ 핵심 검증: 기존 입찰 객체의 outBid()가 호출되어 상태가 변경되었는지 확인 ★
        // (Bid 엔티티 내부에 상태를 나타내는 필드가 Status.OUTBID 라고 가정)
        assertThat(previousBid.getStatus()).isEqualTo(Bid.BidStatus.OUTBID);

        // 저장 메서드가 1번 호출되었는지 검증
        verify(bidRepository, times(1)).save(any(Bid.class));
    }

    private User createDefaultUser() {
        User user = new User();
        user.setId(1L);
        user.setUserId("test");
        user.setPassword("test");
        user.setNickname("testUser");
        return user;
    }

    private User prevUser() {
        User user = new User();
        user.setId(2L);
        user.setUserId("test2");
        user.setPassword("test2");
        user.setNickname("testUser2");
        return user;
    }

    private Auction createDefaultAuction(Long auctionId, BigDecimal highestBid) {
        Auction auction = new Auction();
        auction.setId(auctionId);
        auction.setStatus(Auction.AuctionStatus.IN_PROGRESS); // 경매 진행 중 상태라고 가정
        auction.setStartPrice(BigDecimal.valueOf(10000)); // 시작가 1만 원
        auction.setCurrentHighestBid(highestBid); // 파라미터로 받은 현재 최고가
        auction.setMinimumBid(BigDecimal.valueOf(1000)); // 최소 인상폭 1천 원
        auction.setMaxBid(BigDecimal.valueOf(50000)); // 최대 인상폭 5만 원

        Product dummyProduct = new Product();
        dummyProduct.setProductName("테스트용 맥북 프로");

        auction.setProduct(dummyProduct);
        return auction;
    }
}
