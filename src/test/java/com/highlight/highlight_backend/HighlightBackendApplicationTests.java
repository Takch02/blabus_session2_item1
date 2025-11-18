package com.highlight.highlight_backend;

import com.highlight.highlight_backend.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

@SpringBootTest
class HighlightBackendApplicationTests {

    @Test
    void contextLoads() {

        User user = new User();
        user.setRank(User.Rank.Flower);
        User.Rank rank  = user.getRank();
        User.RankPercent rankPercent = User.RankPercent.findByUserRank(rank);
        String percent = rankPercent.getDescription();
        BigDecimal userPercent = new BigDecimal(percent);

        System.out.println(userPercent);
    }

}
