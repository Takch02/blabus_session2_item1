package com.highlight.highlight_backend.bid.controller;

import com.highlight.highlight_backend.bid.dto.BidCreateRequestDto;
import com.highlight.highlight_backend.bid.dto.BidResponseDto;
import com.highlight.highlight_backend.bid.service.BidService;
import com.highlight.highlight_backend.common.config.ResponseDto;
import com.highlight.highlight_backend.common.util.ResponseUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// package는 test/api 등 원하는 위치
@RestController
@RequestMapping("/api/loadtest/bids")
@RequiredArgsConstructor
public class BidLoadTestController {

    private final BidService bidService;

    @PostMapping
    public ResponseEntity<ResponseDto<BidResponseDto>> createBidLoadTest(
            @Valid @RequestBody BidCreateRequestDto request,
            @RequestHeader("X-User-Id") Long userId
    ) {
        // DB 병목만 보기 위해 인증 로직 우회
        BidResponseDto response = bidService.createBid(request, userId);
        return ResponseUtils.success(response, "입찰에 성공했습니다.");
    }
}
