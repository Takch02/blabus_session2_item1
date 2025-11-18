package com.highlight.highlight_backend.repository;

import com.highlight.highlight_backend.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 상품 Repository
 * 
 * 상품 데이터 액세스를 위한 JPA Repository입니다.
 * 
 * @author 전우선
 * @since 2025.08.13
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    /**
     * 상품명으로 검색
     * 
     * @param productName 상품명 (부분 일치)
     * @param pageable 페이징 정보
     * @return 검색된 상품 목록
     */
    Page<Product> findByProductNameContainingIgnoreCase(String productName, Pageable pageable);
    
    /**
     * 상품 상태로 조회
     * 
     * @param status 상품 상태
     * @param pageable 페이징 정보
     * @return 해당 상태의 상품 목록
     */
    Page<Product> findByStatus(Product.ProductStatus status, Pageable pageable);
    
    /**
     * 카테고리로 조회
     * 
     * @param category 카테고리
     * @param pageable 페이징 정보
     * @return 해당 카테고리의 상품 목록
     */
    Page<Product> findByCategory(String category, Pageable pageable);


    Page<Product> findByRegisteredByOrderByCreatedAtDesc(Long registeredBy, Pageable pageable);
    
    /**
     * 등록한 관리자로 조회
     * 
     * @param registeredBy 등록한 관리자 ID
     * @param pageable 페이징 정보
     * @return 해당 관리자가 등록한 상품 목록
     */
    Page<Product> findByRegisteredBy(Long registeredBy, Pageable pageable);
    
    /**
     * 상품 상태별 개수 조회
     * 
     * @param status 상품 상태
     * @return 해당 상태의 상품 개수
     */
    long countByStatus(Product.ProductStatus status);
    
    /**
     * 경매 가능한 상품 목록 조회 (ACTIVE 상태)
     * 
     * @param pageable 페이징 정보
     * @return 경매 가능한 상품 목록
     */
    Page<Product> findByStatusOrderByCreatedAtDesc(Product.ProductStatus status, Pageable pageable);
    
    /**
     * 상품 ID로 이미지와 함께 조회
     * 
     * @param id 상품 ID
     * @return 상품 정보 (이미지 포함)
     */
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images WHERE p.id = :id")
    Optional<Product> findByIdWithImages(@Param("id") Long id);
    
    /**
     * 활성 상품 중 카테고리별 조회
     * 
     * @param category 카테고리
     * @param status 상품 상태
     * @param pageable 페이징 정보
     * @return 활성 상품 목록
     */
    Page<Product> findByCategoryAndStatus(String category, Product.ProductStatus status, Pageable pageable);
    
    /**
     * 상품명과 상태로 복합 검색
     * 
     * @param productName 상품명 (부분 일치)
     * @param status 상품 상태
     * @param pageable 페이징 정보
     * @return 검색된 상품 목록
     */
    Page<Product> findByProductNameContainingIgnoreCaseAndStatus(String productName, Product.ProductStatus status, Pageable pageable);
    
    /**
     * 모든 카테고리 목록 조회
     * 
     * @return 사용 중인 카테고리 목록
     */
    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.category IS NOT NULL ORDER BY p.category")
    List<String> findAllCategories();

    /**
     * 관련 상품 추천 조회
     * 동일 카테고리 또는 동일 브랜드 상품을 추천하되, 자기 자신은 제외
     * 
     * @param excludeProductId 제외할 상품 ID (자기 자신)
     * @param category 카테고리
     * @param brand 브랜드
     * @param pageable 페이징 정보
     * @return 추천 상품 목록
     */
    @Query("SELECT p FROM Product p WHERE p.id != :excludeProductId " +
           "AND p.status = 'ACTIVE' " +
           "AND (p.category = :category OR p.brand = :brand) " +
           "ORDER BY " +
           "CASE WHEN p.category = :category AND p.brand = :brand THEN 1 " +
           "     WHEN p.category = :category THEN 2 " +
           "     WHEN p.brand = :brand THEN 3 " +
           "     ELSE 4 END, " +
           "p.createdAt DESC")
    List<Product> findRecommendedProducts(@Param("excludeProductId") Long excludeProductId,
                                        @Param("category") Product.Category category,
                                        @Param("brand") String brand,
                                        Pageable pageable);
    
    /**
     * 경매가 등록된 상품 중 관련 상품 추천 조회
     * 동일 카테고리 또는 동일 브랜드 상품을 추천하되, 경매가 등록된 상품만 포함
     * 
     * @param excludeProductId 제외할 상품 ID (자기 자신)
     * @param category 카테고리
     * @param brand 브랜드
     * @param pageable 페이징 정보
     * @return 추천 상품 목록
     */
    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.images " +
           "JOIN Auction a ON p.id = a.product.id " +
           "WHERE p.id != :excludeProductId " +
           "AND p.status = 'ACTIVE' " +
           "AND (p.category = :category OR p.brand = :brand) " +
           "ORDER BY " +
           "CASE WHEN p.category = :category AND p.brand = :brand THEN 1 " +
           "     WHEN p.category = :category THEN 2 " +
           "     WHEN p.brand = :brand THEN 3 " +
           "     ELSE 4 END, " +
           "p.createdAt DESC")
    List<Product> findRecommendedProductsWithAuction(@Param("excludeProductId") Long excludeProductId,
                                                   @Param("category") Product.Category category,
                                                   @Param("brand") String brand,
                                                   Pageable pageable);
    
    /**
     * 경매가 등록된 모든 활성 상품 조회 (카테고리/브랜드 제한 없음)
     * 
     * @param excludeProductId 제외할 상품 ID (자기 자신)
     * @param pageable 페이징 정보
     * @return 경매가 등록된 모든 활성 상품 목록
     */
    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.images " +
           "JOIN Auction a ON p.id = a.product.id " +
           "WHERE p.id != :excludeProductId " +
           "AND p.status IN ('ACTIVE', 'IN_AUCTION') " +
           "AND a.status IN ('IN_PROGRESS', 'SCHEDULED') " +
           "ORDER BY p.createdAt DESC")
    List<Product> findAllActiveProductsWithAuction(@Param("excludeProductId") Long excludeProductId,
                                                 Pageable pageable);
    

}