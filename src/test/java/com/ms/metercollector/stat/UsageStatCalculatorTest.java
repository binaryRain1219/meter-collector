package com.ms.metercollector.stat;

import com.ms.metercollector.message.MeterStatus;
import com.ms.metercollector.reading.RawSample;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UsageStatCalculatorTest {

    private static final LocalDateTime PERIOD = LocalDateTime.parse("2026-10-06T14:15:00");

    @Test
    void 사용량은_끝_시각_누적값에서_시작_시각_누적값을_뺀다() {
        // 14:15 ~ 14:30 매분, 누적값은 1분에 0.5 씩 증가, 순시전력은 30 고정
        List<RawSample> window = minutes(0, 15, MeterStatus.OK);

        UsageStat stat = UsageStatCalculator.of15m(1, PERIOD, window.getFirst(), window);

        assertThat(stat.startCumulativeKwh()).isEqualByComparingTo("100.0");
        assertThat(stat.endCumulativeKwh()).isEqualByComparingTo("107.5");
        assertThat(stat.usageKwh()).isEqualByComparingTo("7.5");
        assertThat(stat.avgKw()).isEqualByComparingTo("30.00");
        assertThat(stat.peakDemandKw()).isEqualByComparingTo("30.00");
        assertThat(stat.peakDemandAt()).isEqualTo(PERIOD);
        assertThat(stat.sampleCount()).isEqualTo(15);  // 14:30 값은 다음 구간 표본
        assertThat(stat.quality()).isEqualTo(Quality.OK);
    }

    @Test
    void 평균_최대_최소는_구간_안의_순시전력_표본으로_낸다() {
        List<RawSample> window = List.of(
                sample(0, "100.0", "10.00", MeterStatus.OK),
                sample(1, "100.2", "20.00", MeterStatus.OK),
                sample(2, "100.5", "45.00", MeterStatus.OK),
                sample(15, "101.0", "99.00", MeterStatus.OK));  // 끝 시각 값은 누적값에만 쓴다

        UsageStat stat = UsageStatCalculator.of15m(1, PERIOD, window.getFirst(), window);

        assertThat(stat.avgKw()).isEqualByComparingTo("25.00");
        assertThat(stat.maxKw()).isEqualByComparingTo("45.00");
        assertThat(stat.minKw()).isEqualByComparingTo("10.00");
        assertThat(stat.usageKwh()).isEqualByComparingTo("1.0");
        assertThat(stat.sampleCount()).isEqualTo(3);
        assertThat(stat.quality()).isEqualTo(Quality.PARTIAL);
    }

    @Test
    void 시작_시각_값이_빠지면_그_전의_마지막_값을_시작_누적값으로_쓴다() {
        RawSample before = sample(-3, "98.5", "30.00", MeterStatus.OK);
        List<RawSample> window = minutes(1, 15, MeterStatus.OK);

        UsageStat stat = UsageStatCalculator.of15m(1, PERIOD, before, window);

        assertThat(stat.startCumulativeKwh()).isEqualByComparingTo("98.5");
        assertThat(stat.usageKwh()).isEqualByComparingTo("9.0");  // 107.5 - 98.5
        assertThat(stat.quality()).isEqualTo(Quality.PARTIAL);
    }

    @Test
    void 이전_값이_없는_새_계량기는_첫_표본에서_시작한다() {
        List<RawSample> window = minutes(5, 15, MeterStatus.OK);

        UsageStat stat = UsageStatCalculator.of15m(1, PERIOD, null, window);

        assertThat(stat.startCumulativeKwh()).isEqualByComparingTo("102.5");
        assertThat(stat.usageKwh()).isEqualByComparingTo("5.0");
    }

    @Test
    void 표본이_없으면_MISSING_이고_사용량은_비운다() {
        RawSample before = sample(-30, "90.0", "30.00", MeterStatus.OK);

        UsageStat stat = UsageStatCalculator.of15m(1, PERIOD, before, List.of());

        assertThat(stat.quality()).isEqualTo(Quality.MISSING);
        assertThat(stat.usageKwh()).isNull();
        assertThat(stat.avgKw()).isNull();
        assertThat(stat.peakDemandAt()).isNull();
        assertThat(stat.sampleCount()).isZero();
    }

    @Test
    void 이상_표본이_섞이면_FAULT_점검_표본이면_MAINT() {
        List<RawSample> faulty = new ArrayList<>(minutes(0, 15, MeterStatus.OK));
        faulty.set(3, sample(3, "101.5", "30.00", MeterStatus.FAULT));
        List<RawSample> maint = new ArrayList<>(minutes(0, 15, MeterStatus.OK));
        maint.set(3, sample(3, "101.5", "30.00", MeterStatus.MAINT));

        assertThat(UsageStatCalculator.of15m(1, PERIOD, null, faulty).quality()).isEqualTo(Quality.FAULT);
        assertThat(UsageStatCalculator.of15m(1, PERIOD, null, maint).quality()).isEqualTo(Quality.MAINT);
    }

    @Test
    void 누적값이_줄면_계량기_교체로_보고_사용량을_비우고_FAULT() {
        RawSample before = sample(0, "500.0", "30.00", MeterStatus.OK);
        List<RawSample> window = minutes(0, 15, MeterStatus.OK);  // 100.0 부터 다시 시작

        UsageStat stat = UsageStatCalculator.of15m(1, PERIOD, before, window);

        assertThat(stat.usageKwh()).isNull();
        assertThat(stat.quality()).isEqualTo(Quality.FAULT);
    }

    @Test
    void 합칠_때_사용량은_더하고_평균은_표본_수로_가중하고_최대수요는_가장_큰_15분_평균() {
        LocalDateTime hour = LocalDateTime.parse("2026-10-06T14:00:00");
        List<UsageStat> children = List.of(
                stat15m(hour, "100.0", "105.0", "5.0", "20.00", "25.00", "15.00", 15, Quality.OK),
                stat15m(hour.plusMinutes(15), "105.0", "112.0", "7.0", "28.00", "35.00", "22.00", 15, Quality.OK),
                stat15m(hour.plusMinutes(30), "112.0", "118.0", "6.0", "24.00", "30.00", "18.00", 15, Quality.OK),
                stat15m(hour.plusMinutes(45), "118.0", "122.0", "4.0", "10.00", "12.00", "8.00", 5, Quality.PARTIAL));

        UsageStat stat = UsageStatCalculator.rollUp(1, hour, 60, children);

        assertThat(stat.startCumulativeKwh()).isEqualByComparingTo("100.0");
        assertThat(stat.endCumulativeKwh()).isEqualByComparingTo("122.0");
        assertThat(stat.usageKwh()).isEqualByComparingTo("22.0");
        // (20*15 + 28*15 + 24*15 + 10*5) / 50 = 1130 / 50
        assertThat(stat.avgKw()).isEqualByComparingTo("22.60");
        assertThat(stat.maxKw()).isEqualByComparingTo("35.00");
        assertThat(stat.minKw()).isEqualByComparingTo("8.00");
        assertThat(stat.peakDemandKw()).isEqualByComparingTo("28.00");
        assertThat(stat.peakDemandAt()).isEqualTo(hour.plusMinutes(15));
        assertThat(stat.sampleCount()).isEqualTo(50);
        assertThat(stat.expectedCount()).isEqualTo(60);
        assertThat(stat.quality()).isEqualTo(Quality.PARTIAL);
    }

    @Test
    void 합칠_때_하나라도_FAULT_면_FAULT_전부_없으면_MISSING() {
        LocalDateTime hour = LocalDateTime.parse("2026-10-06T14:00:00");
        List<UsageStat> children = List.of(
                stat15m(hour, "100.0", "105.0", "5.0", "20.00", "25.00", "15.00", 15, Quality.OK),
                stat15m(hour.plusMinutes(15), "105.0", "112.0", "7.0", "28.00", "35.00", "22.00", 15, Quality.FAULT));

        assertThat(UsageStatCalculator.rollUp(1, hour, 60, children).quality()).isEqualTo(Quality.FAULT);
        UsageStat empty = UsageStatCalculator.rollUp(1, hour, 60, List.of());
        assertThat(empty.quality()).isEqualTo(Quality.MISSING);
        assertThat(empty.usageKwh()).isNull();
    }

    // PERIOD + from 분 ~ PERIOD + to 분 매분. 누적값 100.0 에서 1분에 0.5 씩, 순시전력 30.
    private static List<RawSample> minutes(int from, int to, MeterStatus status) {
        List<RawSample> samples = new ArrayList<>();
        for (int m = from; m <= to; m++) {
            samples.add(sample(m, String.valueOf(100.0 + m * 0.5), "30.00", status));
        }
        return samples;
    }

    private static RawSample sample(int minute, String cumulativeKwh, String instantKw, MeterStatus status) {
        return new RawSample(PERIOD.plusMinutes(minute), new BigDecimal(cumulativeKwh), new BigDecimal(instantKw), status);
    }

    private static UsageStat stat15m(LocalDateTime periodStart, String start, String end, String usage,
                                     String avg, String max, String min, int samples, Quality quality) {
        BigDecimal avgKw = new BigDecimal(avg);
        return new UsageStat(1, periodStart, new BigDecimal(start), new BigDecimal(end), new BigDecimal(usage),
                avgKw, new BigDecimal(max), new BigDecimal(min), avgKw, periodStart, samples, 15, quality);
    }
}
