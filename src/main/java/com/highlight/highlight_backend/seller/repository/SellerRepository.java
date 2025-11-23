package com.highlight.highlight_backend.seller.repository;

import com.highlight.highlight_backend.seller.domain.Seller;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 판매자 Repository
 * 
 * 판매자 데이터 액세스를 위한 JPA Repository입니다.
 * 
 * @author 전우선
 * @since 2025.08.16
 */
@Repository
public interface SellerRepository extends JpaRepository<Seller, Long> {
    
    /**
     * 판매자명으로 검색
     * 
     * @param sellerName 판매자명 (부분 일치)
     * @param pageable 페이징 정보
     * @return 검색된 판매자 목록
     */
    Page<Seller> findBySellerNameContainingIgnoreCase(String sellerName, Pageable pageable);
    
    /**
     * 판매자 상태로 조회
     * 
     * @param status 판매자 상태
     * @param pageable 페이징 정보
     * @return 해당 상태의 판매자 목록
     */
    Page<Seller> findByStatus(Seller.SellerStatus status, Pageable pageable);
    
    /**
     * 활성 상태 판매자 목록 조회
     * 
     * @param pageable 페이징 정보
     * @return 활성 판매자 목록
     */
    Page<Seller> findByStatusOrderByCreatedAtDesc(Seller.SellerStatus status, Pageable pageable);
    
    /**
     * 이메일로 판매자 조회
     * 
     * @param email 이메일
     * @return 판매자 정보
     */
    Optional<Seller> findByEmail(String email);
    
    /**
     * 사업자 등록번호로 판매자 조회
     * 
     * @param businessNumber 사업자 등록번호
     * @return 판매자 정보
     */
    Optional<Seller> findByBusinessNumber(String businessNumber);
    
    /**
     * 이메일 중복 체크
     * 
     * @param email 이메일
     * @return 중복 여부
     */
    boolean existsByEmail(String email);
    
    /**
     * 사업자 등록번호 중복 체크
     * 
     * @param businessNumber 사업자 등록번호
     * @return 중복 여부
     */
    boolean existsByBusinessNumber(String businessNumber);
    
    /**
     * 판매자 상태별 개수 조회
     * 
     * @param status 판매자 상태
     * @return 해당 상태의 판매자 개수
     */
    long countByStatus(Seller.SellerStatus status);
    
    /**
     * 평점이 높은 판매자 조회
     * 
     * @param pageable 페이징 정보
     * @return 평점 순 판매자 목록
     */
    @Query("SELECT s FROM Seller s WHERE s.status = 'ACTIVE' ORDER BY s.rating DESC, s.reviewCount DESC")
    Page<Seller> findTopRatedSellers(Pageable pageable);
    
    /**
     * 판매 건수가 많은 판매자 조회
     * 
     * @param pageable 페이징 정보
     * @return 판매 건수 순 판매자 목록
     */
    @Query("SELECT s FROM Seller s WHERE s.status = 'ACTIVE' ORDER BY s.salesCount DESC, s.rating DESC")
    Page<Seller> findTopSellersBySales(Pageable pageable);
}