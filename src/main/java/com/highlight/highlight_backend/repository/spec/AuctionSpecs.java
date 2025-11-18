package com.highlight.highlight_backend.repository.spec;

import com.highlight.highlight_backend.admin.auction.domain.Auction;
import com.highlight.highlight_backend.admin.product.domian.Product;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Join;
import java.time.LocalDateTime;

public class AuctionSpecs {

    // 카테고리 필터
    public static Specification<Auction> hasCategory(String category) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(category)) {
                return null; // 조건 값이 없으면 무시
            }
            Join<Auction, Product> productJoin = root.join("product"); // Auction -> Product 조인
            return criteriaBuilder.equal(productJoin.get("category"), category);
        };
    }

    // 브랜드 필터
    public static Specification<Auction> hasBrand(String brand) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(brand)) {
                return null;
            }
            Join<Auction, Product> productJoin = root.join("product");
            return criteriaBuilder.equal(productJoin.get("brandName"), brand);
        };
    }

    // 행사명 필터 (Auction의 title에 포함되는지 검색)
    public static Specification<Auction> hasEventName(String eventName) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(eventName)) {
                return null;
            }
            return criteriaBuilder.like(root.get("auctionTitle"), "%" + eventName + "%");
        };
    }

    // 가격 범위 필터
    public static Specification<Auction> betweenPrice(Long minPrice, Long maxPrice) {
        return (root, query, criteriaBuilder) -> {
            if (minPrice == null && maxPrice == null) {
                return null;
            }
            if (minPrice != null && maxPrice != null) {
                return criteriaBuilder.between(root.get("currentHighestBid"), minPrice, maxPrice);
            }
            if (minPrice != null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("currentHighestBid"), minPrice);
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("currentHighestBid"), maxPrice);
        };
    }

    // 프리미엄 상품 필터
    public static Specification<Auction> isPremium(Boolean isPremium) {
        return (root, query, criteriaBuilder) -> {
            if (isPremium == null) {
                return null;
            }
            Join<Auction, Product> productJoin = root.join("product");
            return criteriaBuilder.equal(productJoin.get("isPremium"), isPremium);
        };
    }
    
    // 경매 상태 필터 (진행중/예정/마감임박)
    public static Specification<Auction> hasAuctionStatus(String status) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(status)) {
                return null;
            }
            
            LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
            
            switch (status.toUpperCase()) {
                case "IN_PROGRESS":
                    // 진행중: 상태가 IN_PROGRESS이고 종료시간이 1시간 이후
                    return criteriaBuilder.and(
                        criteriaBuilder.equal(root.get("status"), Auction.AuctionStatus.IN_PROGRESS)

                    );
                    
                case "SCHEDULED":
                    // 예정: 상태가 SCHEDULED
                    return criteriaBuilder.equal(root.get("status"), Auction.AuctionStatus.SCHEDULED);
                    
                case "ENDING_SOON":
                    // 마감임박: 상태가 IN_PROGRESS이고 종료시간이 1시간 이내
                    return criteriaBuilder.and(
                        criteriaBuilder.equal(root.get("status"), Auction.AuctionStatus.IN_PROGRESS),
                        criteriaBuilder.between(root.get("scheduledEndTime"), now, now.plusHours(1))
                    );
                    
                default:
                    return null;
            }
        };
    }

}
