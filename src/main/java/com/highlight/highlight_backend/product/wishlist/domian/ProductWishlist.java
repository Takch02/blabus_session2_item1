package com.highlight.highlight_backend.product.wishlist.domian;

import com.highlight.highlight_backend.product.domian.Product;
import com.highlight.highlight_backend.user.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 상품 찜하기 엔티티
 * 
 * 사용자가 찜한 상품 정보를 저장합니다.
 * 
 * @author 전우선
 * @since 2025.08.16
 */
@Entity
@Table(name = "product_wishlist", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"}))
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ProductWishlist {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 찜한 사용자 ID
     */
    @Column(nullable = false)
    private Long userId;
    
    /**
     * 찜한 상품 ID
     */
    @Column(nullable = false)
    private Long productId;
    
    /**
     * 찜한 시간
     */
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 상품 정보 (Lazy Loading)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productId", insertable = false, updatable = false)
    private Product product;
    
    /**
     * 사용자 정보 (Lazy Loading)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", insertable = false, updatable = false)
    private User user;
    
    /**
     * 찜하기 생성자
     * 
     * @param userId 사용자 ID
     * @param productId 상품 ID
     */
    public ProductWishlist(Long userId, Long productId) {
        this.userId = userId;
        this.productId = productId;
    }
}