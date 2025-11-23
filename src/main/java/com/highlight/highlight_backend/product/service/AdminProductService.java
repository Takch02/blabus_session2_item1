package com.highlight.highlight_backend.product.service;

import com.highlight.highlight_backend.product.domian.Product;
import com.highlight.highlight_backend.product.repository.AdminProductRepository;
import com.highlight.highlight_backend.admin.product.dto.ProductCreateRequestDto;
import com.highlight.highlight_backend.admin.product.dto.ProductResponseDto;
import com.highlight.highlight_backend.admin.product.dto.ProductUpdateRequestDto;
import com.highlight.highlight_backend.admin.validator.AdminValidator;
import com.highlight.highlight_backend.exception.BusinessException;
import com.highlight.highlight_backend.exception.ProductErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * 상품 관리 서비스
 * 
 * 경매 진행 상품의 등록, 수정, 조회 기능을 제공합니다.
 *
 * 리펙토링 포인트 : DTO에서 이미 검증하고있는데 service에서 검증을 한번 더하는 기이한 코드 삭제(상품 추가, 수정에서)
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProductService {
    
    private final AdminProductRepository adminProductRepository;

    private final AdminValidator adminValidator;

    private final AdminProductImageService adminProductImageService;

    
    /**
     * 상품 등록
     * 
     * @param request 상품 등록 요청 데이터
     * @param adminId 등록하는 관리자 ID
     * @return 등록된 상품 정보
     */
    @Transactional
    public ProductResponseDto createProduct(ProductCreateRequestDto request, Long adminId) {
        log.info("상품 등록 요청: {} (관리자: {})", request.getProductName(), adminId);
        
        // 1. 관리자 권한 확인
        adminValidator.validateManagePermission(adminId);
        
        // 2. 상품 엔티티 생성
        Product product = new Product().setFirstProductDetail(request, adminId);

        // 3. 상품 저장
        Product savedProduct = adminProductRepository.save(product);
        
        // 4. 상품 이미지 처리
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            adminProductImageService.processProductImages(savedProduct, request.getImages());
        }
        
        log.info("상품 등록 완료: {} (ID: {})", savedProduct.getProductName(), savedProduct.getId());
        
        return ProductResponseDto.from(savedProduct);
    }



    /**
     * 상품 수정
     * 
     * @param productId 수정할 상품 ID
     * @param request 상품 수정 요청 데이터
     * @param adminId 수정하는 관리자 ID
     * @return 수정된 상품 정보
     */
    @Transactional
    public ProductResponseDto updateProduct(Long productId, ProductUpdateRequestDto request, Long adminId) {
        log.info("상품 수정 요청: {} (관리자: {})", productId, adminId);
        
        // 1. 관리자 권한 확인
        adminValidator.validateManagePermission(adminId);
        
        // 2. 상품 조회
        Product product = adminProductRepository.findByIdWithImages(productId)
            .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        // 3. 상품 이미지 업데이트
        if (request.getImages() != null) {
            adminProductImageService.updateProductImages(product, request.getImages());
        }
        product.updateProductDetail(request, product);
        
        log.info("상품 수정 완료: {} (ID: {})", product.getProductName(), product.getId());
        
        return ProductResponseDto.from(product);
    }


    /**
     * 상품 목록 조회
     *
     * 리펙토링 전 : N + 1 문제 발생, product N개 가져오는 쿼리 1개 + 각 product 마다 image 쿼리 N 개
     * 해결: Global Batch Fetch Size(100) 설정을 통해 IN 절로 쿼리 최적화
     */
    public Page<ProductResponseDto> getProductList(Pageable pageable, Long adminId) {
        log.info("상품 목록 조회 요청 (관리자: {})", adminId);
        
        adminValidator.validateManagePermission(adminId);
        
        return adminProductRepository.findByRegisteredByOrderByCreatedAtDesc(adminId, pageable)
            .map(ProductResponseDto::from);
    }
    
    /**
     * 상품 상세 조회
     * 
     * @param productId 조회할 상품 ID
     * @param adminId 조회하는 관리자 ID
     * @return 상품 상세 정보
     */
    public ProductResponseDto getProduct(Long productId, Long adminId) {
        log.info("상품 상세 조회 요청: {} (관리자: {})", productId, adminId);
        
        adminValidator.validateManagePermission(adminId);
        
        Product product = adminProductRepository.findByIdWithImages(productId)
            .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
        
        return ProductResponseDto.from(product);
    }
    
    /**
     * 상품 삭제
     * 
     * @param productId 삭제할 상품 ID
     * @param adminId 삭제하는 관리자 ID
     */
    @Transactional
    public void deleteProduct(Long productId, Long adminId) {
        log.info("상품 삭제 요청: {} (관리자: {})", productId, adminId);
        
        adminValidator.validateManagePermission(adminId);
        
        Product product = adminProductRepository.findById(productId)
            .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
        
        // 경매 중인 상품은 삭제 불가
        if (product.getStatus() == Product.ProductStatus.IN_AUCTION) {
            throw new BusinessException(ProductErrorCode.CANNOT_DELETE_AUCTION_PRODUCT);
        }
        
        adminProductRepository.delete(product);
        
        log.info("상품 삭제 완료: {} (ID: {})", product.getProductName(), product.getId());
    }


    /**
     * 상품 프리미엄 설정 변경
     * 
     * @param productId 상품 ID
     * @param isPremium 프리미엄 설정 여부
     * @param adminId 관리자 ID
     * @return 업데이트된 상품 정보
     */
    @Transactional
    public ProductResponseDto updateProductPremium(Long productId, Boolean isPremium, Long adminId) {
        log.info("상품 프리미엄 설정 변경: 상품={}, 프리미엄={}, 관리자={}", productId, isPremium, adminId);
        
        // 관리자 권한 검증
        adminValidator.validateManagePermission(adminId);
        
        // 상품 조회
        Product product = adminProductRepository.findById(productId)
            .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));
        
        // 프리미엄 설정 변경
        product.setIsPremium(isPremium);
        
        log.info("상품 프리미엄 설정 완료: {} (ID: {}, 프리미엄: {})",
                product.getProductName(), product.getId(), isPremium);
        
        return ProductResponseDto.from(product);
    }


}