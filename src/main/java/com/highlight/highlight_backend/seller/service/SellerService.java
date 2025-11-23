package com.highlight.highlight_backend.seller.service;

import com.highlight.highlight_backend.seller.domain.Seller;
import com.highlight.highlight_backend.seller.dto.SellerResponseDto;
import com.highlight.highlight_backend.exception.BusinessException;
import com.highlight.highlight_backend.exception.SellerErrorCode;
import com.highlight.highlight_backend.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 판매자 서비스
 * 
 * 판매자 정보 조회, 관리 기능을 제공합니다.
 * 
 * @author 전우선
 * @since 2025.08.16
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerService {
    
    private final SellerRepository sellerRepository;
    
    /**
     * 판매자 상세 정보 조회
     * 
     * @param sellerId 판매자 ID
     * @return 판매자 상세 정보
     */
    public SellerResponseDto getSellerDetail(Long sellerId) {
        log.info("판매자 상세 정보 조회: {}", sellerId);
        
        Seller seller = sellerRepository.findById(sellerId)
            .orElseThrow(() -> new BusinessException(SellerErrorCode.SELLER_NOT_FOUND));
        
        return SellerResponseDto.from(seller);
    }
    
    /**
     * 판매자 목록 조회
     * 
     * @param pageable 페이징 정보
     * @return 판매자 목록
     */
    public Page<SellerResponseDto> getSellerList(Pageable pageable) {
        log.info("판매자 목록 조회");
        
        return sellerRepository.findByStatusOrderByCreatedAtDesc(Seller.SellerStatus.ACTIVE, pageable)
            .map(SellerResponseDto::from);
    }
    
    /**
     * 판매자명으로 검색
     * 
     * @param sellerName 판매자명
     * @param pageable 페이징 정보
     * @return 검색된 판매자 목록
     */
    public Page<SellerResponseDto> searchSellersByName(String sellerName, Pageable pageable) {
        log.info("판매자명 검색: {}", sellerName);
        
        return sellerRepository.findBySellerNameContainingIgnoreCase(sellerName, pageable)
            .map(SellerResponseDto::from);
    }
    
    /**
     * 평점 높은 판매자 조회
     * 
     * @param pageable 페이징 정보
     * @return 평점 순 판매자 목록
     */
    public Page<SellerResponseDto> getTopRatedSellers(Pageable pageable) {
        log.info("평점 높은 판매자 조회");
        
        return sellerRepository.findTopRatedSellers(pageable)
            .map(SellerResponseDto::from);
    }
    
    /**
     * 판매 건수 많은 판매자 조회
     * 
     * @param pageable 페이징 정보
     * @return 판매 건수 순 판매자 목록
     */
    public Page<SellerResponseDto> getTopSellersBySales(Pageable pageable) {
        log.info("판매 건수 많은 판매자 조회");
        
        return sellerRepository.findTopSellersBySales(pageable)
            .map(SellerResponseDto::from);
    }
    
    /**
     * 판매자 마지막 활동 시간 업데이트
     * 
     * @param sellerId 판매자 ID
     */
    @Transactional
    public void updateLastActiveTime(Long sellerId) {
        log.info("판매자 마지막 활동 시간 업데이트: {}", sellerId);
        
        Seller seller = sellerRepository.findById(sellerId)
            .orElseThrow(() -> new BusinessException(SellerErrorCode.SELLER_NOT_FOUND));
        
        seller.updateLastActiveAt();
        sellerRepository.save(seller);
    }
}