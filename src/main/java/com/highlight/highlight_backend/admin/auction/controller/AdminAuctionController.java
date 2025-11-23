package com.highlight.highlight_backend.admin.auction.controller;

import com.highlight.highlight_backend.admin.auction.dto.AuctionEndRequestDto;
import com.highlight.highlight_backend.admin.auction.dto.AuctionResponseDto;
import com.highlight.highlight_backend.admin.auction.dto.AuctionScheduleRequestDto;
import com.highlight.highlight_backend.admin.auction.dto.AuctionStartRequestDto;
import com.highlight.highlight_backend.admin.auction.dto.AuctionUpdateRequestDto;
import com.highlight.highlight_backend.common.config.ResponseDto;
import com.highlight.highlight_backend.auction.service.AdminAuctionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 경매 관리 컨트롤러
 * 
 * 경매 예약, 시작, 종료, 중단 API를 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/auctions")
@RequiredArgsConstructor
@Tag(name = "경매 관리 (관리자)", description = "경매 예약, 시작, 종료, 중단 관련 관리자 API")
public class AdminAuctionController {
    
    private final AdminAuctionService adminAuctionService;
    
    /**
     * 경매 예약
     * 
     * @param request 경매 예약 요청 데이터
     * @param authentication 현재 로그인한 관리자 정보
     * @return 예약된 경매 정보
     */
    @PostMapping("/schedule")
    @Operation(
        summary = "경매 예약", 
        description = "상품을 경매에 예약합니다. 경매 시작/종료 시간, 시작가, 입찰 단위, 즉시 구매가 등을 설정할 수 있습니다. 경매 진행 시간은 최소 10분 이상이어야 합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "경매 예약 성공",
            content = @Content(schema = @Schema(implementation = ResponseDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 (시간, 가격 등)"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 부족"),
        @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음"),
        @ApiResponse(responseCode = "409", description = "이미 경매에 등록된 상품"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ResponseDto<AuctionResponseDto>> scheduleAuction(
            @Parameter(description = "경매 예약 요청 데이터", required = true)
            @Valid @RequestBody AuctionScheduleRequestDto request,
            Authentication authentication) {
        
        Long adminId = (Long) authentication.getPrincipal();
        log.info("POST /api/admin/auctions/schedule - 경매 예약 요청: 상품 {} (관리자: {})",
                request.getProductId(), adminId);
        
        AuctionResponseDto response = adminAuctionService.scheduleAuction(request, adminId);
        
        return ResponseEntity.ok(
            ResponseDto.success(response, "경매가 성공적으로 예약되었습니다.")
        );
    }
    
    /**
     * 경매 시작
     * 
     * @param auctionId 시작할 경매 ID
     * @param request 경매 시작 요청 데이터
     * @param authentication 현재 로그인한 관리자 정보
     * @return 시작된 경매 정보
     */
    @PostMapping("/{auctionId}/start")
    @Operation(
        summary = "경매 시작", 
        description = "예약된 경매를 시작합니다. 즉시 시작하거나 특정 시간으로 설정하여 시작할 수 있습니다. 시작 후에는 사용자들이 입찰할 수 있습니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "경매 시작 성공",
            content = @Content(schema = @Schema(implementation = ResponseDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "이미 진행 중이거나 종료된 경매"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 부족"),
        @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ResponseDto<AuctionResponseDto>> startAuction(
            @Parameter(description = "시작할 경매의 고유 ID", required = true, example = "1")
            @PathVariable Long auctionId,
            @Parameter(description = "경매 시작 요청 데이터", required = true)
            @Valid @RequestBody AuctionStartRequestDto request,
            Authentication authentication) {
        
        Long adminId = (Long) authentication.getPrincipal();
        log.info("POST /api/admin/auctions/{}/start - 경매 시작 요청 (관리자: {}, 즉시시작: {})",
                auctionId, adminId, request.isImmediateStart());
        
        AuctionResponseDto response = adminAuctionService.startAuction(auctionId, request, adminId);
        
        return ResponseEntity.ok(
            ResponseDto.success(response, "경매가 성공적으로 시작되었습니다.")
        );
    }
    
    /**
     * 경매 즉시 시작 (간편 API)
     * 
     * @param auctionId 시작할 경매 ID
     * @param authentication 현재 로그인한 관리자 정보
     * @return 시작된 경매 정보
     */
    @PostMapping("/{auctionId}/start-now")
    @Operation(
        summary = "경매 즉시 시작", 
        description = "버튼 클릭으로 경매를 즉시 시작합니다. 현재 시간부터 시작되며 기존 종료 시간을 유지합니다. 간편 시작 API입니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "경매 즉시 시작 성공",
            content = @Content(schema = @Schema(implementation = ResponseDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "이미 진행 중이거나 종료된 경매"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 부족"),
        @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ResponseDto<AuctionResponseDto>> startAuctionNow(
            @Parameter(description = "즉시 시작할 경매의 고유 ID", required = true, example = "1")
            @PathVariable Long auctionId,
            Authentication authentication) {
        
        Long adminId = (Long) authentication.getPrincipal();
        log.info("POST /api/admin/auctions/{}/start-now - 경매 즉시 시작 요청 (관리자: {})",
                auctionId, adminId);

        // immediateStart는 기본값이 false이므로 별도 설정 필요
        AuctionResponseDto response = adminAuctionService.startAuction(auctionId,
            new AuctionStartRequestDto() {{ setImmediateStart(true); }}, adminId);
        
        return ResponseEntity.ok(
            ResponseDto.success(response, "경매가 즉시 시작되었습니다.")
        );
    }
    
    /**
     * 경매 정상 종료
     * 
     * @param auctionId 종료할 경매 ID
     * @param request 경매 종료 요청 데이터
     * @param authentication 현재 로그인한 관리자 정보
     * @return 종료된 경매 정보
     */
    @PostMapping("/{auctionId}/end")
    @Operation(summary = "경매 종료", 
               description = "진행 중인 경매를 종료합니다. 정상 종료 또는 중단으로 처리할 수 있습니다.")
    public ResponseEntity<ResponseDto<AuctionResponseDto>> endAuction(
            @PathVariable Long auctionId,
            @Valid @RequestBody AuctionEndRequestDto request,
            Authentication authentication) {
        
        Long adminId = (Long) authentication.getPrincipal();
        log.info("POST /api/auctions/{}/end - 경매 종료 요청 (관리자: {}, 중단: {})", 
                auctionId, adminId, request.isCancel());

        String endReason = request.getEndReason();
        AuctionResponseDto response = adminAuctionService.endAuction(auctionId, adminId, endReason);
        
        String message = request.isCancel() ? "경매가 중단되었습니다." : "경매가 종료되었습니다.";
        
        return ResponseEntity.ok(
            ResponseDto.success(response, message)
        );
    }
    
    /**
     * 경매 즉시 종료
     * 
     * @param auctionId 종료할 경매 ID
     * @param authentication 현재 로그인한 관리자 정보
     * @return 종료된 경매 정보
     */
    @PostMapping("/{auctionId}/end-now")
    @Operation(summary = "경매 즉시 종료", 
               description = "버튼 클릭으로 경매를 즉시 정상 종료합니다.")
    public ResponseEntity<ResponseDto<AuctionResponseDto>> endAuctionNow(
            @PathVariable Long auctionId,
            Authentication authentication) {
        
        Long adminId = (Long) authentication.getPrincipal();
        log.info("POST /api/admin/auctions/{}/end-now - 경매 즉시 종료 요청 (관리자: {})",
                auctionId, adminId);
        
        // 즉시 정상 종료 요청 생성
        AuctionEndRequestDto request = new AuctionEndRequestDto();
        request.setImmediateEnd(true);
        request.setEndReason("관리자 즉시 종료");
        
        AuctionResponseDto response = adminAuctionService.endAuctionImmediately(auctionId, adminId);
        
        return ResponseEntity.ok(
            ResponseDto.success(response, "경매가 즉시 종료되었습니다.")
        );
    }
    
    /**
     * 경매 즉시 중단
     * 
     * @param auctionId 중단할 경매 ID
     * @param authentication 현재 로그인한 관리자 정보
     * @return 중단된 경매 정보
     */
    @PostMapping("/{auctionId}/cancel-now")
    @Operation(summary = "경매 즉시 중단", 
               description = "버튼 클릭으로 경매를 즉시 중단합니다.")
    public ResponseEntity<ResponseDto<AuctionResponseDto>> cancelAuctionNow(
            @PathVariable Long auctionId,
            Authentication authentication) {
        
        Long adminId = (Long) authentication.getPrincipal();
        log.info("POST /api/admin/auctions/{}/cancel-now - 경매 즉시 중단 요청 (관리자: {})",
                auctionId, adminId);
        
        // 즉시 중단 요청 생성
        AuctionEndRequestDto request = new AuctionEndRequestDto();
        request.setImmediateEnd(true);
        request.setCancel(true);
        request.setEndReason("관리자 즉시 중단");
        
        AuctionResponseDto response = adminAuctionService.cancelAuction(auctionId, adminId);
        
        return ResponseEntity.ok(
            ResponseDto.success(response, "경매가 즉시 중단되었습니다.")
        );
    }


    /**
     * 경매 수정
     * 
     * @param auctionId 수정할 경매 ID
     * @param request 경매 수정 요청 데이터
     * @param authentication 현재 로그인한 관리자 정보
     * @return 수정된 경매 정보
     */
    @PutMapping("/{auctionId}")
    @Operation(
        summary = "경매 수정", 
        description = "예약된 경매의 정보를 수정합니다. 진행 중이거나 종료된 경매는 수정할 수 없습니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "경매 수정 성공",
            content = @Content(schema = @Schema(implementation = ResponseDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 (시간, 가격 등)"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 부족"),
        @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없음"),
        @ApiResponse(responseCode = "409", description = "수정할 수 없는 경매 상태"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ResponseDto<AuctionResponseDto>> updateAuction(
            @Parameter(description = "수정할 경매의 고유 ID", required = true, example = "1")
            @PathVariable Long auctionId,
            @Parameter(description = "경매 수정 요청 데이터", required = true)
            @Valid @RequestBody AuctionUpdateRequestDto request,
            Authentication authentication) {
        
        Long adminId = (Long) authentication.getPrincipal();
        log.info("PUT /api/admin/auctions/{} - 경매 수정 요청 (관리자: {})",
                auctionId, adminId);
        
        AuctionResponseDto response = adminAuctionService.updateAuction(auctionId, request, adminId);
        
        return ResponseEntity.ok(
            ResponseDto.success(response, "경매가 성공적으로 수정되었습니다.")
        );
    }

}