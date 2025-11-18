package com.highlight.highlight_backend.dto;

import com.highlight.highlight_backend.user.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 마이페이지 응답 DTO
 * 
 * @author 탁찬홍
 * @since 2025.08.21
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "마이페이지 응답 DTO")
public class MyPageResponseDto {
    
    @Schema(description = "사용자 ID", example = "qlswmf12")
    private String userId;
    
    @Schema(description = "닉네임", example = "판다")
    private String nickname;
    
    @Schema(description = "보유 포인트", example = "32")
    private BigDecimal point;
    
    @Schema(description = "현재 등급", example = "SEED")
    private String rank;
    
    @Schema(description = "현재 등급 한글명", example = "새싹")
    private String rankDescription;
    
    @Schema(description = "경매 참여 횟수", example = "3")
    private Long participationCount;
    
    @Schema(description = "다음 등급까지 필요한 참여 횟수", example = "2")
    private Long requiredParticipationForNextRank;
    
    @Schema(description = "등급 진행률 (0-100)", example = "60")
    private Integer rankProgress;
    
    @Schema(description = "휴대폰 번호 (마스킹)", example = "010-***-1234")
    private String maskedPhoneNumber;
    
    @Schema(description = "이메일 (마스킹)", example = "qls***@naver.com")
    private String maskedEmail;
    
    /**
     * User 엔티티로부터 DTO 생성
     */
    public static MyPageResponseDto from(User user) {
        // 등급 진행률 계산
        int rankProgress = calculateRankProgress(user.getParticipationCount(), user.getRank());
        
        return MyPageResponseDto.builder()
            .userId(user.getUserId())
            .nickname(user.getNickname())
            .point(user.getPoint())
            .rank(user.getRank().name())
            .rankDescription(user.getRank().getDescription())
            .participationCount(user.getParticipationCount())
            .requiredParticipationForNextRank(user.getRequiredParticipationForNextRank())
            .rankProgress(rankProgress)
            .maskedPhoneNumber(maskPhoneNumber(user.getPhoneNumber()))
            .maskedEmail(maskEmail(user.getUserId())) // userId를 이메일로 사용
            .build();
    }
    
    /**
     * 등급 진행률 계산 (0-100)
     */
    private static int calculateRankProgress(Long participationCount, User.Rank currentRank) {
        switch (currentRank) {
            case SEED:
                return Math.min(100, (int) ((participationCount * 100) / 5));
            case Leaflet:
                return Math.min(100, (int) (((participationCount - 5) * 100) / 5));
            case Trunker:
                return Math.min(100, (int) (((participationCount - 10) * 100) / 5));
            case Flower:
                return 100; // 최고 등급
            default:
                return 0;
        }
    }
    
    /**
     * 휴대폰 번호 마스킹
     */
    private static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 10) {
            return phoneNumber;
        }
        
        String prefix = phoneNumber.substring(0, 4);
        String suffix = phoneNumber.substring(phoneNumber.length() - 4);
        return prefix + "-***-" + suffix;
    }
    
    /**
     * 이메일 마스킹
     */
    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        
        int atIndex = email.indexOf("@");
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        if (localPart.length() <= 3) {
            return localPart + "***" + domain;
        } else {
            return localPart.substring(0, 3) + "***" + domain;
        }
    }
}
