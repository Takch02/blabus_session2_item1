package com.highlight.highlight_backend.controller;

import com.highlight.highlight_backend.dto.ProductNotificationRequestDto;
import com.highlight.highlight_backend.dto.ProductNotificationResponseDto;
import com.highlight.highlight_backend.common.config.ResponseDto;
import com.highlight.highlight_backend.service.ProductNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 상품 알림 설정 컨트롤러
 * 
 * 사용자의 상품 알림 설정, 조회, 관리 API를 제공합니다.
 * 
 * @author 전우선
 * @since 2025.08.16
 */
@Slf4j
@RestController
@RequestMapping("/api/user/notifications")
@RequiredArgsConstructor
@Tag(name = "상품 알림 관리", description = "상품 재입고 및 경매 시작 알림 설정 API")
public class ProductNotificationController {
    
    private final ProductNotificationService notificationService;
    
    /**
     * 상품 알림 설정/해제
     * 
     * @param productId 상품 ID
     * @param requestDto 알림 설정 요청 데이터
     * @param authentication 현재 로그인한 사용자 정보
     * @return 알림 설정 결과
     */
    @PostMapping("/products/{productId}")
    @Operation(summary = "상품 알림 설정", description = "특정 상품에 대한 알림을 설정하거나 해제합니다. 경매 시작, 마감 임박 등의 알림을 받을 수 있습니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "알림 설정 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음")
    })
    public ResponseEntity<ResponseDto<ProductNotificationResponseDto>> setProductNotification(
            @Parameter(description = "알림 설정할 상품의 고유 ID", required = true, example = "1")
            @PathVariable Long productId,
            @Parameter(description = "알림 설정 요청 데이터", required = true)
            @Valid @RequestBody ProductNotificationRequestDto requestDto,
            @Parameter(hidden = true) Authentication authentication) {
        
        Long userId = (Long) authentication.getPrincipal();
        log.info("POST /api/user/notifications/products/{} - 상품 알림 설정 (사용자: {})", productId, userId);
        
        ProductNotificationResponseDto response = notificationService.setProductNotification(userId, productId, requestDto);
        
        String message = requestDto.getIsActive() ? "알림이 설정되었습니다." : "알림이 해제되었습니다.";
        
        return ResponseEntity.ok(
            ResponseDto.success(response, message)
        );
    }
    
    /**
     * 상품 알림 토글 (간편 설정)
     * 
     * @param productId 상품 ID
     * @param authentication 현재 로그인한 사용자 정보
     * @return 토글된 알림 설정
     */
    @PostMapping("/products/{productId}/toggle")
    @Operation(summary = "상품 알림 토글", description = "특정 상품의 알림을 켜기/끄기 토글합니다.")
    public ResponseEntity<ResponseDto<ProductNotificationResponseDto>> toggleProductNotification(
            @PathVariable Long productId,
            Authentication authentication) {
        
        Long userId = (Long) authentication.getPrincipal();
        log.info("POST /api/user/notifications/products/{}/toggle - 상품 알림 토글 (사용자: {})", productId, userId);
        
        ProductNotificationResponseDto response = notificationService.toggleProductNotification(userId, productId);
        
        String message = response.isActive() ? "알림이 설정되었습니다." : "알림이 해제되었습니다.";
        
        return ResponseEntity.ok(
            ResponseDto.success(response, message)
        );
    }
    
    /**
     * 내 알림 목록 조회
     * 
     * @param pageable 페이징 정보
     * @param authentication 현재 로그인한 사용자 정보
     * @return 사용자의 알림 목록
     */
    @GetMapping
    @Operation(summary = "내 알림 목록 조회", description = "사용자가 설정한 모든 알림 목록을 조회합니다.")
    public ResponseEntity<ResponseDto<Page<ProductNotificationResponseDto>>> getMyNotifications(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        
        Long userId = (Long) authentication.getPrincipal();
        log.info("GET /api/user/notifications - 내 알림 목록 조회 (사용자: {})", userId);
        
        Page<ProductNotificationResponseDto> response = notificationService.getUserNotifications(userId, pageable);
        
        return ResponseEntity.ok(
            ResponseDto.success(response, "알림 목록을 성공적으로 조회했습니다.")
        );
    }
    
    /**
     * 내 활성 알림 목록 조회
     * 
     * @param pageable 페이징 정보
     * @param authentication 현재 로그인한 사용자 정보
     * @return 사용자의 활성 알림 목록
     */
    @GetMapping("/active")
    @Operation(summary = "내 활성 알림 목록 조회", description = "사용자가 설정한 활성 알림 목록만 조회합니다.")
    public ResponseEntity<ResponseDto<Page<ProductNotificationResponseDto>>> getMyActiveNotifications(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        
        Long userId = (Long) authentication.getPrincipal();
        log.info("GET /api/user/notifications/active - 내 활성 알림 목록 조회 (사용자: {})", userId);
        
        Page<ProductNotificationResponseDto> response = notificationService.getUserActiveNotifications(userId, pageable);
        
        return ResponseEntity.ok(
            ResponseDto.success(response, "활성 알림 목록을 성공적으로 조회했습니다.")
        );
    }
    
    /**
     * 특정 상품의 알림 설정 상태 조회
     * 
     * @param productId 상품 ID
     * @param authentication 현재 로그인한 사용자 정보
     * @return 알림 설정 상태
     */
    @GetMapping("/products/{productId}")
    @Operation(summary = "상품 알림 설정 상태 조회", description = "특정 상품에 대한 현재 알림 설정 상태를 조회합니다.")
    public ResponseEntity<ResponseDto<ProductNotificationResponseDto>> getProductNotificationStatus(
            @PathVariable Long productId,
            Authentication authentication) {
        
        Long userId = (Long) authentication.getPrincipal();
        log.info("GET /api/user/notifications/products/{} - 상품 알림 설정 상태 조회 (사용자: {})", productId, userId);
        
        ProductNotificationResponseDto response = notificationService.getProductNotificationStatus(userId, productId);
        
        return ResponseEntity.ok(
            ResponseDto.success(response, "알림 설정 상태를 성공적으로 조회했습니다.")
        );
    }
    
    /**
     * 상품 알림 삭제
     * 
     * @param productId 상품 ID
     * @param authentication 현재 로그인한 사용자 정보
     * @return 삭제 완료 메시지
     */
    @DeleteMapping("/products/{productId}")
    @Operation(summary = "상품 알림 삭제", description = "특정 상품의 알림 설정을 완전히 삭제합니다.")
    public ResponseEntity<ResponseDto<String>> deleteProductNotification(
            @PathVariable Long productId,
            Authentication authentication) {
        
        Long userId = (Long) authentication.getPrincipal();
        log.info("DELETE /api/user/notifications/products/{} - 상품 알림 삭제 (사용자: {})", productId, userId);
        
        notificationService.deleteProductNotification(userId, productId);
        
        return ResponseEntity.ok(
            ResponseDto.success("SUCCESS", "알림 설정이 삭제되었습니다.")
        );
    }
}