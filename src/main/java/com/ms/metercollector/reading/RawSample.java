package com.ms.metercollector.reading;

import com.ms.metercollector.message.MeterStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 통계 계산에 쓰는 rawReading 한 건.
public record RawSample(
        LocalDateTime measuredAt,
        BigDecimal cumulativeKwh,
        BigDecimal instantKw,
        MeterStatus meterStatus
) {
}
