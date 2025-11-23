package com.highlight.highlight_backend.product.wishlist.repository;

import com.highlight.highlight_backend.product.wishlist.domian.ProductWishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 상품 찜하기 Repository
 * 
 * 상품 찜하기 데이터 액세스를 위한 JPA Repository입니다.
 * 
 * @author 전우선
 * @since 2025.08.16
 */
@Repository
public interface ProductWishlistRepository extends JpaRepository<ProductWishlist, Long> {
    
    /**
     * 사용자별 찜한 상품 목록 조회
     * 
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 사용자의 찜한 상품 목록
     */
    Page<ProductWishlist> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * 특정 사용자의 특정 상품 찜하기 조회
     * 
     * @param userId 사용자 ID
     * @param productId 상품 ID
     * @return 찜하기 정보
     */
    Optional<ProductWishlist> findByUserIdAndProductId(Long userId, Long productId);
    
    /**
     * 사용자-상품 찜하기 존재 여부 확인
     * 
     * @param userId 사용자 ID
     * @param productId 상품 ID
     * @return 찜하기 여부
     */
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    
    /**
     * 특정 상품을 찜한 사용자 수 조회
     * 
     * @param productId 상품 ID
     * @return 찜한 사용자 수
     */
    long countByProductId(Long productId);
    
    /**
     * 사용자가 찜한 상품 개수 조회
     * 
     * @param userId 사용자 ID
     * @return 찜한 상품 개수
     */
    long countByUserId(Long userId);
    
    /**
     * 사용자별 찜하기 삭제
     * 
     * @param userId 사용자 ID
     * @param productId 상품 ID
     */
    void deleteByUserIdAndProductId(Long userId, Long productId);
    
    /**
     * 사용자의 찜한 상품을 상품 정보와 함께 조회
     * 
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 상품 정보가 포함된 찜하기 목록
     */
    @Query("SELECT w FROM ProductWishlist w " +
           "JOIN FETCH w.product p " +
           "WHERE w.userId = :userId " +
           "ORDER BY w.createdAt DESC")
    Page<ProductWishlist> findByUserIdWithProduct(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * 여러 상품의 찜하기 상태를 한번에 조회
     * 
     * @param userId 사용자 ID
     * @param productIds 상품 ID 목록
     * @return 찜한 상품 ID 목록
     */
    @Query("SELECT w.productId FROM ProductWishlist w " +
           "WHERE w.userId = :userId AND w.productId IN :productIds")
    List<Long> findWishlistedProductIds(@Param("userId") Long userId, @Param("productIds") List<Long> productIds);
    
    /**
     * 인기 상품 조회 (찜 개수 기준)
     * 
     * @param pageable 페이징 정보
     * @return 찜 개수가 많은 상품 ID 목록
     */
    @Query("SELECT w.productId, COUNT(w) as wishCount " +
           "FROM ProductWishlist w " +
           "GROUP BY w.productId " +
           "ORDER BY wishCount DESC")
    Page<Object[]> findMostWishlistedProducts(Pageable pageable);
}