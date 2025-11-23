package com.highlight.highlight_backend.product.repository;

import com.highlight.highlight_backend.product.domian.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 상품 이미지 Repository
 * 
 * 상품 이미지 데이터 액세스를 위한 JPA Repository입니다.
 */
@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    

    /**
     * 단일 상품의 대표 이미지 URL 조회
     * 
     * @param productId 상품 ID
     * @return 대표 이미지 URL
     */
    @Query("SELECT pi.imageUrl FROM ProductImage pi WHERE pi.product.id = :productId AND pi.isPrimary = true")
    String findPrimaryImageUrlByProductId(@Param("productId") Long productId);
}