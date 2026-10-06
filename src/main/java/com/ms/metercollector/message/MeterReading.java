package com.ms.metercollector.message;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

// meter-publisher 가 발행하는 MQTT 메시지 본문(JSON).
public record MeterReading(
        String deviceCode,
        OffsetDateTime measuredAt,
        BigDecimal cumulativeKwh,
        BigDecimal instantKw,
        Long seq,
        MeterStatus meterStatus,
        List<String> errorCodes,
        RunState runState
) {
}
