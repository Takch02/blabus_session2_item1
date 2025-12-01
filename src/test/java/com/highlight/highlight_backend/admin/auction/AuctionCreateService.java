package com.highlight.highlight_backend.admin.auction;

import com.highlight.highlight_backend.auction.dto.AuctionResponseDto;
import com.highlight.highlight_backend.auction.dto.AuctionScheduleRequestDto;
import com.highlight.highlight_backend.auction.service.AdminAuctionSearchService;
import com.highlight.highlight_backend.auction.service.AdminAuctionService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@SpringBootTest
public class AuctionCreateService {
    @Autowired
    private AdminAuctionService adminAuctionService;
    @Autowired
    private AdminAuctionSearchService AdminAuctionSearchService;

    /**
     * 상품 id를 이용해 경매 생성 및 조회 테스트
     */
    @Test
    void createAuction() {
        AuctionScheduleRequestDto auctionScheduleRequestDto = createAuctionScheduleRequestDto();

        AuctionResponseDto request = adminAuctionService.scheduleAuction(auctionScheduleRequestDto, 1L);
        AuctionResponseDto dto = AdminAuctionSearchService.getAuction(request.getAuctionId(), 1L);

        Assertions.assertNotNull(auctionScheduleRequestDto);
        assertThat(dto.getAuctionId()).isEqualTo(request.getAuctionId());
        assertThat(dto.getStartPrice()).isEqualTo(auctionScheduleRequestDto.getStartPrice());
        assertThat(dto.getDescription()).isEqualTo("상태 매우 좋은 테스트 상품입니다. 네고 사절.");
        System.out.println(dto.getScheduledStartTime());
        System.out.println(dto.getScheduledEndTime());
    }

    /**
     * 경매 dto 생성 예시
     */
    AuctionScheduleRequestDto createAuctionScheduleRequestDto() {
        // 1. 객체 생성
        AuctionScheduleRequestDto requestDto = new AuctionScheduleRequestDto();

        // 2. 필수값 설정 (NotNull)
        requestDto.setProductId(6L); // 상품 ID

        // 금액 관련은 정밀도를 위해 문자열 생성자 사용 권장
        requestDto.setStartPrice(new BigDecimal("10000"));   // 시작가: 10,000원
        requestDto.setBidUnit(new BigDecimal("1000"));       // 입찰 단위: 1,000원
        requestDto.setMaxBid(new BigDecimal("50000"));       // 최대 인상폭: 50,000원
        requestDto.setMinimumBid(new BigDecimal("1000"));    // 최소 인상폭: 1,000원 (조건: 1000원 이상)

        requestDto.setIsPickupAvailable(true); // 직접 픽업 가능 여부

        // 시간 설정 (현재 시간 기준 미래로 설정)
        LocalDateTime now = LocalDateTime.now();
        requestDto.setScheduledStartTime(now.plusDays(1));      // 내일 시작  -- 시간 또 잘못나오는듯
        requestDto.setScheduledEndTime(now.plusDays(1).plusHours(4)); // 내일 시작 후 4시간 뒤 종료

        // 3. 선택값 설정 (Nullable 혹은 기본값이 있는 경우)
        requestDto.setBuyItNowPrice(new BigDecimal("200000")); // 즉시 구매가
        requestDto.setShippingFee(new BigDecimal("3000"));     // 배송비
        requestDto.setDescription("상태 매우 좋은 테스트 상품입니다. 네고 사절.");

        return requestDto;
    }

    @Test
    void searchAuction() {
        Page<AuctionResponseDto> pages = AdminAuctionSearchService.getAdminAuctionList(Pageable.ofSize(3), 1L);

        for (AuctionResponseDto auctionResponseDto : pages.getContent()) {
            System.out.println(auctionResponseDto);
        }
    }
}
