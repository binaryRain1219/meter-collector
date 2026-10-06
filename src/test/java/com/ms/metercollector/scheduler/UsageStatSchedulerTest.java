package com.ms.metercollector.scheduler;

import com.ms.metercollector.stat.UsageStatService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class UsageStatSchedulerTest {

    private final UsageStatService usageStatService = mock(UsageStatService.class);
    private final UsageStatScheduler scheduler = new UsageStatScheduler(usageStatService);

    @Test
    void 끝난_구간까지_최근_1시간치를_다시_계산한다() {
        scheduler.aggregateUntil(LocalDateTime.parse("2026-10-06T14:31:00.123"));

        verify(usageStatService).recalculate(
                LocalDateTime.parse("2026-10-06T13:30:00"),
                LocalDateTime.parse("2026-10-06T14:30:00"));
    }
}
