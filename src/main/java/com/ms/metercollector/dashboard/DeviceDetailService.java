package com.ms.metercollector.dashboard;

import com.ms.metercollector.dashboard.DeviceDetailView.DailyPoint;
import com.ms.metercollector.dashboard.DeviceDetailView.PeriodRow;
import com.ms.metercollector.device.Device;
import com.ms.metercollector.device.DeviceRepository;
import com.ms.metercollector.stat.UsageStat;
import com.ms.metercollector.stat.UsageStatRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DeviceDetailService {

    private final DeviceRepository deviceRepository;
    private final UsageStatRepository usageStatRepository;

    public DeviceDetailService(DeviceRepository deviceRepository, UsageStatRepository usageStatRepository) {
        this.deviceRepository = deviceRepository;
        this.usageStatRepository = usageStatRepository;
    }

    public Optional<DeviceDetailView> load(String deviceCode, LocalDate date, StatUnit unit, LocalDate today) {
        return deviceRepository.findByCode(deviceCode).map(device -> load(device, date, unit, today));
    }

    private DeviceDetailView load(Device device, LocalDate date, StatUnit unit, LocalDate today) {
        long deviceId = device.deviceId();
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();

        List<UsageStat> stats = unit == StatUnit.H1
                ? usageStatRepository.find1h(deviceId, from, to)
                : usageStatRepository.find15m(deviceId, from, to);
        Map<LocalDateTime, UsageStat> byPeriod = stats.stream()
                .collect(Collectors.toMap(UsageStat::periodStart, Function.identity()));
        List<PeriodRow> periods = new ArrayList<>();
        for (LocalDateTime p = from; p.isBefore(to); p = p.plusMinutes(unit.minutes())) {
            periods.add(new PeriodRow(p, p.plusMinutes(unit.minutes()), byPeriod.get(p)));
        }

        LocalDate dailyFrom = date.minusDays(DeviceDetailView.DAILY_DAYS - 1);
        Map<LocalDate, UsageStat> byDate = usageStatRepository.find1d(deviceId, dailyFrom, date).stream()
                .collect(Collectors.toMap(s -> s.periodStart().toLocalDate(), Function.identity()));
        List<DailyPoint> daily = new ArrayList<>();
        for (LocalDate d = dailyFrom; !d.isAfter(date); d = d.plusDays(1)) {
            UsageStat stat = byDate.get(d);
            daily.add(new DailyPoint(d, stat == null ? null : stat.usageKwh(), stat == null ? null : stat.quality().label()));
        }

        return new DeviceDetailView(
                device, date, unit,
                byDate.get(date),
                periods, daily,
                date.minusDays(1),
                date.isBefore(today) ? date.plusDays(1) : null
        );
    }
}
