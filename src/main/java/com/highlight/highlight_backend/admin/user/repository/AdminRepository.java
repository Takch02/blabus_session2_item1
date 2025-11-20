package com.highlight.highlight_backend.admin.user.repository;

import com.highlight.highlight_backend.admin.user.domain.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 관리자 Repository
 * 관리자 데이터 액세스를 위한 JPA Repository입니다.
 * 
 * @author 전우선
 * @since 2025.08.13
 */
@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {

    /**
     * 활성화된 관리자 ID로 조회
     * 
     * @param adminId 관리자 ID
     * @return 활성화된 관리자 정보 (Optional)
     */
    Optional<Admin> findByAdminIdAndIsActiveTrue(String adminId);

    
    /**
     * 관리자 ID 중복 체크
     * 
     * @param adminId 관리자 ID
     * @return 존재 여부
     */
    boolean existsByAdminId(String adminId);

}