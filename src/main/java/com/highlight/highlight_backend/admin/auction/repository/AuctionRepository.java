package com.highlight.highlight_backend.admin.auction.repository;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.exception.AuctionErrorCode;
import com.highlight.highlight_backend.exception.BusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 경매 Repository
 * 
 * 경매 데이터 액세스를 위한 JPA Repository입니다.
 * 
 * @author 전우선
 * @since 2025.08.13
 */
@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long>, JpaSpecificationExecutor<Auction> {


    default Auction getOrThrow(Long id) {
        return findById(id)
                .orElseThrow(() -> new BusinessException(AuctionErrorCode.AUCTION_NOT_FOUND));
    }


    List<Auction> findByStatusAndScheduledStartTimeBefore(Auction.AuctionStatus status, LocalDateTime time);

    @Query("select a FROM Auction a WHERE a.createdBy = :adminId")
    List<Auction> findByAdminAuction(@Param("adminId") Long adminId);

    /**
     * 상품 ID로 진행 중인 경매 조회
     * 
     * @param productId 상품 ID
     * @return 해당 상품의 진행 중인 경매 정보
     */
    @Query("SELECT a FROM Auction a WHERE a.product.id = :productId AND a.status = 'IN_PROGRESS' ORDER BY a.createdAt DESC")
    Optional<Auction> findActiveAuctionByProductId(@Param("productId") Long productId);
    
    /**
     * 상품 ID로 진행 중이거나 예약된 경매 조회
     * 
     * @param productId 상품 ID
     * @return 해당 상품의 진행 중이거나 예약된 경매 정보
     */
    @Query("SELECT a FROM Auction a WHERE a.product.id = :productId AND a.status IN ('IN_PROGRESS', 'SCHEDULED') ORDER BY a.createdAt DESC")
    Optional<Auction> findActiveOrScheduledAuctionByProductId(@Param("productId") Long productId);
    
    /**
     * 경매 상태로 조회
     * 
     * @param status 경매 상태
     * @param pageable 페이징 정보
     * @return 해당 상태의 경매 목록
     */
    Page<Auction> findByStatus(Auction.AuctionStatus status, Pageable pageable);

    /**
     * 진행 중인 경매 중 종료 시간이 지난 경매 조회
     * 
     * @param currentTime 현재 시간
     * @return 종료 대기 중인 경매 목록
     */
    @Query("SELECT a FROM Auction a WHERE a.status = 'IN_PROGRESS' AND a.scheduledEndTime <= :currentTime")
    List<Auction> findInProgressAuctionsReadyToEnd(@Param("currentTime") LocalDateTime currentTime);
    

    /**
     * 관리자가 생성한 경매 목록 조회
     * 
     * @param createdBy 관리자 ID
     * @param pageable 페이징 정보
     * @return 해당 관리자가 생성한 경매 목록
     */
    Page<Auction> findByCreatedByOrderByCreatedAtDesc(Long createdBy, Pageable pageable);

    /**
     * 상품과 함께 경매 정보 조회
     * 
     * @param auctionId 경매 ID
     * @return 상품 정보를 포함한 경매 정보
     */
    @Query("SELECT a FROM Auction a LEFT JOIN FETCH a.product p LEFT JOIN FETCH p.images WHERE a.id = :auctionId")
    Optional<Auction> findByIdWithProduct(@Param("auctionId") Long auctionId);
    
    /**
     * 상품이 이미 경매에 등록되어 있는지 확인
     * 
     * @param productId 상품 ID
     * @return 경매 등록 여부
     */
    boolean existsByProductId(Long productId);
    
    /**
     * 경매 조회 (비관적 락)
     * 동시 입찰 시 데이터 일관성을 위해 락을 사용합니다.
     * 
     * @param auctionId 경매 ID
     * @return 경매 정보
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Auction a WHERE a.id = :auctionId")
    Optional<Auction> findByIdWithLock(@Param("auctionId") Long auctionId);
    
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
}