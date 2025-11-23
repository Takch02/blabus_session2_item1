package com.highlight.highlight_backend.user.controller;

import com.highlight.highlight_backend.common.util.ResponseUtils;
import com.highlight.highlight_backend.common.verification.PhoneVerificationRequestCodeDto;
import com.highlight.highlight_backend.common.verification.PhoneVerificationRequestDto;
import com.highlight.highlight_backend.common.config.ResponseDto;
import com.highlight.highlight_backend.user.service.UserService;
import com.highlight.highlight_backend.user.dto.UserLoginRequestDto;
import com.highlight.highlight_backend.user.dto.UserLoginResponseDto;
import com.highlight.highlight_backend.user.dto.UserSignUpRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 유저 관리 controller
 *
 * @author 탁찬홍
 * @Since 2025.08.15
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "사용자 회원가입 및 로그인", description = "사용자 회원가입, 로그인, 휴대폰 인증 API")
public class UserController {

    private final UserService userService;

    /**
     * 사용자 회원가입
     * 
     * @param signUpRequestDto 회원가입 요청 정보
     * @return 등록된 사용자 정보
     */
    @PostMapping("/signup")
    @Operation(
        summary = "사용자 회원가입", 
        description = "새로운 사용자 계정을 생성합니다. 휴대폰 인증이 선행되어야 하며, ID, 비밀번호, 휴대폰번호, 닉네임이 필요합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "회원가입 성공",
            content = @Content(schema = @Schema(implementation = ResponseDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 또는 휴대폰 인증 필요"),
        @ApiResponse(responseCode = "409", description = "이미 존재하는 사용자 ID 또는 휴대폰번호"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ResponseDto<UserSignUpRequestDto>> signUP(
            @Parameter(description = "회원가입 요청 정보 (ID, 비밀번호, 휴대폰번호, 닉네임)", required = true)
            @Valid @RequestBody UserSignUpRequestDto signUpRequestDto) {
        log.info("POST /api/public/signup - User 회원가입");
        UserSignUpRequestDto response = userService.signUp(signUpRequestDto);
        return ResponseUtils.success(response, "User 회원가입에 성공하였습니다.");
    }

    /**
     * 사용자 로그인
     * 
     * @param loginRequestDto 로그인 요청 정보
     * @return 로그인 응답 (사용자 정보 및 JWT 토큰)
     */
    @PostMapping("/login")
    @Operation(
        summary = "사용자 로그인", 
        description = "사용자 ID와 비밀번호로 로그인하여 JWT 액세스 토큰과 리프레시 토큰을 발급받습니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", 
            description = "로그인 성공",
            content = @Content(schema = @Schema(implementation = ResponseDto.class))
        ),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @ApiResponse(responseCode = "401", description = "인증 실패 - 잘못된 ID 또는 비밀번호"),
        @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ResponseDto<UserLoginResponseDto>> login(
            @Parameter(description = "로그인 요청 정보 (사용자 ID, 비밀번호)", required = true)
            @Valid @RequestBody UserLoginRequestDto loginRequestDto) {
        log.info("POST /api/public/login - User 로그인");
        UserLoginResponseDto response = userService.login(loginRequestDto);
        return ResponseEntity.ok(ResponseDto.success(response, "User 로그인에 성공하였습니다."));
    }


    @DeleteMapping("/delete")
    @Operation(
            summary = "회원 탈퇴",
            description = "JWT 토큰으로 인증된 사용자가 본인 계정을 탈퇴(비활성화)합니다. 탈퇴 후에는 로그인할 수 없으며, 개인정보는 관련 법령에 따라 처리됩니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "회원 탈퇴 성공",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패 - 유효하지 않은 토큰"),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "사용자 정보를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "진행 중인 경매가 있어 탈퇴 불가"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ResponseDto<?>> deleteUser(
            @Parameter(description = "JWT 인증 정보", hidden = true)
            Authentication authentication) {
        log.info("DELETE /api/user/ - 회원 탈퇴");
        Long userId = Long.parseLong(authentication.getPrincipal().toString());
        userService.deleteUser(userId);
        return ResponseEntity.ok(ResponseDto.success(null, "회원 탈퇴 처리가 완료되었습니다."));
    }


    /**
     * 휴대폰 인증 관련 end-point
     */
    @PostMapping("/request-phone-verification")
    @Operation(summary = "휴대폰 인증 코드 요청", description = "사용자의 휴대폰 번호로 인증 코드를 발송합니다.")
    public ResponseEntity<ResponseDto<?>> requestPhoneVerification(@Valid @RequestBody PhoneVerificationRequestCodeDto requestDto) {
        log.info("POST /api/public/request-phone-verification - 휴대폰 인증 코드 요청");
        userService.requestVerificationForSignUp(requestDto);
        return ResponseEntity.ok(ResponseDto.success(null, "인증 코드 발송에 성공하였습니다."));
    }

    @PostMapping("/verify-phone")
    @Operation(summary = "휴대폰 인증 코드 확인", description = "휴대폰 번호와 인증코드를 받아 인증 처리합니다.")
    public ResponseEntity<ResponseDto<?>> verifyPhone(@Valid @RequestBody PhoneVerificationRequestDto requestDto) {
        log.info("POST /api/public/verify-phone - 휴대폰 인증 코드 확인");
        userService.confirmVerification(requestDto);
        return ResponseEntity.ok(ResponseDto.success(null, "휴대폰 인증에 성공하였습니다."));
    }
}

    
