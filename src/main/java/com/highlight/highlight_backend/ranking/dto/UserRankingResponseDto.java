package com.highlight.highlight_backend.ranking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사용자 랭킹 정보 응답 DTO
 * 
 * 경매 참여 횟수를 기준으로 한 개별 사용자의 랭킹 정보를 담는 DTO입니다.
 * 
 * @author 전우선
 * @since 2025.08.20
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "사용자 랭킹 정보")
public class UserRankingResponseDto {
    
    /**
     * 사용자 ID
     */
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;
    
    /**
     * 사용자 닉네임
     */
    @Schema(description = "사용자 닉네임", example = "경매왕123")
    private String nickname;
    
    /**
     * 참여한 경매 수
     */
    @Schema(description = "사용자가 참여한 총 경매 수 (중복 제거)", example = "25")
    private Long auctionCount;
    
    /**
     * 현재 랭킹 순위
     */
    @Schema(description = "경매 참여 횟수 기준 랭킹 순위", example = "1")
    private Integer ranking;
    
    /**
     * UserRankingResponseDto 생성자
     * 
     * @param userId 사용자 ID
     * @param nickname 사용자 닉네임
     * @param auctionCount 참여한 경매 수
     * @param ranking 랭킹 순위
     */
    public UserRankingResponseDto(Long userId, String nickname, Long auctionCount, Integer ranking) {
        this.userId = userId;
        this.nickname = nickname;
        this.auctionCount = auctionCount;
        this.ranking = ranking;
    }
}