package com.ms.metercollector.reading;

import com.ms.metercollector.device.DeviceRepository;
import com.ms.metercollector.message.MeterReading;
import com.ms.metercollector.message.MeterStatus;
import com.ms.metercollector.message.RunState;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MeterReadingHandlerTest {

    private static final String TOPIC = "meter/AHU-01/energy";

    // meter-publisher 가 실제로 보내는 형식
    private static final String PAYLOAD = """
            {
              "deviceCode": "AHU-01",
              "measuredAt": "2026-10-06T14:15:00+09:00",
              "cumulativeKwh": 1234.500,
              "instantKw": 12.30,
              "seq": 42,
              "meterStatus": "OK",
              "errorCodes": [],
              "runState": "RUN"
            }
            """;

    private final DeviceRepository deviceRepository = mock(DeviceRepository.class);
    private final RawReadingRepository rawReadingRepository = mock(RawReadingRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-06T05:15:01.234Z"), MeterReadingHandler.ZONE);
    private final MeterReadingHandler handler =
            new MeterReadingHandler(JsonMapper.builder().build(), deviceRepository, rawReadingRepository, clock);

    @Test
    void 메시지를_읽어_한국시각으로_저장한다() {
        when(deviceRepository.findIdByCode("AHU-01")).thenReturn(Optional.of(2L));

        assertThat(handler.handle(TOPIC, bytes(PAYLOAD))).isTrue();

        ArgumentCaptor<MeterReading> reading = ArgumentCaptor.forClass(MeterReading.class);
        verify(rawReadingRepository).upsert(
                eq(2L),
                eq(LocalDateTime.parse("2026-10-06T14:15:00")),
                reading.capture(),
                eq(LocalDateTime.parse("2026-10-06T14:15:01.234")));
        assertThat(reading.getValue().cumulativeKwh()).isEqualByComparingTo("1234.500");
        assertThat(reading.getValue().instantKw()).isEqualByComparingTo("12.30");
        assertThat(reading.getValue().seq()).isEqualTo(42);
        assertThat(reading.getValue().meterStatus()).isEqualTo(MeterStatus.OK);
        assertThat(reading.getValue().errorCodes()).isEqualTo(List.of());
        assertThat(reading.getValue().runState()).isEqualTo(RunState.RUN);
    }

    @Test
    void 다른_시간대로_와도_한국시각으로_바꿔_저장한다() {
        when(deviceRepository.findIdByCode("AHU-01")).thenReturn(Optional.of(2L));

        handler.handle(TOPIC, bytes(PAYLOAD.replace("2026-10-06T14:15:00+09:00", "2026-10-06T05:15:00Z")));

        verify(rawReadingRepository).upsert(eq(2L), eq(LocalDateTime.parse("2026-10-06T14:15:00")), any(), any());
    }

    @Test
    void 등록되지_않은_장비의_메시지는_버린다() {
        when(deviceRepository.findIdByCode("AHU-01")).thenReturn(Optional.empty());

        assertThat(handler.handle(TOPIC, bytes(PAYLOAD))).isFalse();

        verify(rawReadingRepository, never()).upsert(anyLong(), any(), any(), any());
    }

    @Test
    void JSON_이_아니거나_필수_값이_없으면_버린다() {
        assertThat(handler.handle(TOPIC, bytes("not json"))).isFalse();
        assertThat(handler.handle(TOPIC, bytes(PAYLOAD.replace("\"cumulativeKwh\": 1234.500,", "")))).isFalse();
        assertThat(handler.handle(TOPIC, bytes(PAYLOAD.replace("\"OK\"", "\"BROKEN\"")))).isFalse();

        verify(rawReadingRepository, never()).upsert(anyLong(), any(), any(), any());
    }

    @Test
    void DB_오류는_던져서_브로커가_다시_보내게_한다() {
        when(deviceRepository.findIdByCode("AHU-01")).thenReturn(Optional.of(2L));
        doThrow(new DataAccessResourceFailureException("DB down"))
                .when(rawReadingRepository).upsert(anyLong(), any(), any(), any());

        assertThatThrownBy(() -> handler.handle(TOPIC, bytes(PAYLOAD)))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
