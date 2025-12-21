package com.highlight.highlight_backend.product.controller;

import com.highlight.highlight_backend.common.config.ResponseDto;
import com.highlight.highlight_backend.product.dto.ProductResponseDto;
import com.highlight.highlight_backend.product.service.UserProductSearchService;
import com.highlight.highlight_backend.product.dto.UserAuctionDetailResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 메인 페이지, 상제 페이지, 카테고리 페이지의 내용을 담당하는 컨트롤러
 */

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/public/products")
@Tag(name = "경매 목록 조회", description = "경매 목록 검색, 필터링, 상세조회 API")
public class ProductSearchController {

    private final UserProductSearchService userProductSearchService;


    /**
     * 경매 상세 조회
     *
     * @param auctionId -> auction ID 를 받아서 조회
     * @return -> UserAuctionDetailResponseDto 를 반환
     */
    @GetMapping("/{auctionId}")
    @Operation(
            summary = "경매 상세 정보 조회",
            description = "특정 경매의 상세 정보를 조회합니다. 상품 정보, 현재 입찰 현황, 판매자 정보, 이미지 등 모든 상세 정보를 포함합니다. 로그인 없이 접근 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "경매 상세 정보 조회 성공",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            ),
            @ApiResponse(responseCode = "404", description = "경매를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ResponseDto<UserAuctionDetailResponseDto>> getAuctionDetail(
            @Parameter(description = "조회할 경매의 고유 ID", required = true, example = "1")
            @PathVariable("auctionId") Long auctionId
    ) {
        log.info("GET /api/public/{} - 경매 목록 조회 요청 (비로그인 사용자도 접근 가능)", auctionId);

        // 1. 경매 상세 정보 조회
        UserAuctionDetailResponseDto response = userProductSearchService.getProductsDetail(auctionId);

        return ResponseEntity.ok(ResponseDto.success(response, "경매 상세 목록을 성공적으로 불러왔습니다"));
    }


    /**
     * 관련 상품 추천 조회 (공개 API)
     *
    **/
    @GetMapping("/{productId}/recommendations")
    @Operation(
            summary = "관련 상품 추천",
            description = "특정 상품과 관련된 추천 상품 목록을 조회합니다. 동일 카테고리나 브랜드의 상품을 우선적으로 추천합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "추천 상품 조회 성공",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            ),
            @ApiResponse(responseCode = "404", description = "기준 상품을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ResponseDto<Page<ProductResponseDto>>> getRecommendedProducts(
            @Parameter(description = "추천 기준이 되는 상품의 고유 ID", required = true, example = "1")
            @PathVariable Long productId,
            @Parameter(description = "추천 상품 개수", example = "4")
            @RequestParam(defaultValue = "4") int size) {

        log.info("GET /api/products/{}/recommendations - 관련 상품 추천 조회", productId);

        Page<ProductResponseDto> response = userProductSearchService.getRecommendedProducts(productId, size);

        return ResponseEntity.ok(
                ResponseDto.success(response, "관련 상품을 성공적으로 조회했습니다.")
        );
    }

}
