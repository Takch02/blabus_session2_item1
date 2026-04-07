package com.highlight.highlight_backend.auction.service;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.auction.repository.AuctionRepository;
import com.highlight.highlight_backend.product.domian.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionCountService {

    private final RedisTemplate<String, String> redisTemplate;
    private final AuctionRepository auctionRepository;

    private static final String COUNT_KEY_PREFIX = "auction:count:";

    // 타 service에서 사용할 메서드
    public void transition(
            Auction.AuctionStatus from,
            Auction.AuctionStatus to,
            Product.Category category) {
        if (from != null) decrement(from, category);
        if (to != null)   increment(to, category);
    }

    // 카운트 증가
    public void increment(Auction.AuctionStatus status,
                          Product.Category category) {
        redisTemplate.opsForValue()
                .increment(statusKey(status));
        redisTemplate.opsForValue()
                .increment(statusCategoryKey(status, category));
    }

    // 카운트 감소
    public void decrement(Auction.AuctionStatus status, 
                         Product.Category category) {
        redisTemplate.opsForValue()
                .decrement(statusKey(status));
        redisTemplate.opsForValue()
                .decrement(statusCategoryKey(status, category));
    }

    // 카운트 조회 (Redis 미스 시 DB 폴백)
    public long getCount(String status, String category) {
        String key = StringUtils.hasText(category)
                ? statusCategoryKey(
                    Auction.AuctionStatus.valueOf(status),
                    Product.Category.valueOf(category))
                : statusKey(Auction.AuctionStatus.valueOf(status));

        String count = redisTemplate.opsForValue().get(key);

        if (count == null) {
            // Redis 미스 → DB에서 조회 후 캐싱
            long dbCount = StringUtils.hasText(category)
                    ? auctionRepository.countByStatusAndCategory(
                        Auction.AuctionStatus.valueOf(status),
                        Product.Category.valueOf(category))
                    : auctionRepository.countByStatus(
                        Auction.AuctionStatus.valueOf(status));

            redisTemplate.opsForValue()
                    .set(key, String.valueOf(dbCount));
            return dbCount;
        }
        return Long.parseLong(count);
    }

    // 키 생성
    private String statusKey(Auction.AuctionStatus status) {
        return COUNT_KEY_PREFIX + status.name();
    }

    private String statusCategoryKey(Auction.AuctionStatus status,
                                     Product.Category category) {
        return COUNT_KEY_PREFIX + status.name() 
                + ":" + category.name();
    }

    // 매일 새벽 3시 DB와 동기화 (정합성 보장)
    @Scheduled(cron = "0 0 3 * * *")
    public void syncCountCache() {
        for (Auction.AuctionStatus status : Auction.AuctionStatus.values()) {
            long statusCount = auctionRepository.countByStatus(status);
            redisTemplate.opsForValue()
                    .set(statusKey(status), String.valueOf(statusCount));

            for (Product.Category category : Product.Category.values()) {
                long count = auctionRepository
                        .countByStatusAndCategory(status, category);
                redisTemplate.opsForValue()
                        .set(statusCategoryKey(status, category),
                             String.valueOf(count));
            }
        }
        log.info("경매 카운트 캐시 동기화 완료");
    }

    public void reset(Auction.AuctionStatus status, Product.Category category) {
        redisTemplate.delete(statusKey(status));
        redisTemplate.delete(statusCategoryKey(status, category));
    }
}