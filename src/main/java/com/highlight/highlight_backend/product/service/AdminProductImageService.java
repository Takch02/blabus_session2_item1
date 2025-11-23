package com.highlight.highlight_backend.product.service;

import com.highlight.highlight_backend.admin.service.AdminAuthService;
import com.highlight.highlight_backend.product.domian.Product;
import com.highlight.highlight_backend.product.domian.ProductImage;
import com.highlight.highlight_backend.product.dto.ProductCreateRequestDto;
import com.highlight.highlight_backend.product.dto.ProductUpdateRequestDto;
import com.highlight.highlight_backend.product.repository.ProductImageRepository;
import com.highlight.highlight_backend.product.repository.ProductQueryRepository;
import com.highlight.highlight_backend.exception.BusinessException;
import com.highlight.highlight_backend.exception.ProductErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 리펙토링 하며 만든 service
 *
 * ProductService 안에 이미지 관련 로직이 비대하여 따로 뺐습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProductImageService {

    private final ProductQueryRepository productQueryRepository;
    private final ProductImageRepository productImageRepository;
    private final AdminAuthService adminService;

    private final S3Service s3Service;


    /**
     * 상품 이미지 처리 (신규 등록)
     *
     * @param product 상품 엔티티
     * @param imageDtos 이미지 DTO 목록
     */
    public void processProductImages(Product product, List<ProductCreateRequestDto.ProductImageDto> imageDtos) {
        for (ProductCreateRequestDto.ProductImageDto imageDto : imageDtos) {
            String fileName = generateFileName(imageDto.getOriginalFileName());

            ProductImage image = new ProductImage(
                    fileName,
                    imageDto.getOriginalFileName(),
                    imageDto.getImageUrl(),
                    imageDto.getFileSize(),
                    imageDto.getMimeType()
            );

            image.setPrimary(imageDto.isPrimary());
            image.setSortOrder(imageDto.getSortOrder());

            product.addImage(image);
        }
    }

    /**
     * 상품 이미지 업데이트 (수정)
     *
     * @param product 상품 엔티티
     * @param imageDtos 이미지 DTO 목록
     */
    public void updateProductImages(Product product, List<ProductUpdateRequestDto.ProductImageDto> imageDtos) {
        // 기존 이미지 모두 제거
        product.getImages().clear();

        // 새로운 이미지 추가
        for (ProductUpdateRequestDto.ProductImageDto imageDto : imageDtos) {
            if (!imageDto.isDeleted()) {
                String fileName = imageDto.getId() != null ?
                        productImageRepository.findById(imageDto.getId())
                                .map(ProductImage::getFileName)
                                .orElse(generateFileName(imageDto.getOriginalFileName())) :
                        generateFileName(imageDto.getOriginalFileName());

                ProductImage image = new ProductImage(
                        fileName,
                        imageDto.getOriginalFileName(),
                        imageDto.getImageUrl(),
                        imageDto.getFileSize(),
                        imageDto.getMimeType()
                );

                image.setPrimary(imageDto.isPrimary());
                image.setSortOrder(imageDto.getSortOrder());

                product.addImage(image);
            }
        }
    }

    /**
     * 파일명 생성
     *
     * @param originalFileName 원본 파일명
     * @return 생성된 파일명
     */
    private String generateFileName(String originalFileName) {
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }


    /**
     * 상품 이미지 업로드
     *
     * @param productId 상품 ID
     * @param files 업로드할 파일들
     * @param adminId 관리자 ID
     * @return 업로드된 이미지 URL 목록
     */
    @Transactional
    public List<String> uploadProductImages(Long productId, MultipartFile[] files, Long adminId) {
        log.info("상품 이미지 업로드: 상품={}, 파일개수={}, 관리자={}", productId, files.length, adminId);

        // 관리자 권한 검증
        adminService.validateManagePermission(adminId);

        // 상품 존재 확인
        Product product = productQueryRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        // 파일 검증
        validateImageFiles(files);

        List<String> imageUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                // S3에 파일 업로드
                String imageUrl = s3Service.uploadProductImage(file, productId);

                // DB에 이미지 정보 저장
                ProductImage productImage = new ProductImage(
                        extractFileNameFromUrl(imageUrl),
                        file.getOriginalFilename(),
                        imageUrl,
                        file.getSize(),
                        file.getContentType()
                );

                // 첫 번째 이미지를 대표 이미지로 설정
                if (product.getImages().isEmpty()) {
                    productImage.setPrimary(true);
                }

                product.addImage(productImage);
                imageUrls.add(imageUrl);

            } catch (IOException e) {
                log.error("이미지 업로드 실패: 파일={}, 오류={}", file.getOriginalFilename(), e.getMessage());
                throw new BusinessException(ProductErrorCode.IMAGE_UPLOAD_FAILED);
            }
        }

        productQueryRepository.save(product);

        log.info("상품 이미지 업로드 완료: {} 개 파일", imageUrls.size());
        return imageUrls;
    }

    /**
     * 상품 이미지 삭제
     *
     * @param productId 상품 ID
     * @param imageId 이미지 ID
     * @param adminId 관리자 ID
     */
    @Transactional
    public void deleteProductImage(Long productId, Long imageId, Long adminId) {
        log.info("상품 이미지 삭제: 상품={}, 이미지={}, 관리자={}", productId, imageId, adminId);

        // 관리자 권한 검증
        adminService.validateManagePermission(adminId);

        // 상품 존재 확인
        Product product = productQueryRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

        // 이미지 존재 확인
        ProductImage productImage = productImageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(ProductErrorCode.IMAGE_NOT_FOUND));

        // 이미지가 해당 상품에 속하는지 확인
        if (!productImage.getProduct().getId().equals(productId)) {
            throw new BusinessException(ProductErrorCode.IMAGE_NOT_BELONG_TO_PRODUCT);
        }

        // S3에서 파일 삭제
        s3Service.deleteImage(productImage.getImageUrl());

        // DB에서 이미지 삭제
        product.removeImage(productImage);
        productImageRepository.delete(productImage);

        log.info("상품 이미지 삭제 완료: 이미지ID={}", imageId);
    }

    /**
     * 이미지 파일 검증
     *
     * @param files 검증할 파일들
     */
    private void validateImageFiles(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new BusinessException(ProductErrorCode.NO_IMAGE_FILES);
        }

        // 파일 개수 제한 (최대 10개)
        if (files.length > 10) {
            throw new BusinessException(ProductErrorCode.TOO_MANY_IMAGE_FILES);
        }

        for (MultipartFile file : files) {
            // 빈 파일 검증
            if (file.isEmpty()) {
                throw new BusinessException(ProductErrorCode.EMPTY_IMAGE_FILE);
            }

            // 파일 크기 검증 (최대 10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                throw new BusinessException(ProductErrorCode.IMAGE_FILE_TOO_LARGE);
            }

            // 파일 타입 검증
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new BusinessException(ProductErrorCode.INVALID_IMAGE_FILE_TYPE);
            }

            // 지원하는 이미지 형식 검증
            List<String> allowedTypes = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp");
            if (!allowedTypes.contains(contentType.toLowerCase())) {
                throw new BusinessException(ProductErrorCode.UNSUPPORTED_IMAGE_FILE_TYPE);
            }
        }
    }

    /**
     * URL에서 파일명 추출
     *
     * @param imageUrl 이미지 URL
     * @return 파일명
     */
    private String extractFileNameFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return UUID.randomUUID().toString();
        }

        String[] parts = imageUrl.split("/");
        return parts[parts.length - 1];
    }

}
