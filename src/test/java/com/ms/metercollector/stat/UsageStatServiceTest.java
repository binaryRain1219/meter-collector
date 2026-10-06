package com.ms.metercollector.stat;

import com.ms.metercollector.device.DeviceRepository;
import com.ms.metercollector.message.MeterReading;
import com.ms.metercollector.message.MeterStatus;
import com.ms.metercollector.message.RunState;
import com.ms.metercollector.reading.RawReadingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 로컬 MySQL(meter-publisher 의 docker compose) 에 장비 시드가 들어가 있어야 한다.
// 실제 데이터와 겹치지 않게 2000-01-01 에 값을 넣고, 테스트가 끝나면 롤백한다.
@SpringBootTest
@Transactional
class UsageStatServiceTest {

    private static final LocalDateTime DAY = LocalDateTime.parse("2000-01-01T00:00:00");

    @Autowired
    UsageStatService usageStatService;

    @Autowired
    RawReadingRepository rawReadingRepository;

    @Autowired
    UsageStatRepository usageStatRepository;

    @Autowired
    DeviceRepository deviceRepository;

    @Test
    void 원본_값으로_15분_1시간_일_통계를_만든다() {
        long deviceId = deviceRepository.findIdByCode("AHU-01").orElseThrow();
        // 00:00 ~ 00:30 매분. 누적값 1분에 0.5, 순시전력 30
        for (int m = 0; m <= 30; m++) {
            save(deviceId, DAY.plusMinutes(m), new BigDecimal("100.0").add(new BigDecimal("0.5").multiply(BigDecimal.valueOf(m))));
        }

        usageStatService.recalculate(DAY, DAY.plusMinutes(30));

        List<UsageStat> stats15m = usageStatRepository.find15m(deviceId, DAY, DAY.plusHours(1));
        assertThat(stats15m).hasSize(2);
        assertThat(stats15m).allSatisfy(s -> {
            assertThat(s.usageKwh()).isEqualByComparingTo("7.5");
            assertThat(s.sampleCount()).isEqualTo(15);
            assertThat(s.quality()).isEqualTo(Quality.OK);
        });

        UsageStat hour = usageStatRepository.find1h(deviceId, DAY).orElseThrow();
        assertThat(hour.usageKwh()).isEqualByComparingTo("15.0");
        assertThat(hour.avgKw()).isEqualByComparingTo("30.00");
        assertThat(hour.peakDemandKw()).isEqualByComparingTo("30.00");
        assertThat(hour.sampleCount()).isEqualTo(30);
        assertThat(hour.quality()).isEqualTo(Quality.PARTIAL);

        UsageStat day = usageStatRepository.find1d(deviceId, LocalDate.parse("2000-01-01")).orElseThrow();
        assertThat(day.usageKwh()).isEqualByComparingTo("15.0");
        assertThat(day.peakDemandAt()).isEqualTo(DAY);
        assertThat(day.expectedCount()).isEqualTo(1440);
        assertThat(day.quality()).isEqualTo(Quality.PARTIAL);
    }

    @Test
    void 다시_계산하면_늦게_도착한_값이_반영된다() {
        long deviceId = deviceRepository.findIdByCode("AHU-01").orElseThrow();
        save(deviceId, DAY, new BigDecimal("100.0"));
        save(deviceId, DAY.plusMinutes(15), new BigDecimal("107.5"));
        usageStatService.recalculate(DAY, DAY.plusMinutes(15));
        assertThat(usageStatRepository.find15m(deviceId, DAY, DAY.plusMinutes(15)).getFirst().sampleCount()).isEqualTo(1);

        for (int m = 1; m < 15; m++) {
            save(deviceId, DAY.plusMinutes(m), new BigDecimal("100.0").add(new BigDecimal("0.5").multiply(BigDecimal.valueOf(m))));
        }
        usageStatService.recalculate(DAY, DAY.plusMinutes(15));

        UsageStat stat = usageStatRepository.find15m(deviceId, DAY, DAY.plusMinutes(15)).getFirst();
        assertThat(stat.sampleCount()).isEqualTo(15);
        assertThat(stat.usageKwh()).isEqualByComparingTo("7.5");
        assertThat(stat.quality()).isEqualTo(Quality.OK);
    }

    @Test
    void 값이_없는_장비는_MISSING_으로_남긴다() {
        long deviceId = deviceRepository.findIdByCode("LIGHT-01").orElseThrow();

        usageStatService.recalculate(DAY, DAY.plusMinutes(15));

        UsageStat stat = usageStatRepository.find15m(deviceId, DAY, DAY.plusMinutes(15)).getFirst();
        assertThat(stat.quality()).isEqualTo(Quality.MISSING);
        assertThat(stat.usageKwh()).isNull();
    }

    private void save(long deviceId, LocalDateTime measuredAt, BigDecimal cumulativeKwh) {
        MeterReading reading = new MeterReading("AHU-01", measuredAt.atOffset(ZoneOffset.ofHours(9)),
                cumulativeKwh, new BigDecimal("30.00"), null, MeterStatus.OK, List.of(), RunState.RUN);
        rawReadingRepository.upsert(deviceId, measuredAt, reading, LocalDateTime.now());
    }
}
