package com.ms.metercollector.dashboard;

import com.ms.metercollector.building.Building;
import com.ms.metercollector.device.Device;
import com.ms.metercollector.reading.RawSample;
import com.ms.metercollector.stat.UsageStat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DashboardView(
        LocalDate today,
        LocalDateTime now,
        List<BuildingSummary> buildings,
        List<DeviceRow> devices,
        Chart chart
) {

    // 건물 요약은 메인 계량기 기준. 메인 계량기가 없으면 mainDevice 이하가 비어 있다.
    public record BuildingSummary(
            Building building,
            Device mainDevice,
            UsageStat today,          // 오늘 일 통계 (15분마다 갱신)
            BigDecimal currentKw,     // 메인 계량기 최근 순시전력
            BigDecimal peakRatio      // 오늘 최대수요전력 / 계약전력 × 100
    ) {
    }

    public record DeviceRow(
            Device device,
            RawSample latest,
            DeviceState state
    ) {
    }

    // 장비별 15분 평균전력. values 는 labels 와 같은 길이이고, 통계가 없는 구간은 null.
    public record Chart(List<String> labels, List<Series> series) {
    }

    public record Series(String deviceCode, String name, List<BigDecimal> values) {
    }
}
