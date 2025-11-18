package com.highlight.highlight_backend.user;

import com.highlight.highlight_backend.common.util.JwtUtil;
import com.highlight.highlight_backend.common.verification.VerificationService;
import com.highlight.highlight_backend.domain.PhoneVerification;
import com.highlight.highlight_backend.repository.PhoneVerificationRepository;
import com.highlight.highlight_backend.repository.user.UserRepository;
import com.highlight.highlight_backend.user.dto.UserLoginRequestDto;
import com.highlight.highlight_backend.user.dto.UserLoginResponseDto;
import com.highlight.highlight_backend.user.dto.UserSignUpRequestDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PhoneVerificationRepository phoneVerificationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private VerificationService verificationService;

    /**
     * signUp 테스트
     */
    @Test
    void signUp () {
        UserSignUpRequestDto  userSignUpRequestDto = new UserSignUpRequestDto();
        userSignUpRequestDto.setUserId("tag22kr");
        userSignUpRequestDto.setPassword("tagtag1010");
        userSignUpRequestDto.setNickname("chan");
        userSignUpRequestDto.setPhoneNumber("01039375008");
        userSignUpRequestDto.setIsOver14(true);
        userSignUpRequestDto.setAgreedToTerms(true);
        userSignUpRequestDto.setMarketingEnabled(true);
        userSignUpRequestDto.setEventSnsEnabled(true);

        // 중복은 없다 가정
        when(userRepository.existsByUserId(userSignUpRequestDto.getUserId())).thenReturn(false);
        when(userRepository.existsByNickname(userSignUpRequestDto.getNickname())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(userSignUpRequestDto.getPhoneNumber())).thenReturn(false);

        // 인증했다 가정
        PhoneVerification phoneVerification = new PhoneVerification();
        phoneVerification.setVerificationCode("123456");
        phoneVerification.setPhoneNumber("01039375008");
        phoneVerification.setVerified(true);
        phoneVerification.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        // ID를 찾았다 가정
        when(phoneVerificationRepository.findById("01039375008")).thenReturn(Optional.of(phoneVerification));

        when(passwordEncoder.encode(userSignUpRequestDto.getPassword())).thenReturn("Tagtag1010@");

        UserSignUpRequestDto result = userService.signUp(userSignUpRequestDto);

        assertThat(result.getUserId()).isEqualTo("tag22kr");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void login() {
        UserLoginRequestDto dto = new UserLoginRequestDto("tag22kr", "tagtag1010");

        User user = new User();
        user.setUserId("tag22kr");
        when(userRepository.findByUserId(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(any(), any())).thenReturn(true); // 비밀번호 검증은 패스

        UserLoginResponseDto result = userService.login(dto);

        assertThat(result.getUserId()).isEqualTo("tag22kr");
    }
}
