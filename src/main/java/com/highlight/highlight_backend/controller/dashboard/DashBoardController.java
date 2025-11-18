package com.highlight.highlight_backend.controller.dashboard;

import com.highlight.highlight_backend.common.util.AuthenticationUtils;
import com.highlight.highlight_backend.dto.ResponseDto;
import com.highlight.highlight_backend.dto.dashboard.AdminDashBoardItemResponseDto;
import com.highlight.highlight_backend.dto.dashboard.AdminDashBoardStatsResponseDto;
import com.highlight.highlight_backend.service.AdminDashBoardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Tag(name = "관리자 대쉬보드", description = "관리자 대쉬보드 API")
public class DashBoardController {

    private final AdminDashBoardService adminDashBoardService;

    @GetMapping
    public ResponseEntity<ResponseDto<AdminDashBoardStatsResponseDto>> getDashboardStats(
            Authentication authentication
    ) {
        Long adminId = AuthenticationUtils.extractAdminId(authentication);
        AdminDashBoardStatsResponseDto dashboardStats = adminDashBoardService.getDashboardStats(adminId);
        return ResponseEntity.ok(ResponseDto.success(dashboardStats, "대쉬보드 경매 진행상황 조회에 성공했습니다."));
    }

    @GetMapping("/items")
    public ResponseEntity<ResponseDto<List<AdminDashBoardItemResponseDto>>> getDashboardItems(
            Authentication authentication
    ) {
        List<AdminDashBoardItemResponseDto> dashboardItems = adminDashBoardService.getDashboardItems();
        return ResponseEntity.ok(ResponseDto.success(dashboardItems, "대쉬보드 경매 진행 상품 조회에 성공했습니다."));
    }
}
