package com.highlight.highlight_backend.notification.dto;

import com.highlight.highlight_backend.notification.domain.ProductNotification;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품 알림 설정 요청 DTO
 * 
 * @author 전우선
 * @since 2025.08.16
 */
@Getter
@NoArgsConstructor
public class ProductNotificationRequestDto {
    
    /**
     * 알림 활성화 여부
     */
    @NotNull(message = "알림 활성화 여부는 필수입니다")
    private Boolean isActive;
    
    /**
     * 알림 유형 (선택사항, 기본값: BID_UPDATE)
     */
    private ProductNotification.NotificationType notificationType = ProductNotification.NotificationType.BID_UPDATE;
}