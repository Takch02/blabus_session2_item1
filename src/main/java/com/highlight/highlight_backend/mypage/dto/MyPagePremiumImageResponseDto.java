package com.highlight.highlight_backend.mypage.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class MyPagePremiumImageResponseDto {

    private String premiumImagesURL;

    private String productName;

    private BigDecimal productPrice;

    public MyPagePremiumImageResponseDto(BigDecimal productPrice, String productName, String imageURL) {
        this.productPrice = productPrice;
        this.productName = productName;
        this.premiumImagesURL = imageURL;
    }
}
