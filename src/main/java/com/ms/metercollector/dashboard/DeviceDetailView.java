package com.ms.metercollector.dashboard;

import com.ms.metercollector.device.Device;
import com.ms.metercollector.stat.UsageStat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DeviceDetailView(
        Device device,
        LocalDate date,
        StatUnit unit,
        UsageStat day,                 // 그날 일 통계. 없으면 null
        List<PeriodRow> periods,       // 그날의 모든 구간. 아직 계산 안 된 구간은 stat 이 null
        List<DailyPoint> daily,        // date 까지 최근 DAILY_DAYS 일
        LocalDate previousDate,
        LocalDate nextDate             // 오늘이면 null
) {

    public static final int DAILY_DAYS = 30;

    public record PeriodRow(LocalDateTime periodStart, LocalDateTime periodEnd, UsageStat stat) {
    }

    public record DailyPoint(LocalDate date, BigDecimal usageKwh, String quality) {
    }

    // 그래프에 넘기는 값만 모은 것.
    public record Chart(List<String> periodLabels, List<BigDecimal> periodUsage,
                        List<String> dailyLabels, List<BigDecimal> dailyUsage) {
    }

    public Chart chart() {
        return new Chart(
                periods.stream().map(p -> p.periodStart().toLocalTime().toString()).toList(),
                periods.stream().map(p -> p.stat() == null ? null : p.stat().usageKwh()).toList(),
                daily.stream().map(d -> d.date().toString()).toList(),
                daily.stream().map(DailyPoint::usageKwh).toList()
        );
    }
}
