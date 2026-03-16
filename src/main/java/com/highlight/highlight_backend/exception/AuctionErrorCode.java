package com.highlight.highlight_backend.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 경매 관련 에러 코드
 * 
 * @author 전우선
 * @since 2025.08.16
 */
@Getter
@RequiredArgsConstructor
public enum AuctionErrorCode implements ErrorCode {
    
    /** 경매를 찾을 수 없음 */
    AUCTION_NOT_FOUND(HttpStatus.NOT_FOUND, "AUCTION_001", "경매를 찾을 수 없습니다."),
    /** 상품이 이미 경매에 등록됨 */
    PRODUCT_ALREADY_IN_AUCTION(HttpStatus.CONFLICT, "AUCTION_002", "이미 경매에 등록된 상품입니다."),
    /** 경매 가능하지 않은 상품 상태 */
    INVALID_PRODUCT_STATUS_FOR_AUCTION(HttpStatus.BAD_REQUEST, "AUCTION_003", "경매 가능한 상품 상태가 아닙니다."),
    /** 경매 시작 불가 */
    CANNOT_START_AUCTION(HttpStatus.BAD_REQUEST, "AUCTION_004", "경매를 시작할 수 없는 상태입니다."),
    /** 경매 종료 불가 */
    CANNOT_END_AUCTION(HttpStatus.BAD_REQUEST, "AUCTION_005", "경매를 종료할 수 없는 상태입니다."),
    /** 유효하지 않은 경매 시작 시간 */
    INVALID_AUCTION_START_TIME(HttpStatus.BAD_REQUEST, "AUCTION_006", "경매 시작 시간이 유효하지 않습니다."),
    /** 유효하지 않은 경매 종료 시간 */
    INVALID_AUCTION_END_TIME(HttpStatus.BAD_REQUEST, "AUCTION_007", "경매 종료 시간이 유효하지 않습니다."),
    /** 경매 진행 시간 부족 */
    AUCTION_DURATION_TOO_SHORT(HttpStatus.BAD_REQUEST, "AUCTION_008", "경매 진행 시간은 최소 10분 이상이어야 합니다."),
    /** 유효하지 않은 입찰 단위 */
    INVALID_BID_UNIT(HttpStatus.BAD_REQUEST, "AUCTION_009", "입찰 단위는 0보다 커야 합니다."),
    /** 유효하지 않은 배송비 */
    INVALID_SHIPPING_FEE(HttpStatus.BAD_REQUEST, "AUCTION_010", "배송비는 0원 이상이어야 합니다."),
    /** 유효하지 않은 최소 입찰가 */
    INVALID_MINIMUM_BID(HttpStatus.BAD_REQUEST, "AUCTION_011", "최소 입찰가는 입찰 단위보다 크거나 같아야 합니다."),
    /** 입찰가와 입찰 단위 불일치 */
    BID_UNIT_MISMATCH(HttpStatus.BAD_REQUEST, "AUCTION_012", "입찰가는 입찰 단위의 배수여야 합니다."),
    /** 최대 입찰가가 최소 입찰가보다 작음 */
    MAX_BID_LESS_THAN_MIN_BID(HttpStatus.BAD_REQUEST, "AUCTION_013", "최대 입찰가는 최소 입찰가보다 크거나 같아야 합니다."),
    /** 경매가 진행중이 아님 */
    AUCTION_NOT_IN_PROGRESS(HttpStatus.BAD_REQUEST, "AUCTION_014", "경매가 진행중이 아닙니다."),
    /** 즉시구매 불가능 */
    BUY_IT_NOW_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "AUCTION_015", "즉시구매가 설정되지 않아 즉시구매할 수 없습니다."),
    /** 즉시구매는 재고 1개 상품만 가능 */
    BUY_IT_NOW_ONLY_FOR_SINGLE_ITEM(HttpStatus.BAD_REQUEST, "AUCTION_016", "즉시구매는 재고가 1개인 상품만 가능합니다."),
    /** 입찰가가 너무 낮음 */
    BID_AMOUNT_TOO_LOW(HttpStatus.BAD_REQUEST, "AUCTION_017", "입찰가가 너무 낮습니다."),
    /** 진행 중인 경매는 수정할 수 없음 */
    CANNOT_MODIFY_IN_PROGRESS_AUCTION(HttpStatus.BAD_REQUEST, "AUCTION_018", "진행 중인 경매는 수정할 수 없습니다."),
    /** 종료된 경매는 수정할 수 없음 */
    CANNOT_MODIFY_ENDED_AUCTION(HttpStatus.BAD_REQUEST, "AUCTION_019", "종료된 경매는 수정할 수 없습니다."),
    /** 락 경합으로 인해 입찰에 참여할 수 없음 **/
    ALREADY_HAVE_LOCK(HttpStatus.TOO_MANY_REQUESTS, "AUCTION_020", "현재 접속자가 많아 입찰이 지연되고 있습니다. 잠시 후 다시 시도해주세요."),
    AUCTION_LOCK_INTERRUPT(HttpStatus.SERVICE_UNAVAILABLE, "AUCTION_021", "서버 종류 or 스레드 풀 강제 종료됨.");

    
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}