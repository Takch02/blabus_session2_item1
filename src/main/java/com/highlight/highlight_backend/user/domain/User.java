package com.highlight.highlight_backend.user.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE user SET deleted_at = NOW() WHERE id = ?")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 유저 입력
     */
    @Column(nullable = false, unique = true)
    private String userId;  // ID

    @Column(nullable = false)
    private String password;  // PW

    @Column(nullable = false, unique = true)
    private String nickname;

    @Column(nullable = false, unique = true)
    private String phoneNumber;

    @Column(nullable = false)
    private BigDecimal point = BigDecimal.valueOf(0); // 포인트

    @Enumerated(EnumType.STRING)
    @Column(name = "user_rank", nullable = false)
    private Rank rank = Rank.SEED; // 기본 등급은 SEED

    @Column(nullable = false)
    private Long participationCount = 0L; // 참여 횟수
    
    /**
     * 휴대폰 인증 여부
     */
    @Column(nullable = false)
    private boolean isPhoneVerified = false; // 기본값은 false로 설정

    private String verificationCode; // 휴대폰 인증 코드
    private LocalDateTime verificationCodeExpiresAt; // 인증 코드 만료 시간

    /**
     * 결제수단 등록
     * 
     * 이 부분은 정해지면 추후에 추가 예정
     */


    /**
     * 약관 동의
     */
    @Column(nullable = false)
    private boolean isOver14;  // 14세 이상 여부

    @Column(nullable = false)
    private boolean agreedToTerms;  // 이용약관 동의 여부

    @Column(nullable = false)
    private boolean marketingEnabled;  // 개인정보 마케팅 활용 여부

    @Column(nullable = false)
    private boolean eventSnsEnabled;  // 이벤트 광고 SNS 수신 여부

    private LocalDateTime deletedAt;

    public enum Rank {
        SEED("새싹"),
        Leaflet("잎새"),
        Trunker("줄기"),
        Flower("꽃");

        private final String description;

        Rank(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        /**
         * 참여 횟수에 따른 등급 반환
         */
        public static Rank getRankByParticipationCount(Long participationCount) {
            if (participationCount >= 0 && participationCount <= 4) {
                return SEED;
            } else if (participationCount >= 5 && participationCount <= 9) {
                return Leaflet;
            } else if (participationCount >= 10 && participationCount <= 14) {
                return Trunker;
            } else if (participationCount >= 15 && participationCount <= 20) {
                return Flower;
            } else {
                return Flower; // 20 이상은 최고 등급
            }
        }
        
        /**
         * 다음 등급까지 필요한 참여 횟수 반환
         */
        public static Long getRequiredParticipationForNextRank(Long currentParticipationCount) {
            Rank currentRank = getRankByParticipationCount(currentParticipationCount);
            
            switch (currentRank) {
                case SEED:
                    return 5L - currentParticipationCount;
                case Leaflet:
                    return 10L - currentParticipationCount;
                case Trunker:
                    return 15L - currentParticipationCount;
                case Flower:
                    return 0L; // 이미 최고 등급
                default:
                    return 0L;
            }
        }
    }
    public enum RankPercent {
        SEED("0.01"),
        Leaflet("0.02"),
        Trunker("0.03"),
        Flower("0.05");

        private String description;

        RankPercent(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public static RankPercent findByDescription(String description) {
            // values()는 enum의 모든 상수를 배열로 반환합니다. (SEED, Leaflet, ...)
            for (RankPercent rank : values()) {
                if (rank.getDescription().equals(description)) {
                    return rank; // 일치하는 값을 찾으면 해당 enum 상수를 반환
                }
            }
            return null; // 일치하는 값이 없으면 null을 반환
        }

        /**
         * 사용자 등급(Rank)에 따른 포인트 적립률 반환
         */
        public static RankPercent findByUserRank(Rank userRank) {
            switch (userRank) {
                case SEED:
                    return SEED;
                case Leaflet:
                    return Leaflet;
                case Trunker:
                    return Trunker;
                case Flower:
                    return Flower;
                default:
                    return SEED; // 기본값
            }
        }
    }
    
    /**
     * 경매 참여 시 호출되는 메서드
     * 참여 횟수를 증가시키고 등급을 업데이트
     */
    public void participateInAuction() {
        this.participationCount++;
        this.rank = Rank.getRankByParticipationCount(this.participationCount);
    }
    
    /**
     * 다음 등급까지 필요한 참여 횟수 반환
     */
    public Long getRequiredParticipationForNextRank() {
        return Rank.getRequiredParticipationForNextRank(this.participationCount);
    }

    /**
     * User 수정
     */
    public void updateProfile(
            String nickname,
            String phoneNumber,
            Boolean marketingEnabled,
            Boolean eventSnsEnabled
    ) {
        this.nickname = nickname;
        this.phoneNumber = phoneNumber;
        this.marketingEnabled = marketingEnabled;
        this.eventSnsEnabled = eventSnsEnabled;
    }
}