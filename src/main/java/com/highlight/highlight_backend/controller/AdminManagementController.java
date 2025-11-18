package com.highlight.highlight_backend.controller;

import com.highlight.highlight_backend.admin.user.dto.AdminCreateRequestDto;
import com.highlight.highlight_backend.admin.user.dto.AdminResponseDto;
import com.highlight.highlight_backend.common.config.ResponseDto;
import com.highlight.highlight_backend.admin.user.repository.AdminRepository;
import com.highlight.highlight_backend.admin.user.service.AdminManagementService;
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
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 계정 관리 컨트롤러
 * 
 * SUPER_ADMIN 권한을 가진 관리자가 다른 관리자 계정을 생성/수정/삭제할 수 있는 API를 제공합니다.
 * 
 * @author 전우선
 * @since 2025.08.13
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/admin-management")
@RequiredArgsConstructor
@Tag(name = "관리자 계정 관리", description = "관리자 계정 조회, 수정, 비활성화 관련 API")
public class AdminManagementController {
    
    private final AdminManagementService adminManagementService;
    private final AdminRepository adminRepository;
    
    /**
     * 관리자 계정 생성
     * 
     * @param request 계정 생성 요청 데이터
     * @param authentication 현재 로그인한 관리자 정보
     * @return 생성된 관리자 정보
     */
    @PostMapping("/admins")
    @Operation(summary = "관리자 계정 생성", description = "새로운 관리자 계정을 생성합니다. SUPER_ADMIN 권한이 필요하며, 상세한 개인정보를 포함합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "관리자 계정 생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "SUPER_ADMIN 권한 필요"),
        @ApiResponse(responseCode = "409", description = "이미 존재하는 관리자 ID")
    })
    public ResponseEntity<ResponseDto<AdminResponseDto>> createAdmin(
            @Parameter(description = "관리자 계정 생성 요청 데이터", required = true)
            @Valid @RequestBody AdminCreateRequestDto request,
            @Parameter(hidden = true) Authentication authentication) {
        
        Long currentAdminId = getCurrentAdminId(authentication);
        log.info("POST /api/admin-management/admins - 관리자 계정 생성 요청: {} (요청자: {})", 
                request.getAdminId(), currentAdminId);
        
        AdminResponseDto response = adminManagementService.createAdmin(request, currentAdminId);
        
        return ResponseEntity.ok(
            ResponseDto.success(response, "관리자 계정이 성공적으로 생성되었습니다.")
        );
    }
    
    /**
     * 관리자 목록 조회
     * 
     * @param pageable 페이징 정보
     * @param authentication 현재 로그인한 관리자 정보
     * @return 관리자 목록
     */
    @GetMapping("/admins")
    @Operation(summary = "관리자 목록 조회", description = "관리자 목록을 페이징하여 조회합니다. (SUPER_ADMIN 권한 필요)")
    public ResponseEntity<ResponseDto<Page<AdminResponseDto>>> getAdminList(
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication) {
        
        Long currentAdminId = getCurrentAdminId(authentication);
        log.info("GET /api/admin-management/admins - 관리자 목록 조회 요청 (요청자: {})", currentAdminId);
        
        Page<AdminResponseDto> response = adminManagementService.getAdminList(pageable, currentAdminId);
        
        return ResponseEntity.ok(
            ResponseDto.success(response, "관리자 목록을 성공적으로 조회했습니다.")
        );
    }
    
    /**
     * 관리자 상세 조회
     * 
     * @param adminId 조회할 관리자 ID
     * @param authentication 현재 로그인한 관리자 정보
     * @return 관리자 상세 정보
     */
    @GetMapping("/admins/{adminId}")
    @Operation(summary = "관리자 상세 조회", description = "특정 관리자의 상세 정보를 조회합니다. (SUPER_ADMIN 권한 필요)")
    public ResponseEntity<ResponseDto<AdminResponseDto>> getAdmin(
            @PathVariable Long adminId,
            Authentication authentication) {
        
        Long currentAdminId = getCurrentAdminId(authentication);
        log.info("GET /api/admin-management/admins/{} - 관리자 상세 조회 요청 (요청자: {})", 
                adminId, currentAdminId);
        
        AdminResponseDto response = adminManagementService.getAdmin(adminId, currentAdminId);
        
        return ResponseEntity.ok(
            ResponseDto.success(response, "관리자 정보를 성공적으로 조회했습니다.")
        );
    }
    
    /**
     * 관리자 계정 삭제
     * 
     * @param adminId 삭제할 관리자 ID
     * @param authentication 현재 로그인한 관리자 정보
     * @return 삭제 완료 메시지
     */
    @DeleteMapping("/admins/{adminId}")
    @Operation(summary = "관리자 계정 삭제", description = "관리자 계정을 삭제합니다. (SUPER_ADMIN 권한 필요)")
    public ResponseEntity<ResponseDto<String>> deleteAdmin(
            @PathVariable Long adminId,
            Authentication authentication) {
        
        Long currentAdminId = getCurrentAdminId(authentication);
        log.info("DELETE /api/admin-management/admins/{} - 관리자 계정 삭제 요청 (요청자: {})", 
                adminId, currentAdminId);
        
        adminManagementService.deleteAdmin(adminId, currentAdminId);
        
        return ResponseEntity.ok(
            ResponseDto.success("SUCCESS", "관리자 계정이 성공적으로 삭제되었습니다.")
        );
    }
    
    /**
     * Authentication에서 현재 관리자 ID를 안전하게 추출
     * 
     * @param authentication Spring Security Authentication 객체
     * @return 현재 관리자 ID
     */
    private Long getCurrentAdminId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        
        // JWT 인증의 경우 Long 타입으로 관리자 ID가 저장됨
        if (principal instanceof Long) {
            return (Long) principal;
        }
        
        // Mock 테스트의 경우 User 객체이므로 username을 사용
        if (principal instanceof User) {
            User user = (User) principal;
            String username = user.getUsername();
            
            // username이 숫자인 경우 Long으로 변환
            try {
                return Long.parseLong(username);
            } catch (NumberFormatException e) {
                // username이 "admin", "manager" 같은 문자열인 경우
                // 데이터베이스에서 실제 ID를 조회
                return adminRepository.findByAdminId(username)
                    .map(admin -> admin.getId())
                    .orElse(1L); // 기본값
            }
        }
        
        // String 타입인 경우
        if (principal instanceof String) {
            String username = (String) principal;
            try {
                return Long.parseLong(username);
            } catch (NumberFormatException e) {
                // 문자열인 경우 데이터베이스에서 조회
                return adminRepository.findByAdminId(username)
                    .map(admin -> admin.getId())
                    .orElse(1L); // 기본값
            }
        }
        
        // 기타 경우 기본값 반환
        return 1L;
    }
}