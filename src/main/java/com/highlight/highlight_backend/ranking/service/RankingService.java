package com.highlight.highlight_backend.ranking.service;

import com.highlight.highlight_backend.ranking.dto.RankingDashboardResponseDto;
import com.highlight.highlight_backend.ranking.dto.UserRankingResponseDto;
import com.highlight.highlight_backend.bid.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 랭킹 서비스
 * 
 * 사용자의 경매 참여 횟수를 기준으로 한 랭킹 시스템을 제공합니다.
 * 각 사용자가 참여한 고유한 경매 수를 계산하여 랭킹을 매깁니다.
 * 
 * @author 전우선
 * @since 2025.08.20
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RankingService {
    
    private final BidRepository bidRepository;
    
    /**
     * 사용자 랭킹 대시보드 조회
     * 
     * 경매 참여 횟수를 기준으로 사용자 랭킹을 조회합니다.
     * 각 사용자가 참여한 고유한 경매 수를 계산하여 내림차순으로 정렬합니다.
     * 
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 랭킹 대시보드 데이터 (랭킹 목록, 페이지네이션 정보 포함)
     */
    public RankingDashboardResponseDto getUserRankingDashboard(int page, int size) {
        log.info("랭킹 대시보드 조회 요청 - page: {}, size: {}", page, size);
        
        // 페이지네이션 설정
        Pageable pageable = PageRequest.of(page, size);
        
        // 랭킹 데이터 조회
        List<Object[]> rankingResults = bidRepository.findUserRankingByAuctionParticipation(pageable);
        
        // 총 사용자 수 조회
        Long totalUsers = bidRepository.countDistinctUsers();
        
        log.info("랭킹 데이터 조회 완료 - 조회된 랭킹 수: {}, 총 사용자 수: {}", 
                rankingResults.size(), totalUsers);
        
        // 랭킹 DTO 목록 생성
        List<UserRankingResponseDto> rankings = new ArrayList<>();
        
        for (int i = 0; i < rankingResults.size(); i++) {
            Object[] result = rankingResults.get(i);
            
            // 데이터베이스 결과를 DTO로 변환
            Long userId = ((Number) result[0]).longValue();
            String nickname = (String) result[1];
            Long auctionCount = ((Number) result[2]).longValue();
            
            // 랭킹 계산 (페이지 오프셋 + 현재 인덱스 + 1)
            Integer ranking = (page * size) + i + 1;
            
            rankings.add(new UserRankingResponseDto(userId, nickname, auctionCount, ranking));
        }
        
        // 총 페이지 수 계산
        int totalPages = (int) Math.ceil((double) totalUsers / size);
        
        log.info("랭킹 대시보드 생성 완료 - 총 페이지: {}, 현재 페이지: {}", 
                totalPages, page + 1);
        
        return new RankingDashboardResponseDto(rankings, totalUsers.intValue(), page + 1, totalPages);
    }
}