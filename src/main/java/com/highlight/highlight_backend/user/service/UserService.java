package com.highlight.highlight_backend.user.service;

import com.highlight.highlight_backend.common.util.JwtUtil;
import com.highlight.highlight_backend.common.verification.dto.PhoneVerificationRequestCodeDto;
import com.highlight.highlight_backend.common.verification.dto.PhoneVerificationRequestDto;
import com.highlight.highlight_backend.common.verification.service.VerificationService;
import com.highlight.highlight_backend.common.verification.domain.PhoneVerification;
import com.highlight.highlight_backend.exception.BusinessException;
import com.highlight.highlight_backend.exception.SmsErrorCode;
import com.highlight.highlight_backend.exception.UserErrorCode;
import com.highlight.highlight_backend.common.verification.repository.PhoneVerificationRepository;
import com.highlight.highlight_backend.user.dto.*;
import com.highlight.highlight_backend.user.repository.UserRepository;
import com.highlight.highlight_backend.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * User 회원가입, 로그인 기능 // 휴대폰 SMS 인증 포함
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PhoneVerificationRepository phoneVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private final VerificationService verificationService;

    /**
     * 회원가입
     */
    @Transactional
    public UserSignUpRequestDto signUp(UserSignUpRequestDto signUpRequestDto) {
        // 1. 중복 검사
        if (userRepository.existsByUserId(signUpRequestDto.getUserId())) {
            throw new BusinessException(UserErrorCode.DUPLICATE_USER_ID);
        }
        if (userRepository.existsByNickname(signUpRequestDto.getNickname())) {
            throw new BusinessException(UserErrorCode.DUPLICATE_NICKNAME);
        }
        if (userRepository.existsByPhoneNumber(signUpRequestDto.getPhoneNumber())) {
            throw new BusinessException(UserErrorCode.DUPLICATE_PHONE_NUMBER);
        }
        String phoneNumber = signUpRequestDto.getPhoneNumber();
        PhoneVerification verification = phoneVerificationRepository.findById(phoneNumber)
                .orElseThrow(() -> new BusinessException(UserErrorCode.VERIFICATION_REQUIRED)); // 인증 요청 기록 없음

        // 인증 완료 상태가 아니거나, 인증 유효 시간이 만료된 경우
        if (!verification.isVerified() || LocalDateTime.now().isAfter(verification.getExpiresAt())) {
            throw new BusinessException(SmsErrorCode.VERIFICATION_FAILED_OR_EXPIRED); // 인증 실패 또는 만료
        }

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(signUpRequestDto.getPassword());

        // 3. User 엔티티 생성 및 저장
        User user = new User();
        user.setUserId(signUpRequestDto.getUserId());
        user.setPassword(encodedPassword);
        user.setNickname(signUpRequestDto.getNickname());
        user.setPhoneNumber(signUpRequestDto.getPhoneNumber());
        user.setPhoneVerified(true); // 휴대폰 인증 완료 상태로 설정
        user.setOver14(signUpRequestDto.getIsOver14());
        user.setAgreedToTerms(signUpRequestDto.getAgreedToTerms());
        user.setMarketingEnabled(signUpRequestDto.getMarketingEnabled());
        user.setEventSnsEnabled(signUpRequestDto.getEventSnsEnabled());

        userRepository.save(user);
        return signUpRequestDto;
    }

    /**
     * 로그인
     */
    public UserLoginResponseDto login(UserLoginRequestDto loginRequestDto) {
        // 1. 사용자 조회
        User user = userRepository.findByUserId(loginRequestDto.getUserId())
                .orElseThrow(() -> new BusinessException(UserErrorCode.INVALID_LOGIN_CREDENTIALS));

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(loginRequestDto.getPassword(), user.getPassword())) {
            throw new BusinessException(UserErrorCode.INVALID_LOGIN_CREDENTIALS);
        }

        // 3. JWT 토큰 생성
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUserId(), "USER");
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUserId(), "USER");

        // 4. UserResponseDto 생성 및 반환
        return UserLoginResponseDto.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }


    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        userRepository.delete(user);
    }


    /**
     * 휴대폰 번호를 확인하고 SMS 요청을 보냄
     */
    @Transactional
    public void requestVerificationForSignUp(PhoneVerificationRequestCodeDto requestDto) {
        String phoneNumber = requestDto.getPhoneNumber();

        // 이미 가입된 번호인지 확인
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new BusinessException(UserErrorCode.PHONE_NUMBER_ALREADY_EXISTS);
        }
        // common/verification 에 위임
        verificationService.sendVerificationCode(requestDto.getPhoneNumber());

    }

    /**
     * 입력된 인증번호가 유효한지 확인하는 메소드
     */
    @Transactional
    public void confirmVerification(PhoneVerificationRequestDto requestDto) {
        verificationService.confirmVerificationCode(requestDto.getPhoneNumber(), requestDto.getVerificationCode());
    }

    @Transactional
    public UserDetailResponseDto updateUser(Long userId, UserUpdateRequestDto request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));


        // 엔티티 상태 변경
        user.updateProfile(
                request.getNickname(),
                request.getPhoneNumber(),
                request.getMarketingEnabled(),
                request.getEventSnsEnabled()
        );

        // @Transactional + Dirty Checking → save 호출 불필요
        return UserDetailResponseDto.from(user);
    }

}
