package com.ms.metercollector.dashboard;

import com.ms.metercollector.building.Building;
import com.ms.metercollector.building.BuildingRepository;
import com.ms.metercollector.dashboard.DashboardView.BuildingSummary;
import com.ms.metercollector.dashboard.DashboardView.Chart;
import com.ms.metercollector.dashboard.DashboardView.DeviceRow;
import com.ms.metercollector.dashboard.DashboardView.Series;
import com.ms.metercollector.device.Device;
import com.ms.metercollector.device.DeviceRepository;
import com.ms.metercollector.reading.RawReadingRepository;
import com.ms.metercollector.reading.RawSample;
import com.ms.metercollector.stat.UsageStat;
import com.ms.metercollector.stat.UsageStatRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final BuildingRepository buildingRepository;
    private final DeviceRepository deviceRepository;
    private final RawReadingRepository rawReadingRepository;
    private final UsageStatRepository usageStatRepository;

    public DashboardService(BuildingRepository buildingRepository,
                            DeviceRepository deviceRepository,
                            RawReadingRepository rawReadingRepository,
                            UsageStatRepository usageStatRepository) {
        this.buildingRepository = buildingRepository;
        this.deviceRepository = deviceRepository;
        this.rawReadingRepository = rawReadingRepository;
        this.usageStatRepository = usageStatRepository;
    }

    public DashboardView load(LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        List<Device> devices = deviceRepository.findAll();
        Map<Long, RawSample> latest = rawReadingRepository.findLatestByDevice();

        List<DeviceRow> rows = devices.stream()
                .map(d -> new DeviceRow(d, latest.get(d.deviceId()), DeviceState.of(latest.get(d.deviceId()), now)))
                .toList();

        List<BuildingSummary> buildings = buildingRepository.findAll().stream()
                .map(b -> summarize(b, devices, latest, today))
                .toList();

        return new DashboardView(today, now, buildings, rows, chart(devices, today));
    }

    private BuildingSummary summarize(Building building, List<Device> devices, Map<Long, RawSample> latest, LocalDate today) {
        Device main = devices.stream()
                .filter(d -> d.buildingId() == building.buildingId() && d.isMain())
                .findFirst().orElse(null);
        if (main == null) {
            return new BuildingSummary(building, null, null, null, null);
        }
        UsageStat stat = usageStatRepository.find1d(main.deviceId(), today).orElse(null);
        RawSample last = latest.get(main.deviceId());
        BigDecimal peakRatio = null;
        if (stat != null && stat.peakDemandKw() != null && building.contractKw().signum() > 0) {
            peakRatio = stat.peakDemandKw().multiply(BigDecimal.valueOf(100))
                    .divide(building.contractKw(), 1, RoundingMode.HALF_UP);
        }
        return new BuildingSummary(building, main, stat, last == null ? null : last.instantKw(), peakRatio);
    }

    private Chart chart(List<Device> devices, LocalDate today) {
        List<String> labels = new ArrayList<>();
        for (LocalTime t = LocalTime.MIDNIGHT; labels.size() < 96; t = t.plusMinutes(15)) {
            labels.add(t.toString());
        }
        List<Series> series = new ArrayList<>();
        for (Device device : devices) {
            Map<LocalDateTime, BigDecimal> avgByPeriod = new HashMap<>();
            for (UsageStat stat : usageStatRepository.find15m(device.deviceId(), today.atStartOfDay(), today.plusDays(1).atStartOfDay())) {
                avgByPeriod.put(stat.periodStart(), stat.avgKw());
            }
            List<BigDecimal> values = new ArrayList<>();
            for (int i = 0; i < labels.size(); i++) {
                values.add(avgByPeriod.get(today.atStartOfDay().plusMinutes(15L * i)));
            }
            series.add(new Series(device.deviceCode(), device.name(), values));
        }
        return new Chart(labels, series);
    }
}
