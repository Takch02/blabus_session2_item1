package com.highlight.highlight_backend.admin.product.repository;

import com.highlight.highlight_backend.admin.product.domian.Product;
import com.highlight.highlight_backend.exception.BusinessException;
import com.highlight.highlight_backend.exception.ProductErrorCode;
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
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    default Product getOrThrow(Long id) {
        return findById(id)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
    }
    /**
     * 상품 상태로 조회
     * 
     * @param status 상품 상태
     * @param pageable 페이징 정보
     * @return 해당 상태의 상품 목록
     */
    Page<Product> findByStatus(Product.ProductStatus status, Pageable pageable);


    Page<Product> findByRegisteredByOrderByCreatedAtDesc(Long registeredBy, Pageable pageable);

    /**
     * 상품 ID로 이미지와 함께 조회
     * 
     * @param id 상품 ID
     * @return 상품 정보 (이미지 포함)
     */
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images WHERE p.id = :id")
    Optional<Product> findByIdWithImages(@Param("id") Long id);


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

}