package com.highlight.highlight_backend.bid.repository;

import com.highlight.highlight_backend.bid.domain.Bid;
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
     *
     * 여러 사용자를 보여주지만 한 사용자의 여러 입찰이 아닌 사용자당 금액이 가장 높은거 1개만 보여줌
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
           "ORDER BY b.bidAmount DESC, b.createdAt ASC " +
            "LIMIT 1")
    Optional<Bid> findCurrentHighestBidByAuction(@Param("auction") Auction auction);


    /**
     * Auction 의 입찰 중 가장 금액이 높은 입찰을 가져옴 (FETCH JOIN 으로 USER도 같이 조회)
     */
    @Query ("SELECT b FROM Bid b " +
            "JOIN FETCH b.user " +
            "WHERE b.auction = : auction " +
            "ORDER BY b.bidAmount DESC " +
            "LIMIT 1")
    Optional<Bid> findWinnerBidWithUser(@Param("auction") Auction auction);


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
     * Auction Id, UserId를 이용해 특정 사용자의 특정 경매에서 최고 입찰가 조회
     */

    Optional<Bid> findTopBidByAuction_IdAndUser_IdOrderByBidAmountDesc(Long auctionId, Long userId);
    
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

    /**
     * 현재 최고가 입찰 내역을 확인.
     */
    Optional<Bid> findTopByAuctionOrderByBidAmountDesc(Auction auction);

    /**
     * 입찰을 조회하여 해당 사용자가 입찰에 참여 여부를 반환
     * @return true : 이미 입찰에 참여함. / false : 입찰에 참여 이력 없음.
     */
    boolean existsByAuctionAndUser(Auction auction, User user);


    /**
     * 알림용: Bid + User + Auction 한 번에 조회 (N+1 방지)
     */
    @Query("SELECT b FROM Bid b " +
            "JOIN FETCH b.user " +
            "JOIN FETCH b.auction " +
            "WHERE b.id = :id")
    Optional<Bid> findByIdWithUserAndAuction(@Param("id") Long newBidId);

    /**
     * 알림용: Bid + User 한 번에 조회 (이전 1등 찾기용)
     */
    @Query("SELECT b FROM Bid b " +
            "JOIN FETCH b.user " +
            "WHERE b.id = :id")
    Optional<Bid> findByIdWithUser(@Param("id") Long id);
}