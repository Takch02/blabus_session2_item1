package com.highlight.highlight_backend.auction.controller;

import com.highlight.highlight_backend.auction.dto.BuyItNowRequestDto;
import com.highlight.highlight_backend.auction.dto.BuyItNowResponseDto;
import com.highlight.highlight_backend.common.config.ResponseDto;
import com.highlight.highlight_backend.auction.service.AdminAuctionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequestMapping("/api/user/auctions")
@RequiredArgsConstructor
@Tag(name = "즉시구매", description = "경매 상품 즉시구매 API")
public class UserAuctionController {
    
    private final AdminAuctionService adminAuctionService;
    
    /**
     * 즉시구매
     *
     * @param request 즉시구매 요청 데이터
     * @param authentication 현재 로그인한 사용자 정보
     * @return 즉시구매 완료 정보
     */
    @PostMapping("/{auctionId}/buy-it-now")
    @Operation(summary = "즉시구매", 
               description = "설정된 즉시구매가로 상품을 즉시 구매합니다. 재고 1개 상품만 가능하며, 경매가 즉시 종료됩니다. 결제 정보를 포함해야 합니다.")
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