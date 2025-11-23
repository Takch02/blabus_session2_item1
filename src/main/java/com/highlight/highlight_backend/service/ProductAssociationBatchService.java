package com.highlight.highlight_backend.service;

import com.highlight.highlight_backend.product.domian.Product;
import com.highlight.highlight_backend.domain.ProductAssociation;
import com.highlight.highlight_backend.repository.ProductAssociationRepository;
import com.highlight.highlight_backend.product.repository.AdminProductRepository;
import com.highlight.highlight_backend.repository.UserProductViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 상품 연관도 계산 배치 서비스
 * 
 * 사용자 조회 이력을 기반으로 상품 간 연관도를 계산하고
 * ProductAssociation 테이블을 업데이트하는 배치 작업을 수행합니다.
 * 
 * 실행 주기:
 * - 연관도 계산: 매일 새벽 3시
 * - 데이터 정리: 매주 일요일 새벽 4시
 * 
 * @author 전우선
 * @since 2025.08.18
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProductAssociationBatchService {

    private final UserProductViewRepository userProductViewRepository;
    private final ProductAssociationRepository productAssociationRepository;
    private final AdminProductRepository adminProductRepository;

    /**
     * 상품 연관도 계산 배치 작업 (매일 새벽 3시 실행)
     * 
     * 최근 30일간의 사용자 조회 이력을 분석하여
     * 상품 간 연관도를 계산하고 DB에 저장합니다.
     */
    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시
    @Async
    public CompletableFuture<Void> calculateProductAssociations() {
        long startTime = System.currentTimeMillis();
        log.info("=== 상품 연관도 계산 배치 작업 시작 ===");

        try {
            // 1. 활성 상품 목록 조회
            List<Product> activeProducts = adminProductRepository.findByStatus(Product.ProductStatus.ACTIVE,
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE)).getContent();
            log.info("분석 대상 활성 상품 수: {}", activeProducts.size());

            if (activeProducts.isEmpty()) {
                log.warn("분석할 활성 상품이 없습니다.");
                return CompletableFuture.completedFuture(null);
            }

            // 2. 각 상품에 대해 연관도 계산
            int processedCount = 0;
            int totalAssociations = 0;
            
            LocalDateTime since30Days = LocalDateTime.now().minusDays(30);
            
            for (Product sourceProduct : activeProducts) {
                try {
                    int associations = calculateAssociationsForProduct(sourceProduct.getId(), since30Days);
                    totalAssociations += associations;
                    processedCount++;
                    
                    if (processedCount % 50 == 0) {
                        log.info("진행률: {}/{} 상품 처리 완료", processedCount, activeProducts.size());
                    }
                    
                } catch (Exception e) {
                    log.error("상품 연관도 계산 실패: productId={}, error={}", 
                             sourceProduct.getId(), e.getMessage(), e);
                }
            }

            long endTime = System.currentTimeMillis();
            log.info("=== 상품 연관도 계산 배치 작업 완료 === " +
                    "처리된 상품: {}, 생성/업데이트된 연관도: {}, 소요시간: {}ms",
                    processedCount, totalAssociations, endTime - startTime);

        } catch (Exception e) {
            log.error("상품 연관도 계산 배치 작업 실패", e);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 특정 상품에 대한 연관도 계산
     * 
     * @param sourceProductId 기준 상품 ID
     * @param sinceDate 분석 시작 날짜
     * @return 생성/업데이트된 연관도 개수
     */
    private int calculateAssociationsForProduct(Long sourceProductId, LocalDateTime sinceDate) {
        // 1. 세션 기반 함께 조회된 상품들
        List<Object[]> sessionBasedViews = userProductViewRepository
            .findCoViewedProductsBySession(sourceProductId, sinceDate);
            
        // 2. 사용자 기반 함께 조회된 상품들
        List<Object[]> userBasedViews = userProductViewRepository
            .findCoViewedProductsByUser(sourceProductId, sinceDate);

        // 3. 연관도 점수 계산
        Map<Long, AssociationData> associationDataMap = new HashMap<>();
        
        // 세션 기반 데이터 처리
        for (Object[] result : sessionBasedViews) {
            Long targetProductId = (Long) result[0];
            Long coViewCount = (Long) result[1];
            
            associationDataMap.computeIfAbsent(targetProductId, k -> new AssociationData())
                .addSessionCoView(coViewCount.intValue());
        }
        
        // 사용자 기반 데이터 처리
        for (Object[] result : userBasedViews) {
            Long targetProductId = (Long) result[0];
            Long coViewCount = (Long) result[1];
            
            associationDataMap.computeIfAbsent(targetProductId, k -> new AssociationData())
                .addUserCoView(coViewCount.intValue());
        }

        // 4. 연관도 엔티티 생성/업데이트
        int updatedCount = 0;
        Product sourceProduct = adminProductRepository.findById(sourceProductId).orElse(null);
        if (sourceProduct == null) {
            return 0;
        }

        for (Map.Entry<Long, AssociationData> entry : associationDataMap.entrySet()) {
            Long targetProductId = entry.getKey();
            AssociationData data = entry.getValue();
            
            // 최소 임계값 확인 (너무 적은 연관도는 제외)
            if (data.getTotalCoViewCount() < 2) {
                continue;
            }
            
            try {
                Product targetProduct = adminProductRepository.findById(targetProductId).orElse(null);
                if (targetProduct == null || targetProduct.getStatus() != Product.ProductStatus.ACTIVE) {
                    continue;
                }
                
                // 기존 연관도 조회 또는 새로 생성
                ProductAssociation association = productAssociationRepository
                    .findBySourceProductIdAndTargetProductId(sourceProductId, targetProductId)
                    .orElseGet(() -> new ProductAssociation(sourceProduct, targetProduct));
                
                // 연관도 데이터 업데이트
                association.incrementCoViewCount(
                    data.getSameSessionCount() > 0, 
                    data.getSameUserCount() > 0
                );
                
                // 점수 계산 및 업데이트
                BigDecimal newScore = calculateAssociationScore(association, data);
                association.updateAssociationScore(newScore);
                
                // 저장
                productAssociationRepository.save(association);
                updatedCount++;
                
            } catch (Exception e) {
                log.error("연관도 업데이트 실패: sourceId={}, targetId={}, error={}", 
                         sourceProductId, targetProductId, e.getMessage());
            }
        }
        
        return updatedCount;
    }

    /**
     * 연관도 점수 계산
     */
    private BigDecimal calculateAssociationScore(ProductAssociation association, AssociationData data) {
        double baseScore = data.getTotalCoViewCount();
        double sessionBonus = data.getSameSessionCount() * 2.0;
        double userBonus = data.getSameUserCount() * 1.5;
        
        // 시간 보너스 (최근 30일 내)
        double timeBonus = baseScore * 0.2;
        
        // 카테고리 보너스
        double categoryBonus = 0.0;
        if (association.getSourceProduct().getCategory() != null && 
            association.getTargetProduct().getCategory() != null &&
            association.getSourceProduct().getCategory().equals(
                association.getTargetProduct().getCategory())) {
            categoryBonus = baseScore * 0.1;
        }
        
        double totalScore = baseScore + sessionBonus + userBonus + timeBonus + categoryBonus;
        return BigDecimal.valueOf(Math.min(totalScore, 100.0));
    }

    /**
     * 오래되고 연관도가 낮은 데이터 정리 (매주 일요일 새벽 4시)
     */
    @Scheduled(cron = "0 0 4 * * 0") // 매주 일요일 새벽 4시
    @Async
    public CompletableFuture<Void> cleanupOldAssociations() {
        log.info("=== 오래된 연관도 데이터 정리 배치 작업 시작 ===");
        
        try {
            // 연관도 점수가 5 미만이고 30일 이상 업데이트되지 않은 데이터 삭제
            List<ProductAssociation> oldAssociations = productAssociationRepository
                .findLowScoreOldAssociations(BigDecimal.valueOf(5.0), 30);
                
            if (!oldAssociations.isEmpty()) {
                productAssociationRepository.deleteAll(oldAssociations);
                log.info("정리된 오래된 연관도 데이터: {} 개", oldAssociations.size());
            } else {
                log.info("정리할 오래된 연관도 데이터가 없습니다.");
            }
            
        } catch (Exception e) {
            log.error("오래된 연관도 데이터 정리 실패", e);
        }
        
        log.info("=== 오래된 연관도 데이터 정리 배치 작업 완료 ===");
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 연관도 데이터 임시 저장용 클래스
     */
    private static class AssociationData {
        private int sameSessionCount = 0;
        private int sameUserCount = 0;
        private int totalCoViewCount = 0;
        
        public void addSessionCoView(int count) {
            this.sameSessionCount += count;
            this.totalCoViewCount += count;
        }
        
        public void addUserCoView(int count) {
            this.sameUserCount += count;
            this.totalCoViewCount += count;
        }
        
        public int getSameSessionCount() { return sameSessionCount; }
        public int getSameUserCount() { return sameUserCount; }
        public int getTotalCoViewCount() { return totalCoViewCount; }
    }
}