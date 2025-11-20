package com.highlight.highlight_backend.admin.validator;

import com.highlight.highlight_backend.admin.product.domian.Product;
import com.highlight.highlight_backend.admin.user.domain.Admin;
import com.highlight.highlight_backend.admin.user.repository.AdminRepository;
import com.highlight.highlight_backend.exception.*;
import com.highlight.highlight_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;


/**
 * refactoring 하며 새로 만든 validator
 *
 * 관리자 체크, 시간 검증, 사용자 검증, 경매 시간 검증 등
 */
@Component
@RequiredArgsConstructor
public class CommonValidator {
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;


    /**
     * 관리자 검증 메소드
     */
    public void validateManagePermission(Long adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(AdminErrorCode.ADMIN_NOT_FOUND));

        // 기획 변경: 모든 관리자가 상품 관리 가능, SUPER_ADMIN 체크만 유지
        if (admin.getRole() != Admin.AdminRole.SUPER_ADMIN && admin.getRole() != Admin.AdminRole.ADMIN) {
            throw new BusinessException(AdminErrorCode.INSUFFICIENT_PERMISSION);
        }

    }


    /**
     * UTC 시간을 한국 시간(KST)으로 변환
     *
     * @param utcTime UTC 시간 (예: 2025-08-22T06:30:00.000Z)
     * @return 한국 시간
     */
    public LocalDateTime convertUTCToKST(LocalDateTime utcTime) {
        if (utcTime == null) {
            return null;
        }

        // UTC로 가정하고 ZonedDateTime으로 변환
        ZonedDateTime utcZoned = utcTime.atZone(ZoneId.of("UTC"));

        // 한국 시간대로 변환 (UTC+9)
        ZonedDateTime kstZoned = utcZoned.withZoneSameInstant(ZoneId.of("Asia/Seoul"));

        return kstZoned.toLocalDateTime();
    }

    /**
     * 경매 시간 검증
     *
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     */
    public void validateAuctionTime(LocalDateTime startTime, LocalDateTime endTime) {
        // 한국 시간 기준으로 현재 시간 가져오기
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));

        // 시작 시간이 현재 시간보다 이전일 수 없음 (5분 여유)
        if (startTime.isBefore(now.minusMinutes(5))) {
            throw new BusinessException(AuctionErrorCode.INVALID_AUCTION_START_TIME);
        }

        // 종료 시간이 시작 시간보다 이전일 수 없음
        if (endTime.isBefore(startTime)) {
            throw new BusinessException(AuctionErrorCode.INVALID_AUCTION_END_TIME);
        }

        // 경매 시간이 너무 짧으면 안됨 (최소 10분)
        if (ChronoUnit.MINUTES.between(startTime, endTime) < 10) {
            throw new BusinessException(AuctionErrorCode.AUCTION_DURATION_TOO_SHORT);
        }
    }

    /**
     * 사용자 존재 여부 확인
     */
    public void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }
    }

    /**
     * 즉시구매가 설정 시 상품 개수 검증
     *
     * @param product 상품 정보
     * @param buyItNowPrice 즉시구매가
     */
    public void validateBuyItNowProductCount(Product product, java.math.BigDecimal buyItNowPrice) {
        // 즉시구매가가 설정된 경우에만 검증
        if (buyItNowPrice != null && buyItNowPrice.compareTo(java.math.BigDecimal.ZERO) > 0) {
            if (product.getProductCount() != 1) {
                throw new BusinessException(AuctionErrorCode.BUY_IT_NOW_ONLY_FOR_SINGLE_ITEM);
            }
        }
    }
}
