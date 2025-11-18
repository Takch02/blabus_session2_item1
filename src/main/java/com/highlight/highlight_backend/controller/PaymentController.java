package com.highlight.highlight_backend.controller;

import com.highlight.highlight_backend.dto.BuyItNowRequestDto;
import com.highlight.highlight_backend.dto.BuyItNowResponseDto;
import com.highlight.highlight_backend.dto.PaymentPreviewDto;
import com.highlight.highlight_backend.dto.PaymentResponseDto;
import com.highlight.highlight_backend.common.config.ResponseDto;
import com.highlight.highlight_backend.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 결제 컨트롤러
 * 
 * 경매 낙찰 후 결제 관련 API를 처리합니다.
 * 
 * @author 탁찬홍
 * @since 2025.08.21
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "결제 API", description = "경매 낙찰 후 결제 관련 API")
public class PaymentController {
    
    private final PaymentService paymentService;
    
    /**
     * 결제 미리보기 조회
     * 
     * @param auctionId 경매 ID
     * @return 결제 미리보기 정보
     */
    @GetMapping("/preview/{auctionId}")
    @Operation(
        summary = "결제 미리보기 조회", 
        description = "경매 낙찰 후 결제 전 미리보기 정보를 조회합니다. 낙찰가, 보유 포인트, 실제 결제 금액 등을 확인할 수 있습니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "결제 미리보기 조회 성공",
            content = @Content(schema = @Schema(implementation = PaymentPreviewDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음 (낙찰자가 아님)"),
        @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없음")
    })
    public ResponseEntity<ResponseDto<PaymentPreviewDto>> getPaymentPreview(
            @Parameter(description = "경매 ID", required = true, example = "1")
            @PathVariable Long auctionId,
            Authentication authentication) {
        
        Long currentUserId = getCurrentUserId(authentication);
        log.info("결제 미리보기 조회 요청: 경매ID={}, 사용자ID={}", auctionId, currentUserId);
        
        PaymentPreviewDto preview = paymentService.getPaymentPreview(auctionId, currentUserId);
        
        log.info("결제 미리보기 조회 완료: 경매ID={}, 낙찰가={}, 보유포인트={}, 실제결제={}", 
                auctionId, preview.getWinningBidAmount(), preview.getUserPoint(), preview.getActualPaymentAmount());
        
        return ResponseEntity.ok(ResponseDto.success(preview));
    }
    

    
    /**
     * 결제 처리 (포인트 자동 사용)
     * 
     * @param auctionId 경매 ID
     * @return 결제 결과
     */
    @PostMapping("/process/{auctionId}")
    @Operation(
        summary = "결제 처리 (포인트 자동 사용)", 
        description = "보유한 포인트를 자동으로 최대한 사용하여 결제를 처리합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "결제 처리 성공",
            content = @Content(schema = @Schema(implementation = PaymentResponseDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음 (낙찰자가 아님)"),
        @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없음"),
        @ApiResponse(responseCode = "409", description = "이미 결제 완료된 경매")
    })
    public ResponseEntity<ResponseDto<PaymentResponseDto>> processPayment(
            @Parameter(description = "경매 ID", required = true, example = "1")
            @PathVariable Long auctionId,
            Authentication authentication) {
        
        Long currentUserId = getCurrentUserId(authentication);
        log.info("일반 낙찰 결제 처리 요청 (포인트 자동 사용): 경매ID={}, 사용자ID={}", auctionId, currentUserId);
        
        PaymentResponseDto result = paymentService.processPayment(auctionId, currentUserId);
        
        log.info("일반 낙찰 결제 처리 완료: 경매ID={}, 결제ID={}, 사용포인트={}, 실제결제={}", 
                auctionId, result.getPaymentId(), result.getUsedPointAmount(), result.getActualPaymentAmount());
        
        return ResponseEntity.ok(ResponseDto.success(result));
    }
    /**
     * 즉시 구매 처리 (경매 종료만)
     *
     * @param request 즉시 구매 요청
     * @param authentication 인증 정보
     * @return 즉시 구매 결과
     */
    @PostMapping("/buy-it-now")
    @Operation(
            summary = "즉시 구매 (경매 종료)",
            description = "경매 상품을 즉시 구매하여 경매를 종료합니다. 실제 결제는 별도 API에서 처리됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "즉시 구매 성공",
                    content = @Content(schema = @Schema(implementation = BuyItNowResponseDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (포인트 부족, 잘못된 금액 등)"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "즉시 구매 불가능한 경매")
    })
    public ResponseEntity<ResponseDto<BuyItNowResponseDto>> processBuyItNow(
            @Parameter(description = "즉시 구매 요청 정보", required = true)
            @RequestBody BuyItNowRequestDto request,
            Authentication authentication) {

        Long currentUserId = getCurrentUserId(authentication);
        log.info("즉시 구매 요청: 경매ID={}, 사용자ID={}, 사용포인트={}",
                request.getAuctionId(), currentUserId, request.getUsePointAmount());

        BuyItNowResponseDto result = paymentService.processBuyItNow(request, currentUserId);

        log.info("즉시 구매 완료: 경매ID={}, 즉시구매가={}, 사용포인트={}, 실제결제={}",
                request.getAuctionId(), result.getBuyItNowPrice(), result.getUsedPointAmount(), result.getActualPaymentAmount());

        return ResponseEntity.ok(ResponseDto.success(result));
    }
    /**
     * Authentication에서 현재 사용자 ID를 추출
     * 
     * @param authentication 인증 정보
     * @return 사용자 ID
     */
    private Long getCurrentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
