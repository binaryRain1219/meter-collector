package com.ms.metercollector.stat;

import com.ms.metercollector.device.Device;
import com.ms.metercollector.device.DeviceRepository;
import com.ms.metercollector.reading.RawReadingRepository;
import com.ms.metercollector.reading.RawSample;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class UsageStatService {

    static final int EXPECTED_1H = 60;
    static final int EXPECTED_1D = 1440;

    private final DeviceRepository deviceRepository;
    private final RawReadingRepository rawReadingRepository;
    private final UsageStatRepository usageStatRepository;

    public UsageStatService(DeviceRepository deviceRepository,
                            RawReadingRepository rawReadingRepository,
                            UsageStatRepository usageStatRepository) {
        this.deviceRepository = deviceRepository;
        this.rawReadingRepository = rawReadingRepository;
        this.usageStatRepository = usageStatRepository;
    }

    /**
     * [from, to) 에 걸친 15분 통계를 원본 값으로 다시 만들고, 그 구간이 속한 1시간/일 통계도 다시 만든다.
     * 같은 구간을 몇 번 돌려도 결과가 같다 (upsert). 지난 기간을 다시 채울 때도 이 메서드를 쓴다.
     *
     * <p>1시간·일 통계 모두 15분 통계를 합쳐 만든다. 일 통계를 1시간 통계에서 만들면 최대수요전력이
     * 나온 15분 구간(peakDemandAt)을 알 수 없어서다. 합계/가중평균/최대/최소는 어느 쪽으로 합쳐도 같다.
     */
    public void recalculate(LocalDateTime from, LocalDateTime to) {
        LocalDateTime start = floorTo15m(from);
        for (Device device : deviceRepository.findAll()) {
            long deviceId = device.deviceId();

            for (LocalDateTime period = start; period.isBefore(to); period = period.plusMinutes(UsageStatCalculator.MINUTES_15)) {
                RawSample before = rawReadingRepository.findLatestAtOrBefore(deviceId, period).orElse(null);
                List<RawSample> window = rawReadingRepository.findBetween(
                        deviceId, period, period.plusMinutes(UsageStatCalculator.MINUTES_15));
                usageStatRepository.upsert15m(UsageStatCalculator.of15m(deviceId, period, before, window));
            }

            for (LocalDateTime hour = start.truncatedTo(ChronoUnit.HOURS); hour.isBefore(to); hour = hour.plusHours(1)) {
                List<UsageStat> children = usageStatRepository.find15m(deviceId, hour, hour.plusHours(1));
                usageStatRepository.upsert1h(UsageStatCalculator.rollUp(deviceId, hour, EXPECTED_1H, children));
            }

            for (LocalDate day = start.toLocalDate(); day.atStartOfDay().isBefore(to); day = day.plusDays(1)) {
                List<UsageStat> children = usageStatRepository.find15m(
                        deviceId, day.atStartOfDay(), day.plusDays(1).atStartOfDay());
                usageStatRepository.upsert1d(UsageStatCalculator.rollUp(deviceId, day.atStartOfDay(), EXPECTED_1D, children));
            }
        }
    }

    public static LocalDateTime floorTo15m(LocalDateTime time) {
        LocalDateTime hour = time.truncatedTo(ChronoUnit.HOURS);
        return hour.plusMinutes(time.getMinute() / UsageStatCalculator.MINUTES_15 * UsageStatCalculator.MINUTES_15);
    }
}
