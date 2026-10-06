package com.ms.metercollector.dashboard;

import com.ms.metercollector.message.MeterStatus;
import com.ms.metercollector.reading.RawSample;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceStateTest {

    private static final LocalDateTime NOW = LocalDateTime.parse("2026-10-06T14:15:30");

    @Test
    void 최근_값의_계량기_상태를_따른다() {
        assertThat(DeviceState.of(sample(NOW.minusMinutes(1), MeterStatus.OK), NOW)).isEqualTo(DeviceState.NORMAL);
        assertThat(DeviceState.of(sample(NOW.minusMinutes(1), MeterStatus.FAULT), NOW)).isEqualTo(DeviceState.FAULT);
        assertThat(DeviceState.of(sample(NOW.minusMinutes(1), MeterStatus.MAINT), NOW)).isEqualTo(DeviceState.MAINT);
    }

    @Test
    void 삼분_넘게_값이_없으면_끊김_한_번도_없으면_수신_없음() {
        assertThat(DeviceState.of(sample(NOW.minusMinutes(3), MeterStatus.OK), NOW)).isEqualTo(DeviceState.NORMAL);
        assertThat(DeviceState.of(sample(NOW.minusMinutes(4), MeterStatus.FAULT), NOW)).isEqualTo(DeviceState.STALE);
        assertThat(DeviceState.of(null, NOW)).isEqualTo(DeviceState.NONE);
    }

    private static RawSample sample(LocalDateTime measuredAt, MeterStatus status) {
        return new RawSample(measuredAt, new BigDecimal("100.000"), new BigDecimal("30.00"), status);
    }
}
