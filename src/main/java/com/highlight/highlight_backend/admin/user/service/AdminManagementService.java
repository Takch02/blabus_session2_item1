package com.highlight.highlight_backend.admin.user.service;

import com.highlight.highlight_backend.admin.user.domain.Admin;
import com.highlight.highlight_backend.admin.user.dto.AdminCreateRequestDto;
import com.highlight.highlight_backend.admin.user.dto.AdminResponseDto;
import com.highlight.highlight_backend.admin.user.dto.AdminSignUpRequestDto;
import com.highlight.highlight_backend.exception.BusinessException;
import com.highlight.highlight_backend.exception.AdminErrorCode;
import com.highlight.highlight_backend.admin.user.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 계정 관리 서비스
 * 
 * SUPER_ADMIN 권한을 가진 관리자가 다른 관리자 계정을 생성/수정/삭제할 수 있는 기능을 제공합니다.
 * 
 * @author 전우선
 * @since 2025.08.13
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminManagementService {
    
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * 관리자 계정 생성
     * 
     * @param request 계정 생성 요청 데이터
     * @param currentAdminId 현재 로그인한 관리자 ID (권한 확인용)
     * @return 생성된 관리자 정보
     */
    @Transactional
    public AdminResponseDto createAdmin(AdminCreateRequestDto request, Long currentAdminId) {
        log.info("관리자 계정 생성 요청: {} (요청자: {})", request.getAdminId(), currentAdminId);
        
        // 1. 현재 로그인한 관리자 권한 확인
        Admin currentAdmin = validateSuperAdminPermission(currentAdminId);
        
        // 2. 중복 검사
        if (adminRepository.existsByAdminId(request.getAdminId())) {
            throw new BusinessException(AdminErrorCode.DUPLICATE_ADMIN_ID);
        }
        
        if (adminRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(AdminErrorCode.DUPLICATE_EMAIL);
        }
        
        // 3. 새 관리자 계정 생성
        Admin newAdmin = new Admin();
        newAdmin.setAdminId(request.getAdminId());
        newAdmin.setPassword(passwordEncoder.encode(request.getPassword()));
        newAdmin.setAdminName(request.getAdminName());
        newAdmin.setEmail(request.getEmail());
        newAdmin.setRole(Admin.AdminRole.ADMIN); // 기본적으로 일반 관리자
        newAdmin.setActive(true);
        
        
        Admin savedAdmin = adminRepository.save(newAdmin);
        
        log.info("관리자 계정 생성 완료: {} (ID: {})", savedAdmin.getAdminName(), savedAdmin.getId());
        
        return AdminResponseDto.from(savedAdmin);
    }
    
    /**
     * 관리자 계정 삭제
     * 
     * @param adminId 삭제할 관리자 ID
     * @param currentAdminId 현재 로그인한 관리자 ID
     */
    @Transactional
    public void deleteAdmin(Long adminId, Long currentAdminId) {
        log.info("관리자 계정 삭제 요청: {} (요청자: {})", adminId, currentAdminId);
        
        // 1. 현재 로그인한 관리자 권한 확인
        validateSuperAdminPermission(currentAdminId);
        
        // 2. 본인 계정은 삭제 불가
        if (adminId.equals(currentAdminId)) {
            throw new BusinessException(AdminErrorCode.CANNOT_DELETE_SELF);
        }
        
        // 3. 삭제할 관리자 조회
        Admin admin = adminRepository.findById(adminId)
            .orElseThrow(() -> new BusinessException(AdminErrorCode.ADMIN_NOT_FOUND));
        
        // 4. 삭제 처리
        adminRepository.delete(admin);
        
        log.info("관리자 계정 삭제 완료: {} (ID: {})", admin.getAdminName(), admin.getId());
    }
    
    /**
     * 관리자 목록 조회
     * 
     * @param pageable 페이징 정보
     * @param currentAdminId 현재 로그인한 관리자 ID
     * @return 관리자 목록
     */
    public Page<AdminResponseDto> getAdminList(Pageable pageable, Long currentAdminId) {
        log.info("관리자 목록 조회 요청 (요청자: {})", currentAdminId);
        
        // 현재 로그인한 관리자 권한 확인
        validateSuperAdminPermission(currentAdminId);
        
        return adminRepository.findAll(pageable)
            .map(AdminResponseDto::from);
    }
    
    /**
     * 관리자 상세 조회
     * 
     * @param adminId 조회할 관리자 ID
     * @param currentAdminId 현재 로그인한 관리자 ID
     * @return 관리자 상세 정보
     */
    public AdminResponseDto getAdmin(Long adminId, Long currentAdminId) {
        log.info("관리자 상세 조회 요청: {} (요청자: {})", adminId, currentAdminId);
        
        // 현재 로그인한 관리자 권한 확인
        validateSuperAdminPermission(currentAdminId);
        
        Admin admin = adminRepository.findById(adminId)
            .orElseThrow(() -> new BusinessException(AdminErrorCode.ADMIN_NOT_FOUND));
        
        return AdminResponseDto.from(admin);
    }
    
    /**
     * 관리자 간단 회원가입 (ID, 비밀번호만)
     * 
     * @param signUpRequestDto 간단 회원가입 요청 데이터
     */
    @Transactional
    public void simpleSignUp(AdminSignUpRequestDto signUpRequestDto) {
        log.info("관리자 간단 회원가입 요청: {}", signUpRequestDto.getAdminId());
        
        // 1. 중복 검사
        if (adminRepository.existsByAdminId(signUpRequestDto.getAdminId())) {
            throw new BusinessException(AdminErrorCode.DUPLICATE_ADMIN_ID);
        }
        
        // 2. 새 관리자 계정 생성
        Admin newAdmin = new Admin();
        newAdmin.setAdminId(signUpRequestDto.getAdminId());
        newAdmin.setPassword(passwordEncoder.encode(signUpRequestDto.getPassword()));
        newAdmin.setAdminName(signUpRequestDto.getAdminId()); // 이름은 ID와 동일하게 설정
        newAdmin.setEmail(signUpRequestDto.getAdminId() + "@admin.com"); // 임시 이메일
        newAdmin.setRole(Admin.AdminRole.ADMIN); // 기본적으로 일반 관리자
        newAdmin.setActive(true);
        
        Admin savedAdmin = adminRepository.save(newAdmin);
        
        log.info("관리자 간단 회원가입 완료: {} (ID: {})", savedAdmin.getAdminName(), savedAdmin.getId());
    }

    /**
     * SUPER_ADMIN 권한 검증
     * 
     * @param adminId 검증할 관리자 ID
     * @return 검증된 관리자 정보
     */
    private Admin validateSuperAdminPermission(Long adminId) {
        Admin admin = adminRepository.findById(adminId)
            .orElseThrow(() -> new BusinessException(AdminErrorCode.ADMIN_NOT_FOUND));
        
        if (admin.getRole() != Admin.AdminRole.SUPER_ADMIN) {
            throw new BusinessException(AdminErrorCode.INSUFFICIENT_PERMISSION);
        }
        
        return admin;
    }
}