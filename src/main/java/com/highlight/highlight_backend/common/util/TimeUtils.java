package com.highlight.highlight_backend.common.util;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * UTC 시간을 한국 시간(KST)으로 변환
 *
 */
public class TimeUtils {
    public static LocalDateTime convertUTCToKST(LocalDateTime utcTime) {
        if (utcTime == null) return null;
        return utcTime.atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(ZoneId.of("Asia/Seoul"))
                .toLocalDateTime();
    }
}