package com.highlight.highlight_backend.auction.repository;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.exception.AuctionErrorCode;
import com.highlight.highlight_backend.exception.BusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 경매 repository
 *
 * JPA 메소드를 이용한 repo 입니다.
 */
public interface AuctionRepository extends JpaRepository<Auction, Long>, JpaSpecificationExecutor<Auction> {

    default Auction getOrThrow(Long id) {
        return findById(id)
                .orElseThrow(() -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));
    }

    /**
     * 상품이 이미 경매에 등록되어 있는지 확인
     *
     * @param productId 상품 ID
     * @return 경매 등록 여부
     */
    boolean existsByProductId(Long productId);


    List<Auction> findByStatusAndScheduledStartTimeBefore(Auction.AuctionStatus status, LocalDateTime time);

    /**
     * 경매 상태로 경매 목록 조회 (리스트 형태)
     *
     * @param status 경매 상태
     * @return 해당 상태의 경매 목록
     */
    List<Auction> findByStatus(Auction.AuctionStatus status);

    /**
     * 특정 시간 범위 내 종료 예정인 진행 중 경매 조회
     *
     * @param status 경매 상태
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 해당 범위의 경매 목록
     */
    List<Auction> findByStatusAndScheduledEndTimeBetween(
            Auction.AuctionStatus status,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    /**
     * 경매 상태로 조회
     *
     * @param status 경매 상태
     * @param pageable 페이징 정보
     * @return 해당 상태의 경매 목록
     */
    Page<Auction> findByStatus(Auction.AuctionStatus status, Pageable pageable);

    /**
     * 관리자가 생성한 경매 목록 조회
     *
     * @param createdBy 관리자 ID
     * @param pageable 페이징 정보
     * @return 해당 관리자가 생성한 경매 목록
     */
    Page<Auction> findByCreatedByOrderByCreatedAtDesc(Long createdBy, Pageable pageable);

}
