package com.ms.metercollector.stat;

import com.ms.metercollector.message.MeterStatus;
import com.ms.metercollector.reading.RawSample;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

// DB 를 모르는 순수 계산. 원본 값 → 15분 통계, 15분 통계 여러 건 → 1시간/일 통계.
public final class UsageStatCalculator {

    public static final int MINUTES_15 = 15;

    private UsageStatCalculator() {
    }

    /**
     * 15분 통계. 구간은 [periodStart, periodStart+15분).
     *
     * <p>누적값은 그 시각까지 쓴 양이므로, 구간 사용량 = (끝 시각 누적값) - (시작 시각 누적값) 이다.
     * 예: 14:15 구간 = 14:30 값 - 14:15 값. 순시전력 표본은 14:15 ~ 14:29 의 15개.
     *
     * @param before 시작 시각 이전(같은 시각 포함)의 가장 최근 값. 없으면 첫 표본으로 대신한다 (새로 붙은 계량기)
     * @param window measuredAt 이 [periodStart, periodStart+15분] 인 값들, 시각 순. 끝 시각 값도 포함한다
     */
    public static UsageStat of15m(long deviceId, LocalDateTime periodStart, RawSample before, List<RawSample> window) {
        LocalDateTime periodEnd = periodStart.plusMinutes(MINUTES_15);
        List<RawSample> samples = window.stream()
                .filter(s -> s.measuredAt().isBefore(periodEnd))
                .toList();

        BigDecimal startCumulative = before != null ? before.cumulativeKwh()
                : samples.isEmpty() ? null : samples.getFirst().cumulativeKwh();
        // 구간이 끝나는 쪽 값. 구간 안에 새 값이 하나도 없으면 사용량을 알 수 없다.
        BigDecimal endCumulative = window.stream()
                .filter(s -> s.measuredAt().isAfter(periodStart))
                .reduce((first, second) -> second)
                .map(RawSample::cumulativeKwh)
                .orElse(null);

        BigDecimal usage = null;
        boolean decreased = false;
        if (startCumulative != null && endCumulative != null) {
            usage = endCumulative.subtract(startCumulative);
            if (usage.signum() < 0) {
                usage = null;
                decreased = true;
            }
        }

        BigDecimal avgKw = null;
        BigDecimal maxKw = null;
        BigDecimal minKw = null;
        if (!samples.isEmpty()) {
            BigDecimal sum = samples.stream().map(RawSample::instantKw).reduce(BigDecimal.ZERO, BigDecimal::add);
            avgKw = sum.divide(BigDecimal.valueOf(samples.size()), 2, RoundingMode.HALF_UP);
            maxKw = samples.stream().map(RawSample::instantKw).max(Comparator.naturalOrder()).orElseThrow();
            minKw = samples.stream().map(RawSample::instantKw).min(Comparator.naturalOrder()).orElseThrow();
        }

        boolean fault = decreased || samples.stream().anyMatch(s -> s.meterStatus() == MeterStatus.FAULT);
        boolean maint = samples.stream().anyMatch(s -> s.meterStatus() == MeterStatus.MAINT);

        return new UsageStat(
                deviceId, periodStart,
                startCumulative, endCumulative, usage,
                avgKw, maxKw, minKw,
                avgKw, avgKw == null ? null : periodStart,
                samples.size(), MINUTES_15,
                quality(samples.size(), MINUTES_15, fault, maint)
        );
    }

    /**
     * 15분 통계 여러 건을 합쳐 더 긴 구간(1시간, 일)의 통계를 만든다.
     *
     * @param children    구간 안의 15분 통계들, 시각 순. 빠진 구간이 있어도 된다
     * @param expectedCount 구간 전체의 기대 표본 수 (1시간 60, 일 1440)
     */
    public static UsageStat rollUp(long deviceId, LocalDateTime periodStart, int expectedCount, List<UsageStat> children) {
        int sampleCount = children.stream().mapToInt(UsageStat::sampleCount).sum();

        BigDecimal startCumulative = children.stream()
                .map(UsageStat::startCumulativeKwh).filter(Objects::nonNull).findFirst().orElse(null);
        BigDecimal endCumulative = children.stream()
                .map(UsageStat::endCumulativeKwh).filter(Objects::nonNull).reduce((first, second) -> second).orElse(null);
        BigDecimal usage = children.stream()
                .map(UsageStat::usageKwh).filter(Objects::nonNull).reduce(BigDecimal::add).orElse(null);

        // 표본 수로 가중한 평균. 표본 15개짜리 구간과 3개짜리 구간이 같은 무게를 갖지 않도록.
        List<UsageStat> measured = children.stream().filter(c -> c.avgKw() != null && c.sampleCount() > 0).toList();
        BigDecimal avgKw = null;
        if (!measured.isEmpty()) {
            BigDecimal weighted = measured.stream()
                    .map(c -> c.avgKw().multiply(BigDecimal.valueOf(c.sampleCount())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            int weight = measured.stream().mapToInt(UsageStat::sampleCount).sum();
            avgKw = weighted.divide(BigDecimal.valueOf(weight), 2, RoundingMode.HALF_UP);
        }
        BigDecimal maxKw = children.stream()
                .map(UsageStat::maxKw).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
        BigDecimal minKw = children.stream()
                .map(UsageStat::minKw).filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);

        // 같은 값이면 먼저 나온 구간을 최대수요 시각으로 본다.
        UsageStat peak = children.stream()
                .filter(c -> c.peakDemandKw() != null)
                .reduce((a, b) -> b.peakDemandKw().compareTo(a.peakDemandKw()) > 0 ? b : a)
                .orElse(null);

        boolean fault = children.stream().anyMatch(c -> c.quality() == Quality.FAULT);
        boolean maint = children.stream().anyMatch(c -> c.quality() == Quality.MAINT);

        return new UsageStat(
                deviceId, periodStart,
                startCumulative, endCumulative, usage,
                avgKw, maxKw, minKw,
                peak == null ? null : peak.peakDemandKw(),
                peak == null ? null : peak.peakDemandAt(),
                sampleCount, expectedCount,
                quality(sampleCount, expectedCount, fault, maint)
        );
    }

    private static Quality quality(int sampleCount, int expectedCount, boolean fault, boolean maint) {
        if (sampleCount == 0) {
            return Quality.MISSING;
        }
        if (fault) {
            return Quality.FAULT;
        }
        if (maint) {
            return Quality.MAINT;
        }
        return sampleCount < expectedCount ? Quality.PARTIAL : Quality.OK;
    }
}
