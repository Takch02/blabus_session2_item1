package com.highlight.highlight_backend.admin.auction.service;

import com.highlight.highlight_backend.admin.auction.dto.AuctionResponseDto;
import com.highlight.highlight_backend.admin.auction.repository.AuctionRepository;
import com.highlight.highlight_backend.admin.validator.CommonValidator;
import com.highlight.highlight_backend.auction.domain.Auction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAuctionSearchService {

    private final AuctionRepository auctionRepository;

    private final CommonValidator commonValidator;

    /**
     * 경매 목록 조회
     *
     * @param pageable 페이징 정보
     * @param adminId 조회하는 관리자 ID
     * @return 경매 목록
     */
    public Page<AuctionResponseDto> getAdminAuctionList(Pageable pageable, Long adminId) {
        log.info("경매 목록 조회 요청 (관리자: {})", adminId);

        commonValidator.validateManagePermission(adminId);

        return auctionRepository.findByCreatedByOrderByCreatedAtDesc(adminId, pageable)
                .map(AuctionResponseDto::from);
    }

    /**
     * 경매 상세 조회
     *
     * @param auctionId 조회할 경매 ID
     * @param adminId 조회하는 관리자 ID
     * @return 경매 상세 정보
     */
    public AuctionResponseDto getAuction(Long auctionId, Long adminId) {
        log.info("경매 상세 조회 요청: {} (관리자: {})", auctionId, adminId);

        commonValidator.validateManagePermission(adminId);

        Auction auction = auctionRepository.getOrThrow(auctionId);

        return AuctionResponseDto.from(auction);
    }

    /**
     * 관리자의 전체 경매 목록 조회
     *
     *
     * @param pageable 페이징 정보
     * @param adminId 조회하는 관리자 ID
     * @return 진행 중인 경매 목록
     */
    public Page<AuctionResponseDto> getActiveAuctions(Pageable pageable, Long adminId) {
        log.info("관리자 전체 경매 목록 조회 요청 (관리자: {})", adminId);

        commonValidator.validateManagePermission(adminId);

        return auctionRepository.findByCreatedByOrderByCreatedAtDesc(adminId, pageable)
                .map(AuctionResponseDto::from);
    }

}
