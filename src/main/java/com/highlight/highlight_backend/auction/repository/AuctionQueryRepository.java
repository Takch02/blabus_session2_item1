package com.highlight.highlight_backend.auction.repository;

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
 * Query 를 이용한 Auction Repository 입니다.
 *
 */
@Repository
public interface AuctionQueryRepository extends JpaRepository<Auction, Long>, JpaSpecificationExecutor<Auction> {

    @Query("SELECT a FROM Auction a WHERE a.id = :auctionId")
    Auction findOne (@Param("auctionId")Long auctionId);

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
     * 진행 중인 경매 중 종료 시간이 지난 경매 조회
     * 
     * @param currentTime 현재 시간
     * @return 종료 대기 중인 경매 목록
     */
    @Query("SELECT a FROM Auction a WHERE a.status = 'IN_PROGRESS' AND a.scheduledEndTime <= :currentTime")
    List<Auction> findInProgressAuctionsReadyToEnd(@Param("currentTime") LocalDateTime currentTime);
    

    /**
     * 상품과 함께 경매 정보 조회
     * 
     * @param auctionId 경매 ID
     * @return 상품 정보를 포함한 경매 정보
     */
    @Query("SELECT a FROM Auction a LEFT JOIN FETCH a.product p LEFT JOIN FETCH p.images WHERE a.id = :auctionId")
    Optional<Auction> findByIdWithProduct(@Param("auctionId") Long auctionId);

    
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
}