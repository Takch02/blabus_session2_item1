package com.highlight.highlight_backend.admin.product;

import com.highlight.highlight_backend.product.dto.ProductCreateRequestDto;
import com.highlight.highlight_backend.product.dto.ProductResponseDto;
import com.highlight.highlight_backend.product.domian.Product;
import com.highlight.highlight_backend.product.service.AdminProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * product 의 상품 조회 통합테스트
 */
@SpringBootTest
@Transactional
class AdminProductServiceTest {

    @Autowired
    private AdminProductService adminProductService;

    @Test
    @DisplayName("상품을 등록하면 목록 조회 시 조회되어야 한다")
    void createAndFindProduct() {
        // given (준비): 테스트용 데이터 생성
        Long adminId = 1L; // DB에 존재하는 관리자 ID여야 함 (아니면 @BeforeEach로 미리 넣던가)
        ProductCreateRequestDto request = createDummyDto();

        // when (실행): 실제 서비스 메서드 호출
        // 1. 상품 등록
        ProductResponseDto savedProduct = adminProductService.createProduct(request, adminId);

        PageRequest pageRequest = PageRequest.of(0, 10);

        Page<ProductResponseDto> productList = adminProductService.getProductList(pageRequest, adminId);
        // 상품등록 체크
        assertThat(savedProduct).isNotNull();
        assertThat(savedProduct.getProductName()).isEqualTo("하루노가 지켜보고 있다");

        // 목록 조회 결과에 방금 등록한 상품이 있어야 함
        assertThat(productList.getContent()).isNotEmpty();
        assertThat(productList.getContent().get(0).getProductName()).isEqualTo("하루노가 지켜보고 있다");

        // N+1 잡았는지 확인 (이미지 리스트가 잘 들어있는지)
        assertThat(productList.getContent().get(0).getImages()).isNotNull();
    }

    // 테스트용 더미 데이터 만드는 메서드
    private ProductCreateRequestDto createDummyDto() {

        ProductCreateRequestDto.ProductImageDto imageDto = ProductCreateRequestDto.ProductImageDto.builder()
                .imageUrl("http://test-image.com/1.jpg")
                .originalFileName("test.jpg")
                .fileSize(1024L)
                .mimeType("image/jpeg")
                .isPrimary(true)
                .sortOrder(1)
                .build();

        return ProductCreateRequestDto.builder()
                .productName("하루노가 지켜보고 있다")
                .shortDescription("테스트용 상품입니다.")
                .history("2025년 제조")
                .expectedEffects("기분 전환")
                .detailedInfo("아주 상세한 정보입니다.")
                .isPremium(true)
                .category(Product.Category.FURNITURE) // TODO: 네 Enum 값으로 변경해 (CLOTHES, ELECTRONICS 등)
                .productCount(1L)
                .material("Wood")
                .size("L")
                .brand("Highlight")
                .manufactureYear(2024)
                .condition("New")
                .rank(Product.ProductRank.BEST) // TODO: 네 Enum 값으로 변경해 (A, S, B 등)
                .images(List.of(imageDto))
                .isDraft(false)
                .build();
    }
}