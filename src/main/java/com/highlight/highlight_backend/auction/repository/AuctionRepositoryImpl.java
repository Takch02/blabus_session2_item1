package com.highlight.highlight_backend.auction.repository;

import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.auction.dto.AuctionSearchConditionDto;
import com.highlight.highlight_backend.product.domian.Product;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.support.PageableExecutionUtils;
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

        // 4. [Deferred Join] 실제 데이터 조회
        // ids 리스트를 그대로 쓰면 11개를 다 가져오게 됨. (확인용이니까 괜찮음)
        List<Auction> content = queryFactory
                .selectFrom(auction)
                .join(auction.product).fetchJoin()
                .where(auction.id.in(ids))
                .orderBy(getOrderSpecifiers(pageable))
                .fetch();

        // 5. hasNext 판단 로직
        boolean hasNext = false;
        if (content.size() > pageSize) {
            content.remove(pageSize); // 11번째 데이터는 확인만 하고 버림
            hasNext = true;           // 다음 페이지 있다고 표시
        }

        // 6. PageImpl 대신 SliceImpl 반환 (Count 쿼리 실행 X)
        return new SliceImpl<>(content, pageable, hasNext);
    }

    /**
     * page 로 가져올 경우 step 1 은 금방 가져오지만 step 3 에서 count에서 모든 리소스를 뺐긴다.
     * status = IN_PROGRESS 를 count 하려면 랜덤 IO를 계속하므로 여기서 리소스를 다 뺐김.
     */
    /*
    @Override
    public Page<Auction> searchAuctions(AuctionSearchConditionDto condition, Pageable pageable) {

        // [Step 1] 커버링 인덱스를 활용해 "ID만" 조회 (Deferred Join의 핵심)
        List<Long> ids = queryFactory
                .select(auction.id)
                .from(auction)
                .where(
                        eqStatus(condition.getStatus()),
                        eqCategory(condition.getCategory()),
                        eqBrand(condition.getBrand()),
                        isPremium(condition.getIsPremium()),
                        betweenPrice(condition.getMinPrice(), condition.getMaxPrice())
                )
                .orderBy(getOrderSpecifiers(pageable)) // 동적 정렬 적용
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 대상이 없으면 빈 페이지 반환 (불필요한 2차 쿼리 방지)
        if (ids.isEmpty()) {
            return Page.empty(pageable);
        }

        // [Step 2] 조회된 ID로 진짜 데이터 조회 (IN 절)
        List<Auction> content = queryFactory
                .selectFrom(auction)
                .join(auction.product).fetchJoin() // N+1 방지 (Product 같이 로딩)
                .where(auction.id.in(ids))
                .orderBy(getOrderSpecifiers(pageable)) // [중요] ID 순서 보장을 위해 정렬 다시 적용
                .fetch();

        // [Step 3] Count 쿼리 최적화 (Page 객체 필요 시)
        // (PageableExecutionUtils를 쓰면 데이터가 적을 땐 Count 쿼리 생략함)
        JPAQuery<Long> countQuery = queryFactory
                .select(auction.count())
                .from(auction)
                .where(
                        eqStatus(condition.getStatus()),
                        eqCategory(condition.getCategory()),
                        eqBrand(condition.getBrand()),
                        isPremium(condition.getIsPremium()),
                        betweenPrice(condition.getMinPrice(), condition.getMaxPrice())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

     */

    // --- [동적 쿼리 조건들 (BooleanExpression)] ---
    // null이 반환되면 QueryDSL이 알아서 조건에서 제외함 (가장 큰 장점)

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
        return isPremium != null ? auction.product.isPremium.eq(isPremium) : null;
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
                // 필요한 정렬 조건 계속 추가
                default:
                    // 기본적으로 생성일 역순
                    orders.add(new OrderSpecifier<>(Order.DESC, auction.createdAt));
            }
        }
        return orders.toArray(new OrderSpecifier[0]);
    }
}
