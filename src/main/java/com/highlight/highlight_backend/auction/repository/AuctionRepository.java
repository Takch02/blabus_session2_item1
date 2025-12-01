package com.highlight.highlight_backend.auction.repository;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.exception.AuctionErrorCode;
import com.highlight.highlight_backend.exception.BusinessException;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 경매 repository
 *
 * JPA 메소드를 이용한 repo 입니다.
 */
public interface AuctionRepository extends JpaRepository<Auction, Long>, JpaSpecificationExecutor<Auction> {

    /**
     * auctionId로 경매를 가져옴
     * 없다면 예외를 던짐
     */
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

    /**
     * 예약된 상태이며 예약시간이 지난 경매를 조회
     */
    List<Auction> findByStatusAndScheduledStartTimeBefore(Auction.AuctionStatus status, LocalDateTime time);

    /**
     * 경매 상태로 경매 목록 조회 (리스트 형태)
     *
     */
    List<Auction> findByStatus(Auction.AuctionStatus status);

    /**
     * 특정 시간 범위 내 종료 예정인 진행 중 경매 조회
     *
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



    /**
     * AdminId를 이용해 경매 조회
     */
    @Query("select a FROM Auction a WHERE a.createdBy = :adminId")
    List<Auction> findByAdminAuction(@Param("adminId") Long adminId);


    /**
     * 진행 중인 경매 중 종료 시간이 지난 경매 조회
     *
     * @param currentTime 현재 시간
     * @return 종료 대기 중인 경매 목록
     */
    @Query("SELECT a FROM Auction a WHERE a.status = 'IN_PROGRESS' AND a.scheduledEndTime <= :currentTime")
    List<Auction> findInProgressAuctionsReadyToEnd(@Param("currentTime") LocalDateTime currentTime);


    /**
     * 경매 조회 (비관적 락)
     * 동시 입찰 시 데이터 일관성을 위해 락을 사용합니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Auction a WHERE a.id = :auctionId")
    Optional<Auction> findByIdWithLock(@Param("auctionId") Long auctionId);


    /**
     * 사용자가 낙찰한 프리미엄 상품들의 ID 조회
     *
     * @param userId 사용자 ID
     * @return 프리미엄 상품 ID 목록
     */
    @Query("SELECT DISTINCT a.product.id FROM Auction a " +
            "JOIN Bid b ON a.id = b.auction.id " +
            "WHERE b.user.id = :userId " +
            "AND b.status = 'WINNING' " +
            "AND a.status = 'COMPLETED' " +
            "AND a.product.isPremium = true")
    List<Long> findPremiumProductIdsByUserId(@Param("userId") Long userId);

    /**
     * auction 에 참여한 전체 사용자를 조회
     */
    Long findAuctionByTotalBidders(Long auctionId);

    /**
     *
     * @param auctionId
     * @return
     */
    Long findAuctionByTotalBids(Long auctionId);

}
