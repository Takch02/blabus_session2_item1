package com.highlight.highlight_backend.controller;

import com.highlight.highlight_backend.common.util.ResponseUtils;
import com.highlight.highlight_backend.dto.RankingDashboardResponseDto;
import com.highlight.highlight_backend.dto.ResponseDto;
import com.highlight.highlight_backend.service.RankingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 랭킹 컨트롤러
 * 
 * 사용자의 경매 참여 횟수를 기준으로 한 랭킹 시스템의 API를 제공합니다.
 * 랭킹 대시보드 조회 기능을 통해 페이지네이션된 랭킹 데이터를 제공합니다.
 * 
 * @author 전우선
 * @since 2025.08.20
 */
@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
@Tag(name = "Ranking API", description = "사용자 랭킹 관련 API - 경매 참여 횟수 기준 랭킹 시스템")
@Slf4j
public class RankingController {
    
    private final RankingService rankingService;
    
    @GetMapping("/dashboard")
    @Operation(
        summary = "랭킹 대시보드 조회", 
        description = "경매 참여 횟수를 기준으로 사용자 랭킹을 조회합니다. " +
                     "각 사용자가 참여한 고유한 경매 수를 계산하여 내림차순으로 정렬된 랭킹을 제공합니다. " +
                     "페이지네이션을 지원하여 대량의 사용자 데이터를 효율적으로 조회할 수 있습니다."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "랭킹 대시보드 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ResponseDto.class),
                examples = @ExampleObject(
                    name = "랭킹 대시보드 응답 예시",
                    value = "{" +
                        "\"success\": true," +
                        "\"message\": \"랭킹 대시보드 조회 성공\"," +
                        "\"data\": {" +
                        "\"rankings\": [" +
                        "{\"userId\": 1, \"nickname\": \"경매왕123\", \"auctionCount\": 25, \"ranking\": 1}," +
                        "{\"userId\": 2, \"nickname\": \"입찰고수\", \"auctionCount\": 20, \"ranking\": 2}" +
                        "]," +
                        "\"totalUsers\": 150," +
                        "\"currentPage\": 1," +
                        "\"totalPages\": 15" +
                        "}" +
                        "}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "잘못된 요청 (잘못된 페이지 번호 또는 크기)"
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "서버 내부 오류"
        )
    })
    public ResponseEntity<ResponseDto<RankingDashboardResponseDto>> getRankingDashboard(
            @Parameter(
                description = "조회할 페이지 번호 (0부터 시작)", 
                example = "0",
                schema = @Schema(minimum = "0")
            )
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(
                description = "한 페이지에 표시할 랭킹 수 (최대 100개)", 
                example = "10",
                schema = @Schema(minimum = "1", maximum = "100")
            )
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("랭킹 대시보드 조회 API 호출 - page: {}, size: {}", page, size);
        
        // 페이지 크기 유효성 검사
        if (size <= 0 || size > 100) {
            log.warn("잘못된 페이지 크기 요청 - size: {}", size);
            size = Math.min(Math.max(size, 1), 100); // 1-100 사이로 제한
        }
        
        // 페이지 번호 유효성 검사
        if (page < 0) {
            log.warn("잘못된 페이지 번호 요청 - page: {}", page);
            page = 0;
        }
        
        RankingDashboardResponseDto rankingDashboard = rankingService.getUserRankingDashboard(page, size);
        
        log.info("랭킹 대시보드 조회 완료 - 반환된 랭킹 수: {}", rankingDashboard.getRankings().size());
        
        return ResponseUtils.success(rankingDashboard, "랭킹 대시보드 조회 성공");
    }
}