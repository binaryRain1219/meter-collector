package com.ms.metercollector.reading;

import com.ms.metercollector.device.DeviceRepository;
import com.ms.metercollector.message.MeterReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

// MQTT 메시지 한 건을 읽어 rawReading 에 저장한다.
// 형식이 틀렸거나 모르는 장비의 메시지는 다시 받아도 똑같이 실패하므로 로그만 남기고 버린다.
// DB 오류는 그대로 던져서 브로커가 다시 보내게 한다.
@Component
public class MeterReadingHandler {

    private static final Logger log = LoggerFactory.getLogger(MeterReadingHandler.class);

    // DB 의 DATETIME 은 한국 시각으로 저장한다.
    static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final JsonMapper jsonMapper;
    private final DeviceRepository deviceRepository;
    private final RawReadingRepository rawReadingRepository;
    private final Clock clock;

    @Autowired
    public MeterReadingHandler(JsonMapper jsonMapper,
                               DeviceRepository deviceRepository,
                               RawReadingRepository rawReadingRepository) {
        this(jsonMapper, deviceRepository, rawReadingRepository, Clock.system(ZONE));
    }

    MeterReadingHandler(JsonMapper jsonMapper,
                        DeviceRepository deviceRepository,
                        RawReadingRepository rawReadingRepository,
                        Clock clock) {
        this.jsonMapper = jsonMapper;
        this.deviceRepository = deviceRepository;
        this.rawReadingRepository = rawReadingRepository;
        this.clock = clock;
    }

    // 저장했으면 true.
    public boolean handle(String topic, byte[] payload) {
        LocalDateTime receivedAt = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);

        MeterReading reading;
        try {
            reading = jsonMapper.readValue(payload, MeterReading.class);
        } catch (JacksonException e) {
            log.warn("메시지 형식 오류로 버림: topic={}, payload={} ({})",
                    topic, new String(payload, StandardCharsets.UTF_8), e.getOriginalMessage());
            return false;
        }
        String missing = missingField(reading);
        if (missing != null) {
            log.warn("필수 값 {} 이(가) 없어 버림: topic={}", missing, topic);
            return false;
        }

        Optional<Long> deviceId = deviceRepository.findIdByCode(reading.deviceCode());
        if (deviceId.isEmpty()) {
            log.warn("등록되지 않은 장비라 버림: deviceCode={}, topic={}", reading.deviceCode(), topic);
            return false;
        }

        LocalDateTime measuredAt = reading.measuredAt().atZoneSameInstant(ZONE).toLocalDateTime();
        rawReadingRepository.upsert(deviceId.get(), measuredAt, reading, receivedAt);
        return true;
    }

    private static String missingField(MeterReading reading) {
        if (reading.deviceCode() == null || reading.deviceCode().isBlank()) {
            return "deviceCode";
        }
        if (reading.measuredAt() == null) {
            return "measuredAt";
        }
        if (reading.cumulativeKwh() == null) {
            return "cumulativeKwh";
        }
        if (reading.instantKw() == null) {
            return "instantKw";
        }
        return null;
    }
}
