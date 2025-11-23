package com.highlight.highlight_backend.auction.validator;

import com.highlight.highlight_backend.product.domian.Product;
import com.highlight.highlight_backend.auction.domain.Auction;
import com.highlight.highlight_backend.exception.AuctionErrorCode;
import com.highlight.highlight_backend.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class AuctionValidator {

    /**
     * 경매 시간 검증
     *
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     */
    public void validateAuctionTime(LocalDateTime startTime, LocalDateTime endTime) {
        // 한국 시간 기준으로 현재 시간 가져오기
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));

        // 시작 시간이 현재 시간보다 이전일 수 없음 (5분 여유)
        if (startTime.isBefore(now.minusMinutes(5))) {
            throw new BusinessException(AuctionErrorCode.INVALID_AUCTION_START_TIME);
        }

        // 종료 시간이 시작 시간보다 이전일 수 없음
        if (endTime.isBefore(startTime)) {
            throw new BusinessException(AuctionErrorCode.INVALID_AUCTION_END_TIME);
        }

        // 경매 시간이 너무 짧으면 안됨 (최소 10분)
        if (ChronoUnit.MINUTES.between(startTime, endTime) < 10) {
            throw new BusinessException(AuctionErrorCode.AUCTION_DURATION_TOO_SHORT);
        }
    }

    /**
     * 즉시구매가 설정 시 상품 개수 검증
     *
     * @param product 상품 정보
     * @param buyItNowPrice 즉시구매가
     */
    public void validateBuyItNowProductCount(Product product, java.math.BigDecimal buyItNowPrice) {

        if (validatePrice(buyItNowPrice)) {
            validateSingleItemForBuyItNow(product);
        }
    }

    /**
     * 즉시구매 시 가능 여부 검증
     */
    public void validateBuyItNowEligibility(Auction auction) {
        // 경매가 진행중인지 확인
        if (!auction.isInProgress()) {
            throw new BusinessException(AuctionErrorCode.AUCTION_NOT_IN_PROGRESS);
        }

        // 즉시구매가가 설정되어 있는지 확인
        if (!validatePrice(auction.getBuyItNowPrice())) {
            throw new BusinessException(AuctionErrorCode.BUY_IT_NOW_NOT_AVAILABLE);
        }

        // 재고가 1개인지 확인
        validateSingleItemForBuyItNow(auction.getProduct());
    }

    /**
     * 즉시 구매 시 상품이 1개인지 검증
     */
    public void validateSingleItemForBuyItNow(Product product) {
        if (product.getProductCount() != 1) {
            throw new BusinessException(AuctionErrorCode.BUY_IT_NOW_ONLY_FOR_SINGLE_ITEM);
        }
    }


    private static boolean validatePrice(BigDecimal buyItNowPrice) {
        return buyItNowPrice != null && buyItNowPrice.compareTo(BigDecimal.ZERO) > 0;
    }

    // 경매가 활성화인지 검증 (시작 X)
    private  void validateProductStatus(Product product) {
        if (product.getStatus() != Product.ProductStatus.ACTIVE) {
            throw new BusinessException(AuctionErrorCode.INVALID_PRODUCT_STATUS_FOR_AUCTION);
        }
    }

    /**
     * auction 생성 시 검증
     */
    public void validateAuctionCreation(Product product, LocalDateTime start, LocalDateTime end, BigDecimal buyItNowPrice) {
        // 1. 시간 검증
        validateAuctionTime(start, end);

        // 2. 즉시 구매가, 상품 1개 검증 (즉시 구매가가 없을 경우 판단하지 않음)
        validateBuyItNowProductCount(product, buyItNowPrice);

        // 3. 상품 상태 검증
        validateProductStatus(product);

    }

    /**
     * 경매가 시작중인지 검증
     */
    public void validateAuctionStart(Auction auction) {
        if (!auction.canStart()) {
            throw new BusinessException(AuctionErrorCode.CANNOT_START_AUCTION);
        }
    }
}
