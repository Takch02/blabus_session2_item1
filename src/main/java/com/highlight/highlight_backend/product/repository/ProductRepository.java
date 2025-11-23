package com.highlight.highlight_backend.product.repository;

import com.highlight.highlight_backend.exception.BusinessException;
import com.highlight.highlight_backend.exception.ProductErrorCode;
import com.highlight.highlight_backend.product.domian.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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


}
