package com.highlight.highlight_backend.bid;

import com.highlight.highlight_backend.bid.service.BidService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class BidCreateService {

    @Autowired
    private BidService bidService;

    @Test
    void createBid() {

    }
}
