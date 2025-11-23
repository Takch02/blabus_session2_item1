package com.highlight.highlight_backend.service;

import com.highlight.highlight_backend.domain.ProductNotification;
import com.highlight.highlight_backend.dto.ProductNotificationRequestDto;
import com.highlight.highlight_backend.dto.ProductNotificationResponseDto;
import com.highlight.highlight_backend.exception.BusinessException;
import com.highlight.highlight_backend.exception.CommonErrorCode;
import com.highlight.highlight_backend.exception.UserErrorCode;
import com.highlight.highlight_backend.exception.ProductErrorCode;
import com.highlight.highlight_backend.repository.ProductNotificationRepository;
import com.highlight.highlight_backend.product.repository.AdminProductRepository;
import com.highlight.highlight_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 상품 알림 서비스
 * 
 * 상품 알림 설정, 조회, 관리 기능을 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductNotificationService {
    
    private final ProductNotificationRepository notificationRepository;
    private final AdminProductRepository adminProductRepository;
    private final UserRepository userRepository;
    
    /**
     * 상품 알림 설정/해제
     * 
     * @param userId 사용자 ID
     * @param productId 상품 ID
     * @param requestDto 알림 설정 요청 데이터
     * @return 알림 설정 결과
     */
    @Transactional
    public ProductNotificationResponseDto setProductNotification(Long userId, Long productId, ProductNotificationRequestDto requestDto) {
        log.info("상품 알림 설정 요청: 사용자 {}, 상품 {}, 활성화 {}", userId, productId, requestDto.getIsActive());
        
        // 1. 사용자 및 상품 존재 확인
        validateUserAndProduct(userId, productId);
        
        // 2. 기존 알림 설정 조회
        Optional<ProductNotification> existingNotification = notificationRepository.findByUserIdAndProductId(userId, productId);
        
        ProductNotification notification;
        
        if (existingNotification.isPresent()) {
            // 3-1. 기존 설정이 있으면 업데이트
            notification = existingNotification.get();
            notification.setActive(requestDto.getIsActive());
            if (requestDto.getNotificationType() != null) {
                notification.setNotificationType(requestDto.getNotificationType());
            }
        } else {
            // 3-2. 새로운 알림 설정 생성
            notification = new ProductNotification(
                userId, 
                productId, 
                requestDto.getNotificationType() != null ? 
                    requestDto.getNotificationType() : 
                    ProductNotification.NotificationType.BID_UPDATE
            );
            notification.setActive(requestDto.getIsActive());
        }
        
        ProductNotification savedNotification = notificationRepository.save(notification);
        
        log.info("상품 알림 설정 완료: ID {}, 활성화 {}", savedNotification.getId(), savedNotification.isActive());
        
        return ProductNotificationResponseDto.from(savedNotification);
    }
    
    /**
     * 상품 알림 설정 토글
     * 
     * @param userId 사용자 ID
     * @param productId 상품 ID
     * @return 토글된 알림 설정
     */
    @Transactional
    public ProductNotificationResponseDto toggleProductNotification(Long userId, Long productId) {
        log.info("상품 알림 토글 요청: 사용자 {}, 상품 {}", userId, productId);
        
        // 1. 사용자 및 상품 존재 확인
        validateUserAndProduct(userId, productId);
        
        // 2. 기존 알림 설정 조회
        Optional<ProductNotification> existingNotification = notificationRepository.findByUserIdAndProductId(userId, productId);
        
        ProductNotification notification;
        
        if (existingNotification.isPresent()) {
            // 3-1. 기존 설정이 있으면 토글
            notification = existingNotification.get();
            notification.toggleActive();
        } else {
            // 3-2. 새로운 알림 설정 생성 (기본 활성화)
            notification = new ProductNotification(
                userId, 
                productId, 
                ProductNotification.NotificationType.BID_UPDATE
            );
        }
        
        ProductNotification savedNotification = notificationRepository.save(notification);
        
        log.info("상품 알림 토글 완료: ID {}, 활성화 {}", savedNotification.getId(), savedNotification.isActive());
        
        return ProductNotificationResponseDto.from(savedNotification);
    }
    
    /**
     * 사용자의 알림 목록 조회
     * 
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 사용자의 알림 목록
     */
    public Page<ProductNotificationResponseDto> getUserNotifications(Long userId, Pageable pageable) {
        log.info("사용자 알림 목록 조회: 사용자 {}", userId);
        
        // 사용자 존재 확인
        validateUser(userId);
        
        return notificationRepository.findByUserIdWithProduct(userId, pageable)
            .map(ProductNotificationResponseDto::from);
    }
    
    /**
     * 사용자의 활성 알림 목록 조회
     * 
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 사용자의 활성 알림 목록
     */
    public Page<ProductNotificationResponseDto> getUserActiveNotifications(Long userId, Pageable pageable) {
        log.info("사용자 활성 알림 목록 조회: 사용자 {}", userId);
        
        // 사용자 존재 확인
        validateUser(userId);
        
        return notificationRepository.findByUserIdAndIsActiveOrderByCreatedAtDesc(userId, true, pageable)
            .map(ProductNotificationResponseDto::from);
    }
    
    /**
     * 특정 상품의 알림 설정 상태 조회
     * 
     * @param userId 사용자 ID
     * @param productId 상품 ID
     * @return 알림 설정 상태
     */
    public ProductNotificationResponseDto getProductNotificationStatus(Long userId, Long productId) {
        log.info("상품 알림 설정 상태 조회: 사용자 {}, 상품 {}", userId, productId);
        
        Optional<ProductNotification> notification = notificationRepository.findByUserIdAndProductId(userId, productId);
        
        if (notification.isPresent()) {
            return ProductNotificationResponseDto.from(notification.get());
        } else {
            // 알림 설정이 없으면 기본값 반환
            return ProductNotificationResponseDto.builder()
                .userId(userId)
                .productId(productId)
                .isActive(false)
                .notificationType(ProductNotification.NotificationType.BID_UPDATE.name())
                .build();
        }
    }
    
    /**
     * 상품 알림 삭제
     * 
     * @param userId 사용자 ID
     * @param productId 상품 ID
     */
    @Transactional
    public void deleteProductNotification(Long userId, Long productId) {
        log.info("상품 알림 삭제 요청: 사용자 {}, 상품 {}", userId, productId);
        
        // 알림 설정 존재 확인
        if (!notificationRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND);
        }
        
        notificationRepository.deleteByUserIdAndProductId(userId, productId);
        
        log.info("상품 알림 삭제 완료: 사용자 {}, 상품 {}", userId, productId);
    }
    
    /**
     * 특정 상품에 알림 설정한 사용자들 조회 (입찰 알림 발송용)
     * 
     * @param productId 상품 ID
     * @param notificationType 알림 유형
     * @return 알림 설정 사용자 목록
     */
    public List<ProductNotification> getProductNotificationUsers(Long productId, ProductNotification.NotificationType notificationType) {
        log.info("상품 알림 설정 사용자들 조회: 상품 {}, 유형 {}", productId, notificationType);
        
        return notificationRepository.findByProductIdAndNotificationTypeAndIsActive(productId, notificationType, true);
    }
    
    /**
     * 사용자 존재 확인
     */
    private void validateUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }
    }
    
    /**
     * 사용자 및 상품 존재 확인
     */
    private void validateUserAndProduct(Long userId, Long productId) {
        // 사용자 존재 확인
        validateUser(userId);
        
        // 상품 존재 확인
        if (!adminProductRepository.existsById(productId)) {
            throw new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND);
        }
    }
}