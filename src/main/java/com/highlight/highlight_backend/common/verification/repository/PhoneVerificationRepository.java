package com.highlight.highlight_backend.common.verification.repository;

import com.highlight.highlight_backend.common.verification.domain.PhoneVerification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, String> {
}
