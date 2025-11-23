package com.highlight.highlight_backend.admin.auction.controller;

import com.highlight.highlight_backend.admin.auction.dto.AuctionResponseDto;
import com.highlight.highlight_backend.auction.service.AdminAuctionSearchService;
import com.highlight.highlight_backend.common.config.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 관리자 경매 조회 컨트롤러
 */
@RestController
@Slf4j
@RequestMapping("/api/admin/auctions")
@RequiredArgsConstructor
@Tag(name = "경매 목록 조회 (관리자)", description = "관리자 경매 목록 조회 API")
public class AdminAuctionSearchController {

    private final AdminAuctionSearchService adminAuctionSearchService;

    /**
     * 경매 목록 조회
     *
     * @param pageable 페이징 정보
     * @param authentication 현재 로그인한 관리자 정보
     * @return 경매 목록
     */
    @GetMapping
    @Operation(summary = "경매 목록 조회", description = "전체 경매 목록을 조회합니다.")
    public ResponseEntity<ResponseDto<Page<AuctionResponseDto>>> getAuctionList(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {

        Long adminId = (Long) authentication.getPrincipal();
        log.info("GET /api/admin/auctions - 경매 목록 조회 요청 (관리자: {})", adminId);

        Page<AuctionResponseDto> response = adminAuctionSearchService.getAdminAuctionList(pageable, adminId);

        return ResponseEntity.ok(
                ResponseDto.success(response, "경매 목록을 성공적으로 조회했습니다.")
        );
    }

    /**
     * 진행 중인 경매 목록 조회
     *
     * @param pageable 페이징 정보
     * @param authentication 현재 로그인한 관리자 정보
     * @return 진행 중인 경매 목록
     */
    @GetMapping("/active")
    @Operation(summary = "진행 중인 경매 목록 조회", description = "현재 진행 중인 경매 목록을 조회합니다.")
    public ResponseEntity<ResponseDto<Page<AuctionResponseDto>>> getActiveAuctions(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {

        Long adminId = (Long) authentication.getPrincipal();
        log.info("GET /api/admin/auctions/active - 진행 중인 경매 목록 조회 요청 (관리자: {})", adminId);

        Page<AuctionResponseDto> response = adminAuctionSearchService.getActiveAuctions(pageable, adminId);

        return ResponseEntity.ok(
                ResponseDto.success(response, "진행 중인 경매 목록을 성공적으로 조회했습니다.")
        );
    }

    /**
     * 경매 상세 조회
     *
     * @param auctionId 조회할 경매 ID
     * @param authentication 현재 로그인한 관리자 정보
     * @return 경매 상세 정보
     */
    @GetMapping("/{auctionId}")
    @Operation(summary = "경매 상세 조회", description = "특정 경매의 상세 정보를 조회합니다.")
    public ResponseEntity<ResponseDto<AuctionResponseDto>> getAuction(
            @PathVariable Long auctionId,
            Authentication authentication) {

        Long adminId = (Long) authentication.getPrincipal();
        log.info("GET /api/admin/auctions/{} - 경매 상세 조회 요청 (관리자: {})",
                auctionId, adminId);

        AuctionResponseDto response = adminAuctionSearchService.getAuction(auctionId, adminId);

        return ResponseEntity.ok(
                ResponseDto.success(response, "경매 정보를 성공적으로 조회했습니다.")
        );
    }
}
