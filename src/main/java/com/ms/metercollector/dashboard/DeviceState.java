package com.ms.metercollector.dashboard;

import com.ms.metercollector.message.MeterStatus;
import com.ms.metercollector.reading.RawSample;

import java.time.Duration;
import java.time.LocalDateTime;

// 화면에 보여주는 장비의 현재 상태. 마지막으로 받은 값으로 판단한다.
public enum DeviceState {
    NORMAL("정상"),
    FAULT("이상"),
    MAINT("점검"),
    STALE("끊김"),     // 1분마다 오는 값이 STALE_AFTER 넘게 안 옴
    NONE("수신 없음");  // 받은 값이 하나도 없음

    static final Duration STALE_AFTER = Duration.ofMinutes(3);

    private final String label;

    DeviceState(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static DeviceState of(RawSample latest, LocalDateTime now) {
        if (latest == null) {
            return NONE;
        }
        if (latest.measuredAt().isBefore(now.minus(STALE_AFTER))) {
            return STALE;
        }
        if (latest.meterStatus() == MeterStatus.FAULT) {
            return FAULT;
        }
        return latest.meterStatus() == MeterStatus.MAINT ? MAINT : NORMAL;
    }
}
