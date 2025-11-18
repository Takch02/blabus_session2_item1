package com.highlight.highlight_backend.admin.vaildator;

import com.highlight.highlight_backend.admin.user.domain.Admin;
import com.highlight.highlight_backend.admin.user.repository.AdminRepository;
import com.highlight.highlight_backend.exception.AdminErrorCode;
import com.highlight.highlight_backend.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


/**
 * refactoring 하며 새로 만든 validator
 *
 * 관리자임을 체크합니다.
 */
@Component
@RequiredArgsConstructor
public class AdminValidator {
    private final AdminRepository adminRepository;

    public void validateProductManagePermission(Long adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(AdminErrorCode.ADMIN_NOT_FOUND));

        // 기획 변경: 모든 관리자가 상품 관리 가능, SUPER_ADMIN 체크만 유지
        if (admin.getRole() != Admin.AdminRole.SUPER_ADMIN && admin.getRole() != Admin.AdminRole.ADMIN) {
            throw new BusinessException(AdminErrorCode.INSUFFICIENT_PERMISSION);
        }

    }
}
