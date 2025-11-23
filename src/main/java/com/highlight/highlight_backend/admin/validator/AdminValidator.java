package com.highlight.highlight_backend.admin.validator;

import com.highlight.highlight_backend.admin.user.domain.Admin;
import com.highlight.highlight_backend.admin.user.repository.AdminRepository;
import com.highlight.highlight_backend.exception.*;
import com.highlight.highlight_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


/**
 * refactoring 하며 새로 만든 validator
 *
 * 관리자 체크, 시간 검증, 사용자 검증, 경매 시간 검증 등
 */
@Component
@RequiredArgsConstructor
public class AdminValidator {
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


}
