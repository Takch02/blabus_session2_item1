package com.highlight.highlight_backend.product.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

/**
 * AWS S3 파일 업로드 서비스
 * 
 * 상품 이미지 업로드/삭제를 위한 S3 서비스입니다.
 *
 */
@Slf4j
@Service
public class S3Service {
    
    private final S3Client s3Client;
    
    @Value("${aws.s3.bucket}")
    private String bucketName;
    
    @Value("${aws.s3.region}")
    private String region;
    
    public S3Service(@Value("${aws.s3.access-key}") String accessKey,
                     @Value("${aws.s3.secret-key}") String secretKey,
                     @Value("${aws.s3.region}") String region) {
        
        // AWS 자격 증명이 있는 경우에만 S3Client 생성
        if (accessKey != null && !accessKey.isEmpty() && 
            secretKey != null && !secretKey.isEmpty()) {
            
            AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
            this.s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                    .build();
            log.info("S3Client 초기화 완료");
        } else {
            // 개발 환경에서는 null로 설정 (로컬 테스트용)
            this.s3Client = null;
            log.warn("AWS 자격 증명이 없어 S3Client가 초기화되지 않았습니다. 개발 환경에서만 사용하세요.");
        }
    }
    
    /**
     * 상품 이미지 업로드
     * 
     * @param file 업로드할 파일
     * @param productId 상품 ID
     * @return 업로드된 파일의 S3 URL
     */
    public String uploadProductImage(MultipartFile file, Long productId) throws IOException {
        if (s3Client == null) {
            // 개발 환경에서는 더미 URL 반환
            String fileName = generateFileName(file.getOriginalFilename());
            log.warn("S3 클라이언트가 없어 더미 URL을 반환합니다: {}", fileName);
            return "https://dummy-s3-url.com/products/" + productId + "/" + fileName;
        }
        
        try {
            String fileName = generateFileName(file.getOriginalFilename());
            String key = "products/" + productId + "/" + fileName;
            
            // S3에 파일 업로드
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();
            
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
            
            // 업로드된 파일의 URL 생성
            String imageUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", 
                    bucketName, region, key);
            
            log.info("S3 파일 업로드 성공: {}", imageUrl);
            return imageUrl;
            
        } catch (Exception e) {
            log.error("S3 파일 업로드 실패: {}", e.getMessage());
            throw new RuntimeException("이미지 업로드에 실패했습니다.", e);
        }
    }
    
    /**
     * S3에서 파일 삭제
     * 
     * @param imageUrl 삭제할 이미지 URL
     */
    public void deleteImage(String imageUrl) {
        if (s3Client == null) {
            log.warn("S3 클라이언트가 없어 파일 삭제를 건너뜁니다: {}", imageUrl);
            return;
        }
        
        try {
            // URL에서 S3 key 추출
            String key = extractKeyFromUrl(imageUrl);
            
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            
            s3Client.deleteObject(deleteObjectRequest);
            log.info("S3 파일 삭제 성공: {}", key);
            
        } catch (Exception e) {
            log.error("S3 파일 삭제 실패: {}", e.getMessage());
            // 삭제 실패해도 예외를 던지지 않음 (비즈니스 로직에 영향 최소화)
        }
    }
    
    /**
     * 고유한 파일명 생성
     * 
     * @param originalFileName 원본 파일명
     * @return UUID가 포함된 고유 파일명
     */
    private String generateFileName(String originalFileName) {
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }
    
    /**
     * S3 URL에서 key 추출
     * 
     * @param imageUrl S3 이미지 URL
     * @return S3 key
     */
    private String extractKeyFromUrl(String imageUrl) {
        // https://bucket-name.s3.region.amazonaws.com/key 형식에서 key 추출
        String[] parts = imageUrl.split("/");
        if (parts.length >= 4) {
            StringBuilder key = new StringBuilder();
            for (int i = 3; i < parts.length; i++) {
                if (i > 3) key.append("/");
                key.append(parts[i]);
            }
            return key.toString();
        }
        throw new IllegalArgumentException("잘못된 S3 URL 형식입니다: " + imageUrl);
    }
}