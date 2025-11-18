package com.highlight.highlight_backend.controller;

import com.highlight.highlight_backend.common.util.AuthenticationUtils;
import com.highlight.highlight_backend.common.util.ResponseUtils;
import com.highlight.highlight_backend.dto.AuctionStatusResponseDto;
import com.highlight.highlight_backend.dto.BidCreateRequestDto;
import com.highlight.highlight_backend.dto.BidResponseDto;
import com.highlight.highlight_backend.dto.ResponseDto;
import com.highlight.highlight_backend.dto.WinBidDetailResponseDto;
import com.highlight.highlight_backend.dto.AuctionMyResultResponseDto;
import com.highlight.highlight_backend.service.BidService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

/**
 * 입찰 관련 컨트롤러
 * 
 * 경매 입찰 참여, 입찰 내역 조회, 실시간 경매 상태 조회 API를 제공합니다.
 * 
 * @author 전우선
 * @since 2025.08.15
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "입찰 및 경매 상태", description = "입찰 참여, 입찰 내역 조회, 경매 상태 조회, 낙찰 내역 API")
public class BidController {
    
    // 정렬 필드 매핑 (DTO 필드명 -> 엔티티 필드명)
    private static final Map<String, String> SORT_FIELD_MAPPING = new HashMap<>();
    static {
        SORT_FIELD_MAPPING.put("bidTime", "createdAt");
        SORT_FIELD_MAPPING.put("bidAmount", "bidAmount");
        SORT_FIELD_MAPPING.put("createdAt", "createdAt");
    }
    
    private final BidService bidService;
    
    /**
     * 정렬 필드 매핑 처리
     * DTO 필드명을 엔티티 필드명으로 변환합니다.
     * 
     * @param pageable 원본 Pageable 객체
     * @return 매핑된 Pageable 객체
     */
    private Pageable mapSortFields(Pageable pageable) {
        if (pageable.getSort().isEmpty()) {
            return pageable;
        }
        
        Sort mappedSort = Sort.unsorted();
        boolean hasMappedFields = false;
        
        for (Sort.Order order : pageable.getSort()) {
            String property = order.getProperty();
            String mappedProperty = SORT_FIELD_MAPPING.getOrDefault(property, property);
            
            Sort.Direction direction = order.getDirection();
            mappedSort = mappedSort.and(Sort.by(direction, mappedProperty));
            hasMappedFields = true;
        }
        
        if (hasMappedFields) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), mappedSort);
        }
        
        return pageable;
    }
    
    /**
     * 입찰 참여
     * 
     * @param request 입찰 요청 정보
     * @param authentication 현재 로그인한 사용자 정보
     * @return 입찰 결과
     */
    @PostMapping("/bids")
    @Operation(
        summary = "입찰 참여", 
        description = "경매에 입찰을 참여합니다. 입찰 금액은 현재 최고가보다 높아야 하며, 입찰 단위의 배수여야 합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "입찰 성공",
            content = @Content(schema = @Schema(implementation = ResponseDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "최소 입찰 금액 미달 또는 경매 종료"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없음"),
        @ApiResponse(responseCode = "409", description = "이미 더 높은 입찰가 존재"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ResponseDto<BidResponseDto>> createBid(
            @Parameter(description = "입찰 요청 정보 (경매 ID, 입찰 금액)", required = true)
            @Valid @RequestBody BidCreateRequestDto request,
            Authentication authentication) {
        
        Long userId = AuthenticationUtils.extractUserId(authentication);
        log.info("POST /api/bids - 입찰 참여 요청 (사용자: {}, 경매: {}, 금액: {})", 
                userId, request.getAuctionId(), request.getBidAmount());
        
        BidResponseDto response = bidService.createBid(request, userId);
        
        return ResponseUtils.success(response, "입찰에 성공했습니다.");
    }
    
    /**
     * 경매 입찰 내역 조회 (익명 처리) - 사용자별 최신 입찰만 표시
     * 
     * @param auctionId 경매 ID
     * @param pageable 페이징 정보
     * @return 입찰 내역 목록 (사용자별 최신 입찰)
     */
    @GetMapping("/auctions/{auctionId}/bids")
    @Operation(
        summary = "경매 입찰 내역 조회 (익명)", 
        description = "특정 경매의 입찰 내역을 익명으로 조회합니다. 각 사용자의 최신 입찰만 표시되며, 입찰자 정보는 마스킹되어 표시됩니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "입찰 내역 조회 성공",
            content = @Content(schema = @Schema(implementation = ResponseDto.class))
        ),
        @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ResponseDto<Page<BidResponseDto>>> getAuctionBids(
            @Parameter(description = "입찰 내역을 조회할 경매의 고유 ID", required = true, example = "1")
            @PathVariable Long auctionId,
            @Parameter(description = "페이징 정보 (기본 20개)")
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("GET /api/auctions/{}/bids - 경매 입찰 내역 조회 (익명)", auctionId);
        
        // 정렬 필드 매핑 처리 (bidTime -> createdAt)
        Pageable mappedPageable = mapSortFields(pageable);
        
        Page<BidResponseDto> response = bidService.getAuctionBids(auctionId, mappedPageable);
        
        return ResponseUtils.success(response, "입찰 내역 조회가 완료되었습니다.");
    }
    
    /**
     * 경매 입찰 내역 조회 (본인 입찰 강조) - 사용자별 최신 입찰만 표시
     * 
     * @param auctionId 경매 ID
     * @param pageable 페이징 정보
     * @param authentication 현재 로그인한 사용자 정보
     * @return 입찰 내역 목록 (사용자별 최신 입찰, 본인 입찰 강조)
     */
    @GetMapping("/auctions/{auctionId}/bids/with-user")
    @Operation(summary = "경매 입찰 내역 조회 (본인 강조)", description = "특정 경매의 입찰 내역을 조회하며 본인 입찰을 강조합니다. 각 사용자의 최신 입찰만 표시됩니다.")
    public ResponseEntity<ResponseDto<Page<BidResponseDto>>> getAuctionBidsWithUser(
            @Parameter(description = "경매 ID", required = true)
            @PathVariable Long auctionId,
            @Parameter(description = "페이징 정보")
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        
        Long userId = AuthenticationUtils.extractUserId(authentication);
        log.info("GET /api/auctions/{}/bids/with-user - 경매 입찰 내역 조회 (본인 강조, 사용자: {})", auctionId, userId);
        
        // 정렬 필드 매핑 처리 (bidTime -> createdAt)
        Pageable mappedPageable = mapSortFields(pageable);
        
        Page<BidResponseDto> response = bidService.getAuctionBidsWithUser(auctionId, userId, mappedPageable);
        
        return ResponseUtils.success(response, "입찰 내역 조회가 완료되었습니다.");
    }
    
    /**
     * 경매 전체 입찰 내역 조회 (관리자용)
     * 
     * @param auctionId 경매 ID
     * @param pageable 페이징 정보
     * @return 모든 입찰 내역 목록
     */
    @GetMapping("/admin/auctions/{auctionId}/bids/all")
    @Operation(
        summary = "경매 전체 입찰 내역 조회 (관리자)", 
        description = "특정 경매의 모든 입찰 내역을 조회합니다. 관리자용으로 사용자별 중복 입찰도 모두 표시됩니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "전체 입찰 내역 조회 성공",
            content = @Content(schema = @Schema(implementation = ResponseDto.class))
        ),
        @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ResponseDto<Page<BidResponseDto>>> getAllAuctionBids(
            @Parameter(description = "입찰 내역을 조회할 경매의 고유 ID", required = true, example = "1")
            @PathVariable Long auctionId,
            @Parameter(description = "페이징 정보 (기본 20개)")
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("GET /admin/auctions/{}/bids/all - 경매 전체 입찰 내역 조회 (관리자)", auctionId);
        
        // 정렬 필드 매핑 처리
        Pageable mappedPageable = mapSortFields(pageable);
        
        Page<BidResponseDto> response = bidService.getAllAuctionBids(auctionId, mappedPageable);
        
        return ResponseUtils.success(response, "전체 입찰 내역 조회가 완료되었습니다.");
    }
    
    /**
     * 실시간 경매 상태 조회
     * 
     * @param auctionId 경매 ID
     * @return 경매 상태 정보
     */
    @GetMapping("/auctions/{auctionId}/status")
    @Operation(summary = "실시간 경매 상태 조회", description = "경매의 실시간 상태 정보를 조회합니다.")
    public ResponseEntity<ResponseDto<AuctionStatusResponseDto>> getAuctionStatus(
            @Parameter(description = "경매 ID", required = true)
            @PathVariable Long auctionId) {
        
        log.info("GET /api/auctions/{}/status - 실시간 경매 상태 조회", auctionId);
        
        AuctionStatusResponseDto response = bidService.getAuctionStatus(auctionId);
        
        return ResponseUtils.success(response, "경매 상태 조회가 완료되었습니다.");
    }
    
    /**
     * 내 입찰 내역 조회
     * 
     * @param authentication 현재 로그인한 사용자 정보
     * @param pageable 페이징 정보
     * @return 사용자 입찰 내역
     */
    @GetMapping("/users/bids")
    @Operation(summary = "내 입찰 내역 조회", description = "로그인한 사용자의 입찰 내역을 조회합니다.")
    public ResponseEntity<ResponseDto<Page<BidResponseDto>>> getUserBids(
            Authentication authentication,
            @Parameter(description = "페이징 정보")
            @PageableDefault(size = 20) Pageable pageable) {
        
        Long userId = AuthenticationUtils.extractUserId(authentication);
        log.info("GET /api/users/bids - 내 입찰 내역 조회 (사용자: {})", userId);
        
        Page<BidResponseDto> response = bidService.getUserBids(userId, pageable);
        
        return ResponseUtils.success(response, "내 입찰 내역 조회가 완료되었습니다.");
    }
    
    /**
     * 내 낙찰 내역 조회
     * 
     * @param authentication 현재 로그인한 사용자 정보
     * @param pageable 페이징 정보
     * @return 낙찰 내역
     */
    @GetMapping("/users/wins")
    @Operation(summary = "내 낙찰 내역 조회", description = "로그인한 사용자의 낙찰 내역을 조회합니다.")
    public ResponseEntity<ResponseDto<Page<BidResponseDto>>> getUserWonBids(
            Authentication authentication,
            @Parameter(description = "페이징 정보")
            @PageableDefault(size = 20) Pageable pageable) {
        
        Long userId = AuthenticationUtils.extractUserId(authentication);
        log.info("GET /api/users/wins - 내 낙찰 내역 조회 (사용자: {})", userId);
        
        Page<BidResponseDto> response = bidService.getUserWonBids(userId, pageable);
        
        return ResponseUtils.success(response, "내 낙찰 내역 조회가 완료되었습니다.");
    }
    
    /**
     * 낙찰 상세 정보 조회
     * 
     * @param bidId 입찰 ID
     * @param authentication 현재 로그인한 사용자 정보
     * @return 낙찰 상세 정보
     */
    @GetMapping("/users/wins/{bidId}")
    @Operation(summary = "낙찰 상세 정보 조회", description = "특정 낙찰의 상세 정보를 조회합니다.")
    public ResponseEntity<ResponseDto<WinBidDetailResponseDto>> getWinBidDetail(
            @Parameter(description = "입찰 ID", required = true)
            @PathVariable Long bidId,
            Authentication authentication) {
        
        Long userId = AuthenticationUtils.extractUserId(authentication);
        log.info("GET /api/users/wins/{} - 낙찰 상세 정보 조회 (사용자: {})", bidId, userId);
        
        WinBidDetailResponseDto response = bidService.getWinBidDetail(bidId, userId);
        
        return ResponseUtils.success(response, "낙찰 상세 정보 조회가 완료되었습니다.");
    }
    
    /**
     * 경매에서 내 결과 조회
     * 
     * @param auctionId 경매 ID
     * @param authentication 현재 로그인한 사용자 정보
     * @return 경매 내 결과 정보
     */
    @GetMapping("/auctions/{auctionId}/my-result")
    @Operation(summary = "경매에서 내 결과 조회", description = "특정 경매에서 사용자의 최종 결과를 조회합니다. (낙찰/유찰/취소/미참여)")
    public ResponseEntity<ResponseDto<AuctionMyResultResponseDto>> getMyAuctionResult(
            @Parameter(description = "경매 ID", required = true)
            @PathVariable Long auctionId,
            Authentication authentication) {
        
        Long userId = AuthenticationUtils.extractUserId(authentication);
        log.info("GET /api/auctions/{}/my-result - 경매 내 결과 조회 (사용자: {})", auctionId, userId);
        
        AuctionMyResultResponseDto response = bidService.getMyAuctionResult(auctionId, userId);
        
        return ResponseUtils.success(response, "경매 결과 조회가 완료되었습니다.");
    }
}