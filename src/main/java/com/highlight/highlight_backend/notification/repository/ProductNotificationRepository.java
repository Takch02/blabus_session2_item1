package com.highlight.highlight_backend.notification.repository;

import com.highlight.highlight_backend.notification.domain.ProductNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 상품 알림 Repository
 * 
 * 상품 알림 설정 데이터 액세스를 위한 JPA Repository입니다.
 * 
 * @author 전우선
 * @since 2025.08.16
 */
@Repository
public interface ProductNotificationRepository extends JpaRepository<ProductNotification, Long> {
    
    /**
     * 사용자별 알림 목록 조회
     * 
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 사용자의 알림 목록
     */
    Page<ProductNotification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    /**
     * 사용자의 활성 알림 목록 조회
     * 
     * @param userId 사용자 ID
     * @param isActive 활성화 여부
     * @param pageable 페이징 정보
     * @return 사용자의 활성 알림 목록
     */
    Page<ProductNotification> findByUserIdAndIsActiveOrderByCreatedAtDesc(Long userId, boolean isActive, Pageable pageable);
    
    /**
     * 특정 상품에 대한 사용자의 알림 설정 조회
     * 
     * @param userId 사용자 ID
     * @param productId 상품 ID
     * @return 알림 설정
     */
    Optional<ProductNotification> findByUserIdAndProductId(Long userId, Long productId);
    
    /**
     * 특정 상품에 알림 설정한 모든 사용자 조회
     * 
     * @param productId 상품 ID
     * @param isActive 활성화 여부
     * @return 알림 설정 목록
     */
    List<ProductNotification> findByProductIdAndIsActive(Long productId, boolean isActive);
    
    /**
     * 특정 상품에 특정 유형의 알림을 설정한 사용자들 조회
     * 
     * @param productId 상품 ID
     * @param notificationType 알림 유형
     * @param isActive 활성화 여부
     * @return 알림 설정 목록
     */
    List<ProductNotification> findByProductIdAndNotificationTypeAndIsActive(
        Long productId, 
        ProductNotification.NotificationType notificationType, 
        boolean isActive
    );
    
    /**
     * 사용자-상품 알림 설정 존재 여부 확인
     * 
     * @param userId 사용자 ID
     * @param productId 상품 ID
     * @return 존재 여부
     */
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    
    /**
     * 사용자의 활성 알림 개수 조회
     * 
     * @param userId 사용자 ID
     * @param isActive 활성화 여부
     * @return 활성 알림 개수
     */
    long countByUserIdAndIsActive(Long userId, boolean isActive);
    
    /**
     * 상품별 알림 설정 개수 조회
     * 
     * @param productId 상품 ID
     * @param isActive 활성화 여부
     * @return 알림 설정 개수
     */
    long countByProductIdAndIsActive(Long productId, boolean isActive);
    
    /**
     * 사용자의 알림 설정을 상품 정보와 함께 조회
     * 
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 상품 정보가 포함된 알림 목록
     */
    @Query("SELECT n FROM ProductNotification n " +
           "JOIN FETCH n.product p " +
           "WHERE n.userId = :userId " +
           "ORDER BY n.createdAt DESC")
    Page<ProductNotification> findByUserIdWithProduct(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * 사용자별 알림 설정 삭제
     * 
     * @param userId 사용자 ID
     * @param productId 상품 ID
     */
    void deleteByUserIdAndProductId(Long userId, Long productId);
}