package com.ms.metercollector.stat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 15분/1시간/일 통계 한 건. 일 통계는 periodStart 가 그날 00:00 이다.
// 15분 통계의 최대수요전력은 그 구간의 평균전력 자체라서 peakDemandKw = avgKw, peakDemandAt = periodStart 로 둔다.
public record UsageStat(
        long deviceId,
        LocalDateTime periodStart,
        BigDecimal startCumulativeKwh,
        BigDecimal endCumulativeKwh,
        BigDecimal usageKwh,
        BigDecimal avgKw,
        BigDecimal maxKw,
        BigDecimal minKw,
        BigDecimal peakDemandKw,
        LocalDateTime peakDemandAt,
        int sampleCount,
        int expectedCount,
        Quality quality
) {
}
