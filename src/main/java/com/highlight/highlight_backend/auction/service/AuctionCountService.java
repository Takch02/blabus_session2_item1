package com.highlight.highlight_backend.auction.service;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.auction.repository.AuctionRepository;
import com.highlight.highlight_backend.product.domian.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuctionCountService {

    private final RedisTemplate<String, String> redisTemplate;
    private final AuctionRepository auctionRepository;

    private static final String COUNT_KEY_PREFIX = "auction:count:";
    private static final long TTL_HOURS = 1; // 폴백용 TTL (AFTER_COMMIT 갱신 실패 시 자동 복구)

    // 타 service에서 사용할 메서드
    public void transition(
            Auction.AuctionStatus from,
            Auction.AuctionStatus to,
            Product.Category category) {
        if (from != null) decrement(from, category);
        if (to != null)   increment(to, category);
    }

    // 카운트 증가 — 신규 키(INCR 결과 == 1)면 TTL 설정
    public void increment(Auction.AuctionStatus status,
                          Product.Category category) {
        incrementWithTtl(statusKey(status));
        incrementWithTtl(statusCategoryKey(status, category));
    }

    // 카운트 감소
    public void decrement(Auction.AuctionStatus status,
                          Product.Category category) {
        redisTemplate.opsForValue().decrement(statusKey(status));
        redisTemplate.opsForValue().decrement(statusCategoryKey(status, category));
    }

    // 카운트 조회 (Redis 미스 시 DB 폴백 후 1시간 TTL로 캐싱)
    public long getCount(String status, String category) {
        String key = StringUtils.hasText(category)
                ? statusCategoryKey(
                    Auction.AuctionStatus.valueOf(status),
                    Product.Category.valueOf(category))
                : statusKey(Auction.AuctionStatus.valueOf(status));

        String count = redisTemplate.opsForValue().get(key);

        if (count == null) {
            long dbCount = StringUtils.hasText(category)
                    ? auctionRepository.countByStatusAndCategory(
                        Auction.AuctionStatus.valueOf(status),
                        Product.Category.valueOf(category))
                    : auctionRepository.countByStatus(
                        Auction.AuctionStatus.valueOf(status));

            redisTemplate.opsForValue()
                    .set(key, String.valueOf(dbCount), TTL_HOURS, TimeUnit.HOURS);
            return dbCount;
        }
        // DECR가 0 밑으로 내려갈 수 있어 하한 보정
        return Math.max(0, Long.parseLong(count));
    }

    public void reset(Auction.AuctionStatus status, Product.Category category) {
        redisTemplate.delete(statusKey(status));
        redisTemplate.delete(statusCategoryKey(status, category));
    }

    // INCR 결과가 1이면 키가 새로 생성된 것 → TTL 설정
    private void incrementWithTtl(String key) {
        Long result = redisTemplate.opsForValue().increment(key);
        if (Long.valueOf(1).equals(result)) {
            redisTemplate.expire(key, TTL_HOURS, TimeUnit.HOURS);
        }
    }

    private String statusKey(Auction.AuctionStatus status) {
        return COUNT_KEY_PREFIX + status.name();
    }

    private String statusCategoryKey(Auction.AuctionStatus status,
                                     Product.Category category) {
        return COUNT_KEY_PREFIX + status.name()
                + ":" + category.name();
    }
}
