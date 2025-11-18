package com.highlight.highlight_backend.controller;

import com.highlight.highlight_backend.common.config.ResponseDto;
import com.highlight.highlight_backend.dto.SellerResponseDto;
import com.highlight.highlight_backend.service.SellerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 판매자 정보 조회 컨트롤러
 * 
 * 판매자 상세 정보, 목록 조회 등의 공개 API를 제공합니다.
 * 
 * @author 전우선
 * @since 2025.08.16
 */
@Slf4j
@RestController
@RequestMapping("/api/public/sellers")
@RequiredArgsConstructor
@Tag(name = "판매자 정보 조회", description = "판매자 상세 정보 및 목록 조회 API")
public class SellerController {
    
    private final SellerService sellerService;
    
    /**
     * 판매자 상세 정보 조회
     * 
     * @param sellerId 판매자 ID
     * @return 판매자 상세 정보
     */
    @GetMapping("/{sellerId}")
    @Operation(summary = "판매자 상세 정보 조회", description = "특정 판매자의 상세 정보를 조회합니다. 판매자 이름, 사업자 등록번호, 연락처, 주소 등의 정보를 포함합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "판매자 정보 조회 성공"),
        @ApiResponse(responseCode = "404", description = "판매자를 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ResponseDto<SellerResponseDto>> getSellerDetail(
            @Parameter(description = "조회할 판매자의 고유 ID", required = true, example = "1")
            @PathVariable Long sellerId) {
        log.info("GET /api/public/sellers/{} - 판매자 상세 정보 조회", sellerId);
        
        SellerResponseDto response = sellerService.getSellerDetail(sellerId);
        
        return ResponseEntity.ok(
            ResponseDto.success(response, "판매자 정보를 성공적으로 조회했습니다.")
        );
    }
    
    /**
     * 판매자 목록 조회
     * 
     * @param pageable 페이징 정보
     * @return 판매자 목록
     */
    @GetMapping
    @Operation(summary = "판매자 목록 조회", description = "활성 상태의 판매자 목록을 조회합니다.")
    public ResponseEntity<ResponseDto<Page<SellerResponseDto>>> getSellerList(
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("GET /api/public/sellers - 판매자 목록 조회");
        
        Page<SellerResponseDto> response = sellerService.getSellerList(pageable);
        
        return ResponseEntity.ok(
            ResponseDto.success(response, "판매자 목록을 성공적으로 조회했습니다.")
        );
    }
    
    /**
     * 판매자명 검색
     * 
     * @param name 판매자명
     * @param pageable 페이징 정보
     * @return 검색된 판매자 목록
     */
    @GetMapping("/search")
    @Operation(summary = "판매자명 검색", description = "판매자명으로 판매자를 검색합니다.")
    public ResponseEntity<ResponseDto<Page<SellerResponseDto>>> searchSellers(
            @RequestParam String name,
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("GET /api/public/sellers/search - 판매자명 검색: {}", name);
        
        Page<SellerResponseDto> response = sellerService.searchSellersByName(name, pageable);
        
        return ResponseEntity.ok(
            ResponseDto.success(response, "판매자 검색 결과를 성공적으로 조회했습니다.")
        );
    }
    
    /**
     * 평점 높은 판매자 조회
     * 
     * @param pageable 페이징 정보
     * @return 평점 순 판매자 목록
     */
    @GetMapping("/top-rated")
    @Operation(summary = "평점 높은 판매자", description = "평점이 높은 판매자 목록을 조회합니다.")
    public ResponseEntity<ResponseDto<Page<SellerResponseDto>>> getTopRatedSellers(
            @PageableDefault(size = 10) Pageable pageable) {
        
        log.info("GET /api/public/sellers/top-rated - 평점 높은 판매자 조회");
        
        Page<SellerResponseDto> response = sellerService.getTopRatedSellers(pageable);
        
        return ResponseEntity.ok(
            ResponseDto.success(response, "평점 높은 판매자 목록을 성공적으로 조회했습니다.")
        );
    }
    
    /**
     * 판매 실적 높은 판매자 조회
     * 
     * @param pageable 페이징 정보
     * @return 판매 건수 순 판매자 목록
     */
    @GetMapping("/top-sales")
    @Operation(summary = "판매 실적 높은 판매자", description = "판매 건수가 많은 판매자 목록을 조회합니다.")
    public ResponseEntity<ResponseDto<Page<SellerResponseDto>>> getTopSellersBySales(
            @PageableDefault(size = 10) Pageable pageable) {
        
        log.info("GET /api/public/sellers/top-sales - 판매 실적 높은 판매자 조회");
        
        Page<SellerResponseDto> response = sellerService.getTopSellersBySales(pageable);
        
        return ResponseEntity.ok(
            ResponseDto.success(response, "판매 실적 높은 판매자 목록을 성공적으로 조회했습니다.")
        );
    }
}