package com.highlight.highlight_backend.common.security;


import com.highlight.highlight_backend.common.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT 인증 필터
 * 
 * 모든 HTTP 요청에서 JWT 토큰을 검증하고 인증 정보를 설정합니다.
 * Spring Security 필터 체인에서 실행되어 요청마다 한 번씩 처리됩니다.
 * 
 * @author 전우선
 * @since 2025.08.08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    
    /**
     * JWT 토큰 검증 및 인증 처리
     * 
     * @param request HTTP 요청
     * @param response HTTP 응답
     * @param filterChain 필터 체인
     * @throws ServletException 서블릿 예외
     * @throws IOException IO 예외
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Authorization 헤더에서 토큰 추출
        String authorizationHeader = request.getHeader("Authorization");
        String token = jwtUtil.extractTokenFromBearer(authorizationHeader);
        log.info("JWT token: {}", token);
        // 토큰이 존재하고 유효한 경우 인증 처리
        if (token != null && jwtUtil.validateToken(token)) {
            try {
                // 토큰에서 사용자 정보 추출
                Long userId = jwtUtil.getUserId(token);
                String role = jwtUtil.getRole(token);

                List<GrantedAuthority> authorities = new ArrayList<>();
                if (role != null && !role.isEmpty()) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                }

                // Spring Security 인증 객체 생성
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        userId,                    // Principal (사용자 식별자)
                        null,                      // Credentials (비밀번호, JWT에서는 불필요)
                        authorities                // Authorities (권한)
                    );

                // 인증 세부 정보 설정
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // SecurityContext에 인증 정보 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("JWT authentication successful for user: {}", userId);

            } catch (Exception e) {
                log.warn("JWT authentication failed: {}", e.getMessage());
                // 인증 실패시 SecurityContext 초기화
                SecurityContextHolder.clearContext();
            }
        }

        // 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }

    /**
     * 필터 적용 여부 결정
     * 
     * 특정 경로는 JWT 검증을 건너뛸 수 있습니다.
     * 
     * @param request HTTP 요청
     * @return 필터 적용 여부 (false면 건너뜀)
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Swagger, API 문서 등은 JWT 검증 건너뜀
        return path.startsWith("/swagger-ui/") ||
               path.startsWith("/api-docs/") ||
               path.startsWith("/v3/api-docs/") ||
               path.equals("/error");
    }
}