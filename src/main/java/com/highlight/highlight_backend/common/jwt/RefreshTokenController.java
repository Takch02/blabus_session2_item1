package com.highlight.highlight_backend.common.jwt;

import com.highlight.highlight_backend.common.util.JwtUtil;
import com.highlight.highlight_backend.common.config.ResponseDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 토큰 갱신 컨트롤러
 *
 * @author 탁찬홍
 * @since 2025.08.23
 */
@Slf4j
@RestController
@RequestMapping("/api/public/refresh")
@RequiredArgsConstructor
@Tag(name = "토큰 갱신", description = "RefreshToken으로 AccessToken 갱신 API")
public class RefreshTokenController {

    private final JwtUtil jwtUtil;

    @PostMapping
    public ResponseEntity<ResponseDto<RefreshTokenResponseDto>> refreshToken(
            @RequestBody RefreshTokenRequestDto requestDto) {

        try {
            String refreshToken = requestDto.getRefreshToken();

            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ResponseDto.error("Refresh token이 필요합니다."));
            }

            Boolean isValid = jwtUtil.validateToken(refreshToken);

            if (isValid) {
                RefreshTokenResponseDto responseDto = new RefreshTokenResponseDto();

                Long id = jwtUtil.getUserId(refreshToken);
                String email = jwtUtil.getEmail(refreshToken);
                String role = jwtUtil.getRole(refreshToken);

                // 새로운 AccessToken 생성
                String newAccessToken = jwtUtil.generateAccessToken(id, email, role);
                // 새로운 RefreshToken 생성 (선택사항 - 보안상 권장)
                String newRefreshToken = jwtUtil.generateRefreshToken(id, email, role);

                responseDto.setAccessToken(newAccessToken);
                responseDto.setRefreshToken(newRefreshToken);
                responseDto.setAdminId(id.toString());
                responseDto.setAdminName(email); // 또는 실제 사용자 이름
                responseDto.setMessage("토큰 갱신 성공!");

                log.info("토큰 갱신 성공 - 사용자 ID: {}", id);
                return ResponseEntity.ok(ResponseDto.success(responseDto, "토큰 갱신 성공!"));
            } else {
                log.warn("유효하지 않은 refresh token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseDto.error("유효하지 않은 refresh token입니다."));
            }
        } catch (Exception e) {
            log.error("토큰 갱신 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.error("토큰 갱신 중 오류가 발생했습니다."));
        }
    }
}
