package com.highlight.highlight_backend.auction.repository;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.auction.dto.AuctionSearchConditionDto;
import com.highlight.highlight_backend.product.domian.Product;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.highlight.highlight_backend.auction.domain.QAuction.auction;

@RequiredArgsConstructor
public class AuctionRepositoryImpl implements AuctionRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    /**
     * Slice 를 이용하여 count 하지 않고 사용자가 10개를 원한다면 11개 까지 탐색 후 다음 페이지가 있다는 정보만 넘기고 끝냄.
     */
    @Override
    public Slice<Auction> searchAuctions(AuctionSearchConditionDto condition, Pageable pageable) {

        // 1. 페이지 사이즈보다 1개 더 조회 (Limit + 1)
        int pageSize = pageable.getPageSize();

        // 2. [커버링 인덱스] ID 조회 (Limit를 pageSize + 1로 설정)
        List<Long> ids = queryFactory
                .select(auction.id)
                .from(auction)
                .where(
                        eqStatus(condition.getStatus()),
                        eqCategory(condition.getCategory()),
                        isPremium(condition.getIsPremium()),
                        betweenPrice(condition.getMinPrice(), condition.getMaxPrice())
                )
                .orderBy(getOrderSpecifiers(pageable))
                .offset(pageable.getOffset())
                .limit(pageSize + 1) // <--- 핵심! 1개 더 가져옴
                .fetch();

        // 3. 데이터가 없으면 빈 Slice 반환
        if (ids.isEmpty()) {
            return new SliceImpl<>(Collections.emptyList(), pageable, false);
        }


        // 4. hasNext 판단 로직
        boolean hasNext = ids.size() > pageSize;
        if (hasNext) {
            ids = ids.subList(0, pageSize); // 11번째 제거 후 데이터 조회
        }

        // 5. [Deferred Join] 실제 데이터 조회
        // ids 리스트를 그대로 쓰면 11개를 다 가져오게 됨.
        List<Auction> content = queryFactory
                .selectFrom(auction)
                .join(auction.product).fetchJoin()
                .where(auction.id.in(ids))
                .orderBy(getOrderSpecifiers(pageable))
                .fetch();

        // 6. PageImpl 대신 SliceImpl 반환 (Count 쿼리 실행 X)
        return new SliceImpl<>(content, pageable, hasNext);
    }

    private BooleanExpression eqStatus(String status) {
        return StringUtils.hasText(status) ? auction.status.eq(Auction.AuctionStatus.valueOf(status)) : null;
    }

    private BooleanExpression eqCategory(String category) {
        return StringUtils.hasText(category) ? auction.category.eq(Product.Category.valueOf(category)) : null;
    }

    private BooleanExpression eqBrand(String brand) {
        // 브랜드는 정확한 일치인지, 포함인지 정책에 따라 eq() 또는 contains() 사용
        return StringUtils.hasText(brand) ? auction.product.brand.eq(brand) : null;
    }

    private BooleanExpression isPremium(Boolean isPremium) {
        return isPremium != null ? auction.isPremium.eq(isPremium) : null;
    }

    private BooleanExpression betweenPrice(Long minPrice, Long maxPrice) {
        if (minPrice != null && maxPrice != null) {
            return auction.currentHighestBid.between(BigDecimal.valueOf(minPrice), BigDecimal.valueOf(maxPrice));
        }
        if (minPrice != null) {
            return auction.currentHighestBid.goe(BigDecimal.valueOf(minPrice));
        }
        if (maxPrice != null) {
            return auction.currentHighestBid.loe(BigDecimal.valueOf(maxPrice));
        }
        return null;
    }

    // --- [동적 정렬 처리] ---
    private OrderSpecifier<?>[] getOrderSpecifiers(Pageable pageable) {
        if (pageable.getSort().isEmpty()) {
            return new OrderSpecifier[]{auction.createdAt.desc()}; // 기본 정렬
        }

        List<OrderSpecifier<?>> orders = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            Order direction = order.getDirection().isAscending() ? Order.ASC : Order.DESC;

            switch (order.getProperty()) {
                case "newest":
                    orders.add(new OrderSpecifier<>(direction, auction.actualStartTime));
                    break;
                case "currentHighestBid":
                    orders.add(new OrderSpecifier<>(direction, auction.currentHighestBid));
                    break;
                default:
                    // 기본적으로 최신순
                    orders.add(new OrderSpecifier<>(Order.DESC, auction.createdAt));
            }
        }
        return orders.toArray(new OrderSpecifier[0]);
    }
}
