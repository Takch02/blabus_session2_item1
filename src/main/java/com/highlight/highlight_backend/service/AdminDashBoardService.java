package com.highlight.highlight_backend.service;

import com.highlight.highlight_backend.admin.user.domain.Admin;
import com.highlight.highlight_backend.admin.auction.domain.Auction;
import com.highlight.highlight_backend.domain.ProductImage;
import com.highlight.highlight_backend.dto.dashboard.AdminDashBoardItemResponseDto;
import com.highlight.highlight_backend.dto.dashboard.AdminDashBoardStatsResponseDto;
import com.highlight.highlight_backend.exception.AdminErrorCode;
import com.highlight.highlight_backend.exception.BusinessException;
import com.highlight.highlight_backend.admin.user.repository.AdminRepository;
import com.highlight.highlight_backend.admin.auction.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashBoardService {

    private final AdminRepository adminRepository;
    private final AuctionRepository auctionRepository;

    public AdminDashBoardStatsResponseDto getDashboardStats(Long adminId) {
        // 1. Admin 엔티티 조회 (기존과 동일)
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(AdminErrorCode.ADMIN_NOT_FOUND));

        // 2. 해당 Admin이 관리하는 모든 Auction 리스트 조회
        List<Auction> auctions = auctionRepository.findByAdminAuction(adminId);

        Map<Auction.AuctionStatus, Long> statusCounts = auctions.stream()
                .collect(Collectors.groupingBy(Auction::getStatus, Collectors.counting()));

        // 4. 계산된 개수를 DTO에 직접 설정
        //    getOrDefault를 사용하면 해당 상태의 경매가 없을 경우 0을 기본값으로 사용합니다.
        long pendingCount = statusCounts.getOrDefault(Auction.AuctionStatus.SCHEDULED, 0L);
        long inProgressCount = statusCounts.getOrDefault(Auction.AuctionStatus.IN_PROGRESS, 0L);
        long completedCount = statusCounts.getOrDefault(Auction.AuctionStatus.COMPLETED, 0L);

        // 5. DTO 객체를 생성하고 계산된 값들을 설정
        AdminDashBoardStatsResponseDto responseDto = new AdminDashBoardStatsResponseDto();
        responseDto.setPending(pendingCount);
        responseDto.setInProgress(inProgressCount);
        responseDto.setCompleted(completedCount);

        // 6. 최종 DTO 반환
        return responseDto;
    }

    public List<AdminDashBoardItemResponseDto> getDashboardItems() {
        List<Auction> inProgressAuctions = auctionRepository.findByStatus(Auction.AuctionStatus.IN_PROGRESS, PageRequest.of(0, 3)).getContent();

        return inProgressAuctions.stream()
                .map(auction -> {
                    AdminDashBoardItemResponseDto dto = new AdminDashBoardItemResponseDto();
                    dto.setAuctionId(auction.getId());
                    dto.setProductName(auction.getProduct().getProductName());
                    dto.setCurrentBid(auction.getCurrentHighestBid());

                    ProductImage primaryImage = auction.getProduct().getPrimaryImage();
                    if (primaryImage != null) {
                        dto.setProductImageUrl(primaryImage.getImageUrl());
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }
}
