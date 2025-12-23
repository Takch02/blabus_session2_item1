package com.highlight.highlight_backend.auction.controller;

import com.highlight.highlight_backend.auction.dto.BuyItNowRequestDto;
import com.highlight.highlight_backend.auction.dto.BuyItNowResponseDto;
import com.highlight.highlight_backend.auction.service.UserAuctionSearchService;
import com.highlight.highlight_backend.common.config.ResponseDto;
import com.highlight.highlight_backend.auction.service.AdminAuctionService;
import com.highlight.highlight_backend.auction.dto.AuctionSearchConditionDto;
import com.highlight.highlight_backend.product.dto.UserAuctionResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 경매 참여 컨트롤러
 * 
 * 사용자가 경매에 참여하는 기능(입찰, 즉시구매 등)을 제공합니다.
 *
 */
@Slf4j
@RestController
@RequestMapping("/api/public/auctions")
@RequiredArgsConstructor
@Tag(name = "유저 경매 검색", description = "유저 경매 검색 API")
public class UserAuctionController {
    
    private final AdminAuctionService adminAuctionService;
    private final UserAuctionSearchService userAuctionSearchService;


    /**
     * 메인 화면 및 카테고리 화면
     *
     */
    @GetMapping("/")
    @Operation(
            summary = "경매 목록 조회 및 검색",
            description = "모든 경매 목록을 필터링과 정렬 조건에 따라 조회합니다. 로그인 없이 접근 가능한 공개 API입니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "경매 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 검색 파라미터"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ResponseDto<Slice<UserAuctionResponseDto>>> home (
            @Parameter(description = "카테고리 필터 (PROPS, FURNITURE, HOME_APPLIANCES, SCULPTURE, FASHION, CERAMICS, PAINTING)")
            @RequestParam(required = false) String category,
            @Parameter(description = "최소 가격 (원)")
            @RequestParam(required = false) Long minPrice,
            @Parameter(description = "최대 가격 (원)")
            @RequestParam(required = false) Long maxPrice,
            @Parameter(description = "브랜드명")
            @RequestParam(required = false) String brand,
            @Parameter(description = "프리미엄 상품 필터 (true: 프리미엄만, false: 일반만, null: 전체)")
            @RequestParam(required = false) Boolean isPremium,
            @Parameter(description = "경매 상태 (IN_PROGRESS: 진행중, SCHEDULED: 예정, ENDING_SOON: 마감임박)", example = "IN_PROGRESS")
            @RequestParam(required = false) String status,
            @Parameter(description = "페이징 정보 (page, size, sort)")
            Pageable pageable) {

        log.info("GET /api/public/auctions - 경매 목록 조회 요청 (비로그인 사용자도 접근 가능)");
        AuctionSearchConditionDto auctionSearchConditionDto = new AuctionSearchConditionDto(category, brand,
                 minPrice, maxPrice, isPremium, status);
        Slice<UserAuctionResponseDto> response = userAuctionSearchService.getProductsFiltered(
                auctionSearchConditionDto, pageable);

        return ResponseEntity.ok(
                ResponseDto.success(response, "경매 목록을 성공적으로 불러왔습니다."));
    }




    /**
     * 즉시구매
     *
     * @param request 즉시구매 요청 데이터
     * @param authentication 현재 로그인한 사용자 정보
     * @return 즉시구매 완료 정보
     */

    @PostMapping("/{auctionId}/buy-it-now")
    @Operation(summary = "즉시구매", 
               description = "설정된 즉시구매가로 상품을 즉시 구매합니다. 재고 1개 상품만 가능하다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "즉시구매 성공"),
        @ApiResponse(responseCode = "400", description = "즉시구매 불가 상품 또는 재고 부족"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없음"),
        @ApiResponse(responseCode = "409", description = "이미 종료된 경매")
    })
    public ResponseEntity<ResponseDto<BuyItNowResponseDto>> buyItNow(
            @Parameter(description = "즉시구매 요청 데이터 (결제 정보 포함)", required = true)
            @Valid @RequestBody BuyItNowRequestDto request,
            @Parameter(hidden = true) Authentication authentication) {
        
        Long userId = (Long) authentication.getPrincipal();
        log.info("POST /api/user/auctions/buy-it-now - 즉시구매 요청 (사용자: {})",
                 userId);
        
        BuyItNowResponseDto response = adminAuctionService.buyItNow(request, userId);
        
        return ResponseEntity.ok(
            ResponseDto.success(response, "즉시구매가 완료되었습니다.")
        );
    }
}