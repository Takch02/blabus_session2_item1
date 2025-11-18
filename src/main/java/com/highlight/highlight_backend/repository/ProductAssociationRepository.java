package com.highlight.highlight_backend.repository;

import com.highlight.highlight_backend.domain.ProductAssociation;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 상품 연관도 Repository
 * 
 * @author 전우선
 * @since 2025.08.18
 */
@Repository
public interface ProductAssociationRepository extends JpaRepository<ProductAssociation, Long> {

    /**
     * 특정 상품과 연관된 상품들을 연관도 점수 순으로 조회
     * 
     * @param sourceProductId 기준 상품 ID
     * @param pageable 페이징 정보
     * @return 연관도 순 상품 목록
     */
    @Query("SELECT pa FROM ProductAssociation pa " +
           "WHERE pa.sourceProduct.id = :sourceProductId " +
           "AND pa.targetProduct.status = 'ACTIVE' " +
           "ORDER BY pa.associationScore DESC, pa.coViewCount DESC")
    List<ProductAssociation> findBySourceProductIdOrderByScoreDesc(
            @Param("sourceProductId") Long sourceProductId, 
            PageRequest pageable);

    /**
     * 두 상품 간의 연관도 조회
     * 
     * @param sourceProductId 기준 상품 ID
     * @param targetProductId 대상 상품 ID
     * @return 상품 연관도 (존재하지 않으면 Optional.empty())
     */
    Optional<ProductAssociation> findBySourceProductIdAndTargetProductId(
            @Param("sourceProductId") Long sourceProductId,
            @Param("targetProductId") Long targetProductId);

    /**
     * 특정 상품을 기준으로 한 모든 연관도 조회 (양방향)
     * A->B 와 B->A 모두 조회
     * 
     * @param productId 상품 ID
     * @return 연관도 목록
     */
    @Query("SELECT pa FROM ProductAssociation pa " +
           "WHERE pa.sourceProduct.id = :productId OR pa.targetProduct.id = :productId")
    List<ProductAssociation> findAllByProductId(@Param("productId") Long productId);

    /**
     * 연관도 점수가 특정 임계값 이상인 연관도들 조회
     * 
     * @param sourceProductId 기준 상품 ID
     * @param minScore 최소 연관도 점수
     * @param pageable 페이징 정보
     * @return 임계값 이상의 연관도 목록
     */
    @Query("SELECT pa FROM ProductAssociation pa " +
           "WHERE pa.sourceProduct.id = :sourceProductId " +
           "AND pa.associationScore >= :minScore " +
           "AND pa.targetProduct.status = 'ACTIVE' " +
           "ORDER BY pa.associationScore DESC")
    List<ProductAssociation> findBySourceProductIdAndScoreGreaterThanEqual(
            @Param("sourceProductId") Long sourceProductId,
            @Param("minScore") BigDecimal minScore,
            PageRequest pageable);

    /**
     * 배치 작업용: 연관도 점수 업데이트가 필요한 연관도들 조회
     * 마지막 점수 계산 시간이 특정 시간 이전인 것들
     * 
     * @param hours 업데이트 주기 (시간)
     * @return 업데이트 대상 연관도 목록
     */
    @Query("SELECT pa FROM ProductAssociation pa " +
           "WHERE pa.scoreCalculatedAt IS NULL " +
           "OR pa.scoreCalculatedAt <= CURRENT_TIMESTAMP - :hours HOUR " +
           "ORDER BY pa.coViewCount DESC")
    List<ProductAssociation> findAssociationsNeedingScoreUpdate(@Param("hours") Integer hours);

    /**
     * 특정 상품의 상위 N개 추천 상품 ID만 조회 (성능 최적화용)
     * 
     * @param sourceProductId 기준 상품 ID
     * @param limit 조회 개수
     * @return 추천 상품 ID 목록
     */
    @Query("SELECT pa.targetProduct.id " +
           "FROM ProductAssociation pa " +
           "WHERE pa.sourceProduct.id = :sourceProductId " +
           "AND pa.targetProduct.status = 'ACTIVE' " +
           "AND pa.associationScore > 0 " +
           "ORDER BY pa.associationScore DESC")
    List<Long> findTopRecommendedProductIds(
            @Param("sourceProductId") Long sourceProductId,
            PageRequest pageable);

    /**
     * 연관도 통계 조회 - 특정 상품의 총 연관 상품 수
     * 
     * @param sourceProductId 기준 상품 ID
     * @param minScore 최소 연관도 점수
     * @return 연관 상품 수
     */
    @Query("SELECT COUNT(pa) FROM ProductAssociation pa " +
           "WHERE pa.sourceProduct.id = :sourceProductId " +
           "AND pa.associationScore >= :minScore " +
           "AND pa.targetProduct.status = 'ACTIVE'")
    Long countAssociationsWithMinScore(
            @Param("sourceProductId") Long sourceProductId,
            @Param("minScore") BigDecimal minScore);

    /**
     * 연관도가 낮은 오래된 데이터 정리용
     * 특정 점수 미만이고 마지막 함께 조회가 오래된 연관도 삭제
     * 
     * @param maxScore 최대 점수
     * @param days 마지막 조회 기준 일수
     * @return 삭제할 연관도 목록
     */
    @Query("SELECT pa FROM ProductAssociation pa " +
           "WHERE pa.associationScore < :maxScore " +
           "AND (pa.lastCoViewedAt IS NULL OR pa.lastCoViewedAt <= CURRENT_TIMESTAMP - :days DAY)")
    List<ProductAssociation> findLowScoreOldAssociations(
            @Param("maxScore") BigDecimal maxScore,
            @Param("days") Integer days);

    /**
     * 특정 상품이 타겟으로 포함된 연관도들 조회 (역방향)
     * 
     * @param targetProductId 대상 상품 ID
     * @param pageable 페이징 정보
     * @return 역방향 연관도 목록
     */
    @Query("SELECT pa FROM ProductAssociation pa " +
           "WHERE pa.targetProduct.id = :targetProductId " +
           "AND pa.sourceProduct.status = 'ACTIVE' " +
           "ORDER BY pa.associationScore DESC")
    List<ProductAssociation> findByTargetProductIdOrderByScoreDesc(
            @Param("targetProductId") Long targetProductId,
            PageRequest pageable);
}