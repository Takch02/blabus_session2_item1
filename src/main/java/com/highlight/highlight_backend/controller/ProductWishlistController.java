package com.highlight.highlight_backend.controller;

import com.highlight.highlight_backend.dto.ProductWishlistResponseDto;
import com.highlight.highlight_backend.common.config.ResponseDto;
import com.highlight.highlight_backend.service.ProductWishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 상품 찜하기 컨트롤러
 * 
 * 사용자의 상품 찜하기, 취소, 조회 API를 제공합니다.
 * 
 * @author 전우선
 * @since 2025.08.16
 */
@Slf4j
@RestController
@RequestMapping("/api/user/wishlist")
@RequiredArgsConstructor
@Tag(name = "상품 찜하기", description = "상품 찜하기, 취소, 조회 관련 API")
public class ProductWishlistController {
    
    private final ProductWishlistService wishlistService;
    
    /**
     * 상품 찜하기/취소 토글
     * 
     * @param productId 상품 ID
     * @param authentication 현재 로그인한 사용자 정보
     * @return 찜하기 토글 결과
     */
    @PostMapping("/products/{productId}/toggle")
    @Operation(summary = "상품 찜하기 토글", description = "상품을 찜하기/취소를 토글합니다. 이미 찜한 상품이면 취소하고, 찜하지 않은 상품이면 찜합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "찜하기 토글 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음")
    })
    public ResponseEntity<ResponseDto<ProductWishlistResponseDto>> toggleWishlist(
            @Parameter(description = "찜할 상품의 고유 ID", required = true, example = "1")
            @PathVariable Long productId,
            @Parameter(hidden = true) Authentication authentication) {
        
        Long userId = (Long) authentication.getPrincipal();
        log.info("POST /api/user/wishlist/products/{}/toggle - 상품 찜하기 토글 (사용자: {})", productId, userId);
        
        ProductWishlistResponseDto response = wishlistService.toggleWishlist(userId, productId);
        
        String message = response.isWishlisted() ? "상품을 찜했습니다." : "찜을 취소했습니다.";
        
        return ResponseEntity.ok(
            ResponseDto.success(response, message)
        );
    }
    
    /**
     * 상품 찜하기 추가
     * 
     * @param productId 상품 ID
     * @param authentication 현재 로그인한 사용자 정보
     * @return 찜하기 추가 결과
     */
    @PostMapping("/products/{productId}")
    @Operation(summary = "상품 찜하기", description = "상품을 찜하기 목록에 추가합니다.")
    public ResponseEntity<ResponseDto<ProductWishlistResponseDto>> addToWishlist(
            @PathVariable Long productId,
            Authentication authentication) {
        
        Long userId = (Long) authentication.getPrincipal();
        log.info("POST /api/user/wishlist/products/{} - 상품 찜하기 추가 (사용자: {})", productId, userId);
        
        ProductWishlistResponseDto response = wishlistService.addToWishlist(userId, productId);
        
        return ResponseEntity.ok(
            ResponseDto.success(response, "상품을 찜했습니다.")
        );
    }
    
    /**
     * 상품 찜하기 취소
     * 
     * @param productId 상품 ID
     * @param authentication 현재 로그인한 사용자 정보
     * @return 찜하기 취소 결과
     */
    @DeleteMapping("/products/{productId}")
    @Operation(summary = "상품 찜하기 취소", description = "상품을 찜하기 목록에서 제거합니다.")
    public ResponseEntity<ResponseDto<String>> removeFromWishlist(
            @PathVariable Long productId,
            Authentication authentication) {
        
        Long userId = (Long) authentication.getPrincipal();
        log.info("DELETE /api/user/wishlist/products/{} - 상품 찜하기 취소 (사용자: {})", productId, userId);
        
        wishlistService.removeFromWishlist(userId, productId);
        
        return ResponseEntity.ok(
            ResponseDto.success("SUCCESS", "찜을 취소했습니다.")
        );
    }
    
    /**
     * 내 찜한 상품 목록 조회
     * 
     * @param pageable 페이징 정보
     * @param authentication 현재 로그인한 사용자 정보
     * @return 찜한 상품 목록
     */
    @GetMapping
    @Operation(summary = "내 찜한 상품 목록", description = "사용자가 찜한 상품 목록을 조회합니다.")
    public ResponseEntity<ResponseDto<Page<ProductWishlistResponseDto>>> getMyWishlist(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        
        Long userId = (Long) authentication.getPrincipal();
        log.info("GET /api/user/wishlist - 내 찜한 상품 목록 조회 (사용자: {})", userId);
        
        Page<ProductWishlistResponseDto> response = wishlistService.getUserWishlist(userId, pageable);
        
        return ResponseEntity.ok(
            ResponseDto.success(response, "찜한 상품 목록을 성공적으로 조회했습니다.")
        );
    }
    
    /**
     * 특정 상품의 찜하기 상태 조회
     * 
     * @param productId 상품 ID
     * @param authentication 현재 로그인한 사용자 정보
     * @return 찜하기 상태
     */
    @GetMapping("/products/{productId}")
    @Operation(summary = "상품 찜하기 상태 조회", description = "특정 상품의 찜하기 상태를 조회합니다.")
    public ResponseEntity<ResponseDto<ProductWishlistResponseDto>> getWishlistStatus(
            @PathVariable Long productId,
            Authentication authentication) {
        
        Long userId = (Long) authentication.getPrincipal();
        log.info("GET /api/user/wishlist/products/{} - 상품 찜하기 상태 조회 (사용자: {})", productId, userId);
        
        ProductWishlistResponseDto response = wishlistService.getWishlistStatus(userId, productId);
        
        return ResponseEntity.ok(
            ResponseDto.success(response, "찜하기 상태를 성공적으로 조회했습니다.")
        );
    }
    
    /**
     * 여러 상품의 찜하기 상태 조회
     * 
     * @param productIds 상품 ID 목록
     * @param authentication 현재 로그인한 사용자 정보
     * @return 찜한 상품 ID 목록
     */
    @PostMapping("/status")
    @Operation(summary = "여러 상품 찜하기 상태 조회", description = "여러 상품의 찜하기 상태를 한번에 조회합니다.")
    public ResponseEntity<ResponseDto<List<Long>>> getMultipleWishlistStatus(
            @RequestBody List<Long> productIds,
            Authentication authentication) {
        
        Long userId = (Long) authentication.getPrincipal();
        log.info("POST /api/user/wishlist/status - 여러 상품 찜하기 상태 조회 (사용자: {}, 상품 개수: {})", 
                userId, productIds.size());
        
        List<Long> wishlistedProductIds = wishlistService.getWishlistedProductIds(userId, productIds);
        
        return ResponseEntity.ok(
            ResponseDto.success(wishlistedProductIds, "찜하기 상태를 성공적으로 조회했습니다.")
        );
    }
    
    /**
     * 사용자가 찜한 상품 개수 조회
     * 
     * @param authentication 현재 로그인한 사용자 정보
     * @return 찜한 상품 개수
     */
    @GetMapping("/count")
    @Operation(summary = "내 찜한 상품 개수", description = "사용자가 찜한 상품의 총 개수를 조회합니다.")
    public ResponseEntity<ResponseDto<Long>> getMyWishlistCount(Authentication authentication) {
        
        Long userId = (Long) authentication.getPrincipal();
        log.info("GET /api/user/wishlist/count - 내 찜한 상품 개수 조회 (사용자: {})", userId);
        
        long count = wishlistService.getUserWishlistCount(userId);
        
        return ResponseEntity.ok(
            ResponseDto.success(count, "찜한 상품 개수를 성공적으로 조회했습니다.")
        );
    }
}