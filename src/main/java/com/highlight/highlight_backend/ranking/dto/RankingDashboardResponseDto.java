package com.highlight.highlight_backend.ranking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 랭킹 대시보드 응답 DTO
 * 
 * 경매 참여 횟수 기준 사용자 랭킹 대시보드 데이터를 담는 DTO입니다.
 * 페이지네이션 정보와 함께 랭킹 목록을 제공합니다.
 * 
 * @author 전우선
 * @since 2025.08.20
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "랭킹 대시보드 응답 데이터")
public class RankingDashboardResponseDto {
    
    /**
     * 사용자 랭킹 목록
     */
    @Schema(description = "경매 참여 횟수 기준 사용자 랭킹 목록")
    private List<UserRankingResponseDto> rankings;
    
    /**
     * 총 사용자 수
     */
    @Schema(description = "경매에 참여한 총 사용자 수", example = "150")
    private Integer totalUsers;
    
    /**
     * 현재 페이지 번호
     */
    @Schema(description = "현재 페이지 번호 (1부터 시작)", example = "1")
    private Integer currentPage;
    
    /**
     * 총 페이지 수
     */
    @Schema(description = "총 페이지 수", example = "15")
    private Integer totalPages;
    
    /**
     * RankingDashboardResponseDto 생성자
     * 
     * @param rankings 사용자 랭킹 목록
     * @param totalUsers 총 사용자 수
     * @param currentPage 현재 페이지 번호
     * @param totalPages 총 페이지 수
     */
    public RankingDashboardResponseDto(List<UserRankingResponseDto> rankings, Integer totalUsers, Integer currentPage, Integer totalPages) {
        this.rankings = rankings;
        this.totalUsers = totalUsers;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
    }
}