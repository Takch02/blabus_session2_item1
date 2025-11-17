package com.highlight.highlight_backend.common.verification;

import com.highlight.highlight_backend.domain.PhoneVerification;
import com.highlight.highlight_backend.exception.BusinessException;
import com.highlight.highlight_backend.exception.SmsErrorCode;
import com.highlight.highlight_backend.exception.UserErrorCode;
import com.highlight.highlight_backend.repository.PhoneVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.nurigo.java_sdk.api.Message;
import net.nurigo.java_sdk.exceptions.CoolsmsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Random;

/**
 * 회원의 휴대폰 인증을 위한 service
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class VerificationService {

    private final PhoneVerificationRepository phoneVerificationRepository;

    @Value("${coolsms.api.key}")
    private String apiKey;

    @Value("${coolsms.api.secret}")
    private String apiSecret;

    @Value("${coolsms.from-number}")
    private String fromPhoneNumber;

    /**
     * 휴대폰 번호를 확인하고 SMS 요청을 보냄
     */
    @Transactional
    public void sendVerificationCode(String phoneNumber) {

        String verificationCode = createRandomCode();
        // DB에 인증번호와 만료 시간 저장 (기존에 있어도 덮어쓰기)
        PhoneVerification verification = new PhoneVerification(phoneNumber, verificationCode);
        phoneVerificationRepository.save(verification);

        // SMS 발송 로직 (수정: user 객체 대신 phoneNumber 변수 사용)
        Message coolsms = new Message(apiKey, apiSecret);
        HashMap<String, String> params = new HashMap<>();
        params.put("to", phoneNumber);
        params.put("from", fromPhoneNumber);
        params.put("type", "SMS");
        params.put("text", "nafal 회원가입 인증번호 " + verificationCode + " 를 입력해주세요.");

        try {
            coolsms.send(params);
        } catch (CoolsmsException e) {
            log.error("SMS 발송 실패: {}", e.getMessage());
            throw new BusinessException(SmsErrorCode.SMS_SEND_FAILED);
        }

        log.info("회원가입용 인증번호 발송: {}", phoneNumber);
    }

    /**
     * 입력된 인증번호가 유효한지 확인하는 메소드
     */
    @Transactional
    public void confirmVerificationCode(String phoneNumber, String verificationCode) {
        // DB에서 휴대폰 번호로 인증 정보를 조회
        PhoneVerification verification = phoneVerificationRepository.findById(phoneNumber)
                .orElseThrow(() -> new BusinessException(SmsErrorCode.VERIFICATION_CODE_NOT_FOUND));

        // 만료 시간 확인
        if (LocalDateTime.now().isAfter(verification.getExpiresAt())) {
            throw new BusinessException(SmsErrorCode.VERIFICATION_CODE_EXPIRED);
        }

        // 인증번호 일치 여부 확인
        if (!verification.getVerificationCode().equals(verificationCode) {
            throw new BusinessException(UserErrorCode.VERIFICATION_CODE_NOT_MATCH);
        }

        // 인증 성공 시, verified 상태를 true로 변경하고 저장
        verification.setVerified(true);
        phoneVerificationRepository.save(verification);
    }

    private String createRandomCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 100000 ~ 999999
        return String.valueOf(code);
    }
}
