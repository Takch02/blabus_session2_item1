package com.highlight.highlight_backend.notification.dto;

import com.highlight.highlight_backend.notification.domain.ProductNotification;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 상품 알림 설정 응답 DTO
 * 
 * @author 전우선
 * @since 2025.08.16
 */
@Getter
@Builder
public class ProductNotificationResponseDto {
    
    private Long id;
    private Long userId;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private boolean isActive;
    private String notificationType;
    private String notificationTypeDescription;
    private LocalDateTime lastNotifiedAt;
    private LocalDateTime createdAt;
    
    /**
     * ProductNotification 엔티티를 DTO로 변환
     * 
     * @param notification 알림 설정 엔티티
     * @return ProductNotificationResponseDto
     */
    public static ProductNotificationResponseDto from(ProductNotification notification) {
        return ProductNotificationResponseDto.builder()
            .id(notification.getId())
            .userId(notification.getUserId())
            .productId(notification.getProductId())
            .productName(notification.getProduct() != null ? notification.getProduct().getProductName() : null)
            .productImageUrl(notification.getProduct() != null && notification.getProduct().getPrimaryImage() != null ? 
                notification.getProduct().getPrimaryImage().getImageUrl() : null)
            .isActive(notification.isActive())
            .notificationType(notification.getNotificationType().name())
            .notificationTypeDescription(notification.getNotificationType().getDescription())
            .lastNotifiedAt(notification.getLastNotifiedAt())
            .createdAt(notification.getCreatedAt())
            .build();
    }
    
    /**
     * 간단한 알림 상태만 포함하는 DTO 생성
     * (상품 목록에서 알림 설정 여부 표시용)
     * 
     * @param notification 알림 설정 엔티티
     * @return 간단한 알림 상태 DTO
     */
    public static ProductNotificationResponseDto simple(ProductNotification notification) {
        return ProductNotificationResponseDto.builder()
            .id(notification.getId())
            .productId(notification.getProductId())
            .isActive(notification.isActive())
            .notificationType(notification.getNotificationType().name())
            .build();
    }
}