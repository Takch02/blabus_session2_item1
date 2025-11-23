package com.highlight.highlight_backend.admin.service;

import com.highlight.highlight_backend.admin.dto.AdminSignUpRequestDto;
import com.highlight.highlight_backend.common.util.JwtUtil;
import com.highlight.highlight_backend.admin.domain.Admin;
import com.highlight.highlight_backend.admin.dto.LoginRequestDto;
import com.highlight.highlight_backend.admin.dto.LoginResponseDto;
import com.highlight.highlight_backend.exception.BusinessException;
import com.highlight.highlight_backend.exception.AdminErrorCode;
import com.highlight.highlight_backend.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 인증 서비스
 * 
 * 백오피스 관리자 로그인 및 인증 처리를 담당하는 서비스입니다.
 * 
 * @author 전우선
 * @since 2025.08.13
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAuthService {
    
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    
    /**
     * 관리자 로그인 처리
     * 
     * @param request 로그인 요청 정보
     * @return 로그인 응답 정보 (JWT 토큰 포함)
     */
    @Transactional
    public LoginResponseDto login(LoginRequestDto request) {
        log.info("관리자 로그인 시도: {}", request.getAdminId());
        
        // 1. 관리자 계정 조회
        Admin admin = adminRepository.findByAdminIdAndIsActiveTrue(request.getAdminId())
            .orElseThrow(() -> {
                log.warn("로그인 실패 - 존재하지 않는 관리자 ID 또는 비활성화된 계정");
                return new BusinessException(AdminErrorCode.INVALID_LOGIN_CREDENTIALS);
            });
        
        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            log.warn("로그인 실패 - 잘못된 비밀번호");
            throw new BusinessException(AdminErrorCode.INVALID_LOGIN_CREDENTIALS);
        }
        
        // 3. 마지막 로그인 시간 업데이트
        admin.setLastLoginAt(LocalDateTime.now());
        adminRepository.save(admin);

        Admin.AdminRole role  = admin.getRole();
        String accessToken = "";
        String refreshToken = "";
        if (role == Admin.AdminRole.ADMIN) {
            accessToken = jwtUtil.generateAccessToken(admin.getId(), admin.getEmail(), "ADMIN");
            refreshToken = jwtUtil.generateRefreshToken(admin.getId(), admin.getEmail(), "ADMIN");
        }
        else if (role == Admin.AdminRole.SUPER_ADMIN) {
            accessToken = jwtUtil.generateAccessToken(admin.getId(), admin.getEmail(), "SUPER_ADMIN");
            refreshToken = jwtUtil.generateRefreshToken(admin.getId(), admin.getEmail(), "SUPER_ADMIN");
        }
        
        log.info("관리자 로그인 성공: {} (ID: {})", admin.getAdminName(), admin.getAdminId());
        
        return new LoginResponseDto(
            accessToken,
            refreshToken,
            admin.getAdminId(),
            admin.getAdminName(),
            "로그인이 성공적으로 완료되었습니다."
        );
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
        newAdmin.setPassword(passwordEncoder.encode(signUpRequestDto.getPassword()));
        newAdmin.setFirstAdminDetail(signUpRequestDto, newAdmin);
        Admin savedAdmin = adminRepository.save(newAdmin);

        log.info("관리자 간단 회원가입 완료: {} (ID: {})", savedAdmin.getAdminName(), savedAdmin.getId());
    }


    /**
     * 토큰 유효성 검증
     * 
     * @param token JWT 토큰
     * @return 유효성 여부
     */
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

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