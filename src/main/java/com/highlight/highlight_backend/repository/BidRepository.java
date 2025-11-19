package com.highlight.highlight_backend.repository;

import com.highlight.highlight_backend.domain.Bid;
import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 입찰 정보 리포지토리
 * 
 * @author 전우선
 * @since 2025.08.15
 */
@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {
    
    /**
     * 특정 경매의 입찰 내역 조회 (최신순)
     */
    @Query("SELECT b FROM Bid b " +
           "WHERE b.auction = :auction " +
           "AND b.status != 'CANCELLED' " +
           "ORDER BY b.createdAt DESC")
    Page<Bid> findBidsByAuctionOrderByCreatedAtDesc(
            @Param("auction") Auction auction, 
            Pageable pageable);
    
    /**
     * 특정 경매의 입찰 내역 조회 (입찰가 높은순) - 전체 입찰 내역
     * 관리자용으로 사용되며, 모든 입찰 기록을 반환합니다.
     */
    @Query("SELECT b FROM Bid b " +
           "WHERE b.auction = :auction " +
           "AND b.status != 'CANCELLED' " +
           "ORDER BY b.bidAmount DESC, b.createdAt ASC")
    Page<Bid> findAllBidsByAuctionOrderByBidAmountDesc(
            @Param("auction") Auction auction, 
            Pageable pageable);
    
    /**
     * 특정 경매의 사용자별 최신 입찰 조회 (입찰가 높은순)
     * 각 사용자의 최신 입찰 1개씩만 반환하여 일반적인 경매 UX를 제공합니다.
     */
    @Query("SELECT b FROM Bid b " +
           "WHERE b.auction = :auction " +
           "AND b.status != 'CANCELLED' " +
           "AND b.id IN (" +
           "    SELECT MAX(b2.id) FROM Bid b2 " +
           "    WHERE b2.auction = :auction " +
           "    AND b2.user = b.user " +
           "    AND b2.status != 'CANCELLED' " +
           "    GROUP BY b2.user" +
           ") " +
           "ORDER BY b.bidAmount DESC, b.createdAt ASC")
    Page<Bid> findBidsByAuctionOrderByBidAmountDesc(
            @Param("auction") Auction auction, 
            Pageable pageable);
    
    /**
     * 특정 경매의 현재 최고 입찰 조회
     */
    @Query("SELECT b FROM Bid b " +
           "WHERE b.auction = :auction " +
           "AND b.status IN ('ACTIVE', 'WINNING') " +
           "ORDER BY b.bidAmount DESC, b.createdAt ASC")
    Optional<Bid> findCurrentHighestBidByAuction(@Param("auction") Auction auction);
    
    /**
     * 특정 경매의 최고 입찰가 조회
     */
    @Query("SELECT MAX(b.bidAmount) FROM Bid b " +
           "WHERE b.auction = :auction " +
           "AND b.status IN ('ACTIVE', 'WINNING')")
    Optional<BigDecimal> findMaxBidAmountByAuction(@Param("auction") Auction auction);
    
    /**
     * 특정 경매의 입찰자 수 조회 (전체 입찰 기록 기준) - 관리자용
     */
    @Query("SELECT COUNT(DISTINCT b.user) FROM Bid b " +
           "WHERE b.auction = :auction " +
           "AND b.status != 'CANCELLED'")
    Long countAllDistinctBiddersByAuction(@Param("auction") Auction auction);
    
    /**
     * 특정 경매의 총 입찰 횟수 조회 (전체 입찰 기록 기준) - 관리자용
     */
    @Query("SELECT COUNT(b) FROM Bid b " +
           "WHERE b.auction = :auction " +
           "AND b.status != 'CANCELLED'")
    Long countAllBidsByAuction(@Param("auction") Auction auction);
    
    /**
     * 특정 경매의 입찰자 수 조회 (사용자별 최신 입찰 기준)
     * 거래내역 표시와 일치하는 통계를 제공합니다.
     */
    @Query("SELECT COUNT(DISTINCT b.user) FROM Bid b " +
           "WHERE b.auction = :auction " +
           "AND b.status != 'CANCELLED' " +
           "AND b.id IN (" +
           "    SELECT MAX(b2.id) FROM Bid b2 " +
           "    WHERE b2.auction = :auction " +
           "    AND b2.user = b.user " +
           "    AND b2.status != 'CANCELLED' " +
           "    GROUP BY b2.user" +
           ")")
    Long countDistinctBiddersByAuction(@Param("auction") Auction auction);
    
    /**
     * 특정 경매의 사용자별 최신 입찰 수 조회
     * 거래내역 표시와 일치하는 통계를 제공합니다.
     */
    @Query("SELECT COUNT(b) FROM Bid b " +
           "WHERE b.auction = :auction " +
           "AND b.status != 'CANCELLED' " +
           "AND b.id IN (" +
           "    SELECT MAX(b2.id) FROM Bid b2 " +
           "    WHERE b2.auction = :auction " +
           "    AND b2.user = b.user " +
           "    AND b2.status != 'CANCELLED' " +
           "    GROUP BY b2.user" +
           ")")
    Long countBidsByAuction(@Param("auction") Auction auction);
    
    /**
     * 사용자의 특정 경매 입찰 내역 조회
     */
    @Query("SELECT b FROM Bid b " +
           "WHERE b.user = :user " +
           "AND b.auction = :auction " +
           "AND b.status != 'CANCELLED' " +
           "ORDER BY b.createdAt DESC")
    List<Bid> findBidsByUserAndAuctionOrderByCreatedAtDesc(
            @Param("user") User user, 
            @Param("auction") Auction auction);
    
    /**
     * 사용자의 전체 입찰 내역 조회
     */
    @Query("SELECT b FROM Bid b " +
           "WHERE b.user = :user " +
           "AND b.status != 'CANCELLED' " +
           "ORDER BY b.createdAt DESC")
    Page<Bid> findBidsByUserOrderByCreatedAtDesc(
            @Param("user") User user, 
            Pageable pageable);
    
    /**
     * 사용자가 낙찰받은 입찰 조회
     */
    @Query("SELECT b FROM Bid b " +
           "WHERE b.user = :user " +
           "AND b.status = 'WON' " +
           "ORDER BY b.createdAt DESC")
    Page<Bid> findWonBidsByUser(
            @Param("user") User user, 
            Pageable pageable);
    
    /**
     * 특정 사용자가 해당 경매에 입찰했는지 확인
     */
    @Query("SELECT COUNT(b) > 0 FROM Bid b " +
           "WHERE b.user = :user " +
           "AND b.auction = :auction " +
           "AND b.status != 'CANCELLED'")
    boolean existsBidByUserAndAuction(@Param("user") User user, @Param("auction") Auction auction);
    
    /**
     * 동일한 금액으로 입찰된 내역 조회 (선도착 시간 우선 처리용)
     */
    @Query("SELECT b FROM Bid b " +
           "WHERE b.auction = :auction " +
           "AND b.bidAmount = :bidAmount " +
           "AND b.status != 'CANCELLED' " +
           "ORDER BY b.createdAt ASC")
    Optional<Bid> findBidByAuctionAndBidAmount(@Param("auction") Auction auction, @Param("bidAmount") java.math.BigDecimal bidAmount);
    
    /**
     * 특정 사용자의 특정 경매에서 연속 패배 횟수 조회
     * (마지막 승리 이후 또는 첫 입찰부터 현재까지의 OUTBID 횟수)
     */
    @Query("SELECT COUNT(b) FROM Bid b " +
           "WHERE b.user = :user " +
           "AND b.auction = :auction " +
           "AND b.status = 'OUTBID' " +
           "AND b.createdAt > COALESCE(" +
           "    (SELECT MAX(wb.createdAt) FROM Bid wb " +
           "     WHERE wb.user = :user AND wb.auction = :auction AND wb.status IN ('WINNING', 'WON')), " +
           "    '1970-01-01 00:00:00'" +
           ")")
    Long countConsecutiveLossesByUserAndAuction(@Param("user") User user, @Param("auction") Auction auction);
    
    /**
     * 특정 사용자의 특정 경매에서 최고 입찰가 조회
     */
    @Query("SELECT b FROM Bid b " +
           "WHERE b.user = :user " +
           "AND b.auction = :auction " +
           "AND b.status != 'CANCELLED' " +
           "ORDER BY b.bidAmount DESC")
    Optional<Bid> findTopBidByAuctionAndUserOrderByBidAmountDesc(@Param("auction") Auction auction, @Param("user") User user);
    
    /**
     * 사용자별 경매 참여 횟수 기준 랭킹 조회
     * 
     * 각 사용자가 참여한 고유한 경매 수를 기준으로 랭킹을 생성합니다.
     * 취소된 입찰은 제외하고 계산하며, 참여 횟수가 같은 경우 userId 오름차순으로 정렬합니다.
     * 
     * @param pageable 페이지네이션 정보 (페이지 번호, 크기)
     * @return Object[] 배열의 리스트 - [userId, nickname, auctionCount] 순서
     *         - userId (Long): 사용자 ID
     *         - nickname (String): 사용자 닉네임  
     *         - auctionCount (Long): 참여한 고유 경매 수
     */
    @Query("SELECT u.id as userId, u.nickname as nickname, COUNT(DISTINCT b.auction) as auctionCount " +
           "FROM User u " +
           "JOIN Bid b ON u.id = b.user.id " +
           "WHERE b.status != 'CANCELLED' " +
           "GROUP BY u.id, u.nickname " +
           "ORDER BY COUNT(DISTINCT b.auction) DESC, u.id ASC")
    List<Object[]> findUserRankingByAuctionParticipation(Pageable pageable);
    
    /**
     * 경매에 참여한 총 사용자 수 조회
     * 
     * 적어도 하나 이상의 입찰을 한 사용자의 수를 반환합니다.
     * 취소된 입찰은 제외하고 계산합니다.
     * 
     * @return 경매에 참여한 총 사용자 수 (중복 제거)
     */
    @Query("SELECT COUNT(DISTINCT b.user) FROM Bid b WHERE b.status != 'CANCELLED'")
    Long countDistinctUsers();
    
    /**
     * 특정 상품의 경매에서 사용자가 낙찰한 입찰 조회
     * 
     * @param productId 상품 ID
     * @param userId 사용자 ID
     * @return 낙찰 입찰 정보
     */
    @Query("SELECT b FROM Bid b " +
           "JOIN b.auction a " +
           "WHERE a.product.id = :productId " +
           "AND b.user.id = :userId " +
           "AND b.status = 'WINNING' " +
           "AND a.status = 'COMPLETED'")
    Optional<Bid> findWinningBidByProductIdAndUserId(
            @Param("productId") Long productId, 
            @Param("userId") Long userId);
}