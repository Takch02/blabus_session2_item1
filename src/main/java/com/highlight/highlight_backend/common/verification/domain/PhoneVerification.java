package com.highlight.highlight_backend.common.verification.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class PhoneVerification {

    @Id
    private String phoneNumber; // 휴대폰 번호를 PK로 사용

    private String verificationCode;

    private LocalDateTime expiresAt; // 만료 시간

    private boolean verified; // 인증 여부

    public PhoneVerification(String phoneNumber, String verificationCode) {
        this.phoneNumber = phoneNumber;
        this.verificationCode = verificationCode;
        this.expiresAt = LocalDateTime.now().plusMinutes(3); // 3분 후 만료
        this.verified = false; // 초기값은 false
    }
}
