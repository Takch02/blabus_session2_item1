package com.highlight.highlight_backend.admin.auction;

import com.highlight.highlight_backend.admin.domain.Admin;
import com.highlight.highlight_backend.admin.repository.AdminRepository;
import com.highlight.highlight_backend.auction.application.AuctionFacade;
import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.auction.dto.AuctionScheduleRequestDto;
import com.highlight.highlight_backend.auction.dto.AuctionStartRequestDto;
import com.highlight.highlight_backend.auction.repository.AuctionRepository;
import com.highlight.highlight_backend.auction.service.AuctionCountService;
import com.highlight.highlight_backend.exception.BusinessException;
import com.highlight.highlight_backend.product.domian.Product;
import com.highlight.highlight_backend.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.highlight.highlight_backend.auction.domain.Auction.AuctionStatus.*;
import static com.highlight.highlight_backend.product.domian.Product.Category.CERAMICS;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
class AuctionFacadeRedisCountTest {

    @Autowired
    AuctionFacade auctionFacade;
    @Autowired
    AuctionCountService auctionCountService;
    @Autowired
    AuctionRepository auctionRepository;
    @Autowired
    ProductRepository productRepository;
    @Autowired
    AdminRepository adminRepository;

    private Admin admin;
    private Product product;

    @BeforeEach
    void setUp() {
        // Redis count 초기화
        for (var status : Auction.AuctionStatus.values()) {
            for (var category : Product.Category.values()) {
                auctionCountService.reset(status, category);
            }
        }

        admin = adminRepository.findById(1L).orElseThrow();
        product = createProduct();
    }

    // 1. scheduleAuction → SCHEDULED +1
    @Test
    @DisplayName("경매 예약 시 SCHEDULED count +1")
    void scheduleAuction_shouldIncrementScheduledCount() {
        long before = auctionCountService.getCount(
                SCHEDULED.name(), CERAMICS.name());

        var response = auctionFacade.scheduleAuction(
                createScheduleRequest(product.getId()), admin.getId());

        long after = auctionCountService.getCount(
                SCHEDULED.name(), CERAMICS.name());

        assertThat(after).isEqualTo(before + 1);
    }

    // 2. startAuction → SCHEDULED -1, IN_PROGRESS +1
    @Test
    @DisplayName("경매 시작 시 SCHEDULED -1, IN_PROGRESS +1")
    void startAuction_shouldTransitionCount() {
        // given: 예약된 경매 생성
        var response = auctionFacade.scheduleAuction(
                createScheduleRequest(product.getId()), admin.getId());
        Long auctionId = response.getAuctionId();

        long scheduledBefore = auctionCountService.getCount(SCHEDULED.name(), CERAMICS.name());
        long inProgressBefore = auctionCountService.getCount(IN_PROGRESS.name(), CERAMICS.name());

        // when
        auctionFacade.startAuction(auctionId, new AuctionStartRequestDto(LocalDateTime.now(), LocalDateTime.now().plusHours(1)), admin.getId());

        // then
        assertThat(auctionCountService.getCount(SCHEDULED.name(), CERAMICS.name()))
                .isEqualTo(scheduledBefore - 1);
        assertThat(auctionCountService.getCount(IN_PROGRESS.name(), CERAMICS.name()))
                .isEqualTo(inProgressBefore + 1);
    }

    // 3. cancelAuction → SCHEDULED -1
    @Test
    @DisplayName("경매 취소 시 SCHEDULED -1")
    void cancelAuction_shouldDecrementScheduledCount() {
        // 진행중 경매
        Long auctionId = createInProgressAuction();

        long inProgressBefore = auctionCountService.getCount(IN_PROGRESS.name(), CERAMICS.name());
        long cancelledBefore  = auctionCountService.getCount(CANCELLED.name(), CERAMICS.name());

        auctionFacade.cancelAuction(auctionId, admin.getId());

        assertThat(auctionCountService.getCount(IN_PROGRESS.name(), CERAMICS.name()))
                .isEqualTo(inProgressBefore - 1);
        assertThat(auctionCountService.getCount(CANCELLED.name(), CERAMICS.name()))
                .isEqualTo(cancelledBefore + 1);
    }
    
    // 4. endAuction → IN_PROGRESS -1, COMPLETED +1
    @Test
    @DisplayName("경매 종료 시 IN_PROGRESS -1, COMPLETED +1")
    void endAuction_shouldTransitionCount() {
        // 진행중 경매
        Long auctionId = createInProgressAuction();

        long inProgressBefore = auctionCountService.getCount(IN_PROGRESS.name(), CERAMICS.name());
        long completedBefore  = auctionCountService.getCount(COMPLETED.name(), CERAMICS.name());

        auctionFacade.endAuction(auctionId, admin.getId(), "테스트 종료");

        assertThat(auctionCountService.getCount(IN_PROGRESS.name(), CERAMICS.name()))
                .isEqualTo(inProgressBefore - 1);
        assertThat(auctionCountService.getCount(COMPLETED.name(), CERAMICS.name()))
                .isEqualTo(completedBefore + 1);
    }
    
    // 5. 멱등성 - 이미 종료된 경매 재종료 시 count 변화 없음
    @Test
    @DisplayName("이미 종료된 경매 재종료 시 count 변화 없음")
    void endAuction_idempotent_shouldNotChangeCount() {
        Long auctionId = createInProgressAuction();
        auctionFacade.endAuction(auctionId, admin.getId(), "첫 번째 종료");

        long inProgressAfterFirst = auctionCountService.getCount(IN_PROGRESS.name(), CERAMICS.name());
        long completedAfterFirst  = auctionCountService.getCount(COMPLETED.name(), CERAMICS.name());

        // 두 번째 종료 시도 → 예외 발생하거나 무시되어야 함
        assertThatThrownBy(() ->
            auctionFacade.endAuction(auctionId, admin.getId(), "두 번째 종료")
        ).isInstanceOf(BusinessException.class); // validator에서 막혀야 함

        // count 변화 없음
        assertThat(auctionCountService.getCount(IN_PROGRESS.name(), CERAMICS.name()))
                .isEqualTo(inProgressAfterFirst);
        assertThat(auctionCountService.getCount(COMPLETED.name(), CERAMICS.name()))
                .isEqualTo(completedAfterFirst);
    }


    /**
     * 헬퍼 메서드
     */
    private Long createInProgressAuction() {
        var response = auctionFacade.scheduleAuction(
                createScheduleRequest(product.getId()), admin.getId());

        auctionFacade.startAuction(response.getAuctionId(), new AuctionStartRequestDto(LocalDateTime.now(), LocalDateTime.now().plusHours(1)), admin.getId());
        return response.getAuctionId();
    }

    // 예시 경매 생성
    private AuctionScheduleRequestDto createScheduleRequest(Long productId) {
        AuctionScheduleRequestDto dto = new AuctionScheduleRequestDto();
        dto.setProductId(productId);
        dto.setStartPrice(BigDecimal.valueOf(10000));
        dto.setBidUnit(BigDecimal.valueOf(1000));
        dto.setMaxBid(BigDecimal.valueOf(100000));
        dto.setMinimumBid(BigDecimal.valueOf(1000));
        dto.setBuyItNowPrice(BigDecimal.valueOf(500000));
        dto.setShippingFee(BigDecimal.ZERO);
        dto.setIsPickupAvailable(false);
        dto.setScheduledStartTime(LocalDateTime.now().plusHours(1));
        dto.setScheduledEndTime(LocalDateTime.now().plusHours(3));
        dto.setDescription("테스트 경매");
        return dto;
    }

    // 예시 상품 생성
    private Product createProduct() {
        Product product = new Product();
        product.setProductName("테스트 상품");
        product.setShortDescription("테스트 상품 설명");
        product.setHistory("상품 히스토리");
        product.setExpectedEffects("기대효과");
        product.setDetailedInfo("상세정보");
        product.setCategory(CERAMICS);
        product.setProductCount(1L);
        product.setMaterial("목재");
        product.setSize("100x100");
        product.setBrand("테스트브랜드");
        product.setManufactureYear(2020);
        product.setCondition("상태 양호");
        product.setRank(Product.ProductRank.GOOD);
        product.setStatus(Product.ProductStatus.ACTIVE);
        product.setRegisteredBy(admin.getId());
        product.setSellerId(1L);
        product.setIsPremium(false);
        return productRepository.save(product);
    }
}