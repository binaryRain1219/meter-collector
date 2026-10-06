package com.ms.metercollector.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// meter.scheduler.enabled=false 면 스케줄러를 켜지 않는다. 테스트가 실제 통계 테이블을 건드리지 않게 하기 위함.
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "meter.scheduler.enabled", havingValue = "true", matchIfMissing = true)
class SchedulingConfig {
}
