package com.highlight.highlight_backend.product.domian;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 상품 이미지 엔티티
 * 
 * 상품에 첨부되는 이미지 정보를 저장하는 엔티티입니다.
 * 
 * @author 전우선
 * @since 2025.08.13
 */
@Entity
@Table(name = "product_image")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ProductImage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 연관된 상품
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    /**
     * 이미지 파일명
     */
    @Column(nullable = false, length = 255)
    private String fileName;
    
    /**
     * 원본 파일명
     */
    @Column(nullable = false, length = 255)
    private String originalFileName;
    
    /**
     * 이미지 URL/경로
     */
    @Column(nullable = false, length = 500)
    private String imageUrl;
    
    /**
     * 파일 크기 (bytes)
     */
    @Column(nullable = false)
    private Long fileSize;
    
    /**
     * MIME 타입
     */
    @Column(length = 100)
    private String mimeType;
    
    /**
     * 대표 이미지 여부
     */
    @Column(nullable = false)
    private boolean isPrimary = false;
    
    /**
     * 정렬 순서
     */
    @Column(nullable = false)
    private Integer sortOrder = 0;
    
    /**
     * 생성 시간
     */
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 생성자
     */
    public ProductImage(String fileName, String originalFileName, String imageUrl, Long fileSize, String mimeType) {
        this.fileName = fileName;
        this.originalFileName = originalFileName;
        this.imageUrl = imageUrl;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
    }
}