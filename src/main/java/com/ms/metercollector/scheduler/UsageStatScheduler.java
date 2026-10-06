package com.ms.metercollector.scheduler;

import com.ms.metercollector.stat.UsageStatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

// 15분마다 최근 1시간치 통계를 다시 계산한다.
@Component
public class UsageStatScheduler {

    private static final Logger log = LoggerFactory.getLogger(UsageStatScheduler.class);

    // 서버 시간대(EC2 는 UTC)와 상관없이 한국 시각 기준으로 구간을 나눈다.
    private static final String ZONE = "Asia/Seoul";

    // 이만큼 늦게 도착한 원본 값까지 통계에 반영된다.
    private static final int LOOKBACK_MINUTES = 60;

    private final UsageStatService usageStatService;

    public UsageStatScheduler(UsageStatService usageStatService) {
        this.usageStatService = usageStatService;
    }

    // 구간이 끝나고 1분 뒤(:01, :16, :31, :46)에 돈다. 구간 끝 시각(예: 14:30) 값이 도착할 시간을 준다.
    @Scheduled(cron = "0 1/15 * * * *", zone = ZONE)
    public void aggregate() {
        aggregateUntil(LocalDateTime.now(ZoneId.of(ZONE)));
    }

    void aggregateUntil(LocalDateTime now) {
        LocalDateTime to = UsageStatService.floorTo15m(now);
        LocalDateTime from = to.minusMinutes(LOOKBACK_MINUTES);
        usageStatService.recalculate(from, to);
        log.info("통계 재계산: [{}, {})", from, to);
    }
}
