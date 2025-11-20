package com.highlight.highlight_backend.service;

import com.highlight.highlight_backend.domain.ProductWishlist;
import com.highlight.highlight_backend.dto.ProductWishlistResponseDto;
import com.highlight.highlight_backend.exception.BusinessException;
import com.highlight.highlight_backend.exception.WishlistErrorCode;
import com.highlight.highlight_backend.exception.UserErrorCode;
import com.highlight.highlight_backend.exception.ProductErrorCode;
import com.highlight.highlight_backend.admin.product.repository.ProductRepository;
import com.highlight.highlight_backend.repository.ProductWishlistRepository;
import com.highlight.highlight_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 상품 찜하기 서비스
 * 
 * 상품 찜하기, 취소, 조회 기능을 제공합니다.
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductWishlistService {
    
    private final ProductWishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    
    /**
     * 상품 찜하기/취소 토글
     * 
     * @param userId 사용자 ID
     * @param productId 상품 ID
     * @return 찜하기 결과
     */
    @Transactional
    public ProductWishlistResponseDto toggleWishlist(Long userId, Long productId) {
        log.info("상품 찜하기 토글: 사용자 {}, 상품 {}", userId, productId);
        
        // 1. 사용자 및 상품 존재 확인
        validateUserAndProduct(userId, productId);
        
        // 2. 기존 찜하기 조회
        Optional<ProductWishlist> existingWishlist = wishlistRepository.findByUserIdAndProductId(userId, productId);
        
        if (existingWishlist.isPresent()) {
            // 3-1. 이미 찜했으면 찜 취소
            wishlistRepository.deleteByUserIdAndProductId(userId, productId);
            log.info("찜하기 취소 완료: 사용자 {}, 상품 {}", userId, productId);
            
            return ProductWishlistResponseDto.simple(productId, false);
        } else {
            // 3-2. 찜하지 않았으면 찜하기 추가
            ProductWishlist newWishlist = new ProductWishlist(userId, productId);
            ProductWishlist savedWishlist = wishlistRepository.save(newWishlist);
            log.info("찜하기 추가 완료: ID {}, 사용자 {}, 상품 {}", savedWishlist.getId(), userId, productId);
            
            return ProductWishlistResponseDto.simple(productId, true);
        }
    }
    
    /**
     * 상품 찜하기 추가
     * 
     * @param userId 사용자 ID
     * @param productId 상품 ID
     * @return 찜하기 결과
     */
    @Transactional
    public ProductWishlistResponseDto addToWishlist(Long userId, Long productId) {
        log.info("상품 찜하기 추가: 사용자 {}, 상품 {}", userId, productId);
        
        // 1. 사용자 및 상품 존재 확인
        validateUserAndProduct(userId, productId);
        
        // 2. 이미 찜했는지 확인
        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new BusinessException(WishlistErrorCode.DUPLICATE_WISHLIST);
        }
        
        // 3. 찜하기 추가
        ProductWishlist wishlist = new ProductWishlist(userId, productId);
        ProductWishlist savedWishlist = wishlistRepository.save(wishlist);
        
        log.info("찜하기 추가 완료: ID {}", savedWishlist.getId());
        
        return ProductWishlistResponseDto.simple(productId, true);
    }
    
    /**
     * 상품 찜하기 취소
     * 
     * @param userId 사용자 ID
     * @param productId 상품 ID
     */
    @Transactional
    public void removeFromWishlist(Long userId, Long productId) {
        log.info("상품 찜하기 취소: 사용자 {}, 상품 {}", userId, productId);
        
        // 1. 찜하기 존재 확인
        if (!wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new BusinessException(WishlistErrorCode.WISHLIST_NOT_FOUND);
        }
        
        // 2. 찜하기 삭제
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
        
        log.info("찜하기 취소 완료: 사용자 {}, 상품 {}", userId, productId);
    }
    
    /**
     * 사용자의 찜한 상품 목록 조회
     * 
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 찜한 상품 목록
     */
    public Page<ProductWishlistResponseDto> getUserWishlist(Long userId, Pageable pageable) {
        log.info("사용자 찜한 상품 목록 조회: 사용자 {}", userId);
        
        // 사용자 존재 확인
        validateUser(userId);
        
        return wishlistRepository.findByUserIdWithProduct(userId, pageable)
            .map(ProductWishlistResponseDto::from);
    }
    
    /**
     * 특정 상품의 찜하기 상태 조회
     * 
     * @param userId 사용자 ID
     * @param productId 상품 ID
     * @return 찜하기 상태
     */
    public ProductWishlistResponseDto getWishlistStatus(Long userId, Long productId) {
        log.info("상품 찜하기 상태 조회: 사용자 {}, 상품 {}", userId, productId);
        
        boolean isWishlisted = wishlistRepository.existsByUserIdAndProductId(userId, productId);
        
        return ProductWishlistResponseDto.simple(productId, isWishlisted);
    }
    
    /**
     * 여러 상품의 찜하기 상태를 한번에 조회
     * 
     * @param userId 사용자 ID
     * @param productIds 상품 ID 목록
     * @return 찜한 상품 ID 목록
     */
    public List<Long> getWishlistedProductIds(Long userId, List<Long> productIds) {
        log.info("여러 상품 찜하기 상태 조회: 사용자 {}, 상품 개수 {}", userId, productIds.size());
        
        return wishlistRepository.findWishlistedProductIds(userId, productIds);
    }
    
    /**
     * 특정 상품을 찜한 사용자 수 조회
     * 
     * @param productId 상품 ID
     * @return 찜한 사용자 수
     */
    public long getWishlistCount(Long productId) {
        log.info("상품 찜하기 개수 조회: 상품 {}", productId);
        
        return wishlistRepository.countByProductId(productId);
    }
    
    /**
     * 사용자가 찜한 상품 개수 조회
     * 
     * @param userId 사용자 ID
     * @return 찜한 상품 개수
     */
    public long getUserWishlistCount(Long userId) {
        log.info("사용자 찜한 상품 개수 조회: 사용자 {}", userId);
        
        return wishlistRepository.countByUserId(userId);
    }
    
    /**
     * 사용자 존재 확인
     */
    private void validateUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }
    }
    
    /**
     * 사용자 및 상품 존재 확인
     */
    private void validateUserAndProduct(Long userId, Long productId) {
        // 사용자 존재 확인
        validateUser(userId);
        
        // 상품 존재 확인
        if (!productRepository.existsById(productId)) {
            throw new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND);
        }
    }
}