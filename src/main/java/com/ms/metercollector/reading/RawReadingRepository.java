package com.ms.metercollector.reading;

import com.ms.metercollector.message.MeterReading;
import com.ms.metercollector.message.MeterStatus;
import com.ms.metercollector.message.RunState;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Repository
public class RawReadingRepository {

    private static final RowMapper<RawSample> SAMPLE_MAPPER = (rs, rowNum) -> new RawSample(
            rs.getObject("measuredAt", LocalDateTime.class),
            rs.getBigDecimal("cumulativeKwh"),
            rs.getBigDecimal("instantKw"),
            MeterStatus.valueOf(rs.getString("meterStatus"))
    );

    private final JdbcTemplate jdbcTemplate;

    public RawReadingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // QoS 1 은 같은 메시지가 두 번 올 수 있어 upsert 로 흡수한다. receivedAt 은 처음 받은 시각을 남긴다.
    public void upsert(long deviceId, LocalDateTime measuredAt, MeterReading reading, LocalDateTime receivedAt) {
        List<String> errorCodes = Objects.requireNonNullElse(reading.errorCodes(), List.of());
        jdbcTemplate.update("""
                        INSERT INTO rawReading
                            (deviceId, measuredAt, cumulativeKwh, instantKw, seq, meterStatus, errorCodes, runState, receivedAt)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) AS new
                        ON DUPLICATE KEY UPDATE
                            cumulativeKwh = new.cumulativeKwh,
                            instantKw = new.instantKw,
                            seq = new.seq,
                            meterStatus = new.meterStatus,
                            errorCodes = new.errorCodes,
                            runState = new.runState
                        """,
                deviceId,
                measuredAt,
                reading.cumulativeKwh(),
                reading.instantKw(),
                reading.seq(),
                Objects.requireNonNullElse(reading.meterStatus(), MeterStatus.OK).name(),
                errorCodes.isEmpty() ? null : String.join(",", errorCodes),
                Objects.requireNonNullElse(reading.runState(), RunState.UNKNOWN).name(),
                receivedAt
        );
    }

    // at 이전(같은 시각 포함)의 가장 최근 값. 구간 시작 시점 누적값으로 쓴다.
    public Optional<RawSample> findLatestAtOrBefore(long deviceId, LocalDateTime at) {
        return jdbcTemplate.query("""
                        SELECT measuredAt, cumulativeKwh, instantKw, meterStatus
                        FROM rawReading
                        WHERE deviceId = ? AND measuredAt <= ?
                        ORDER BY measuredAt DESC
                        LIMIT 1
                        """,
                SAMPLE_MAPPER, deviceId, at
        ).stream().findFirst();
    }

    // [from, to] 양 끝 포함, 시각 순.
    public List<RawSample> findBetween(long deviceId, LocalDateTime from, LocalDateTime to) {
        return jdbcTemplate.query("""
                        SELECT measuredAt, cumulativeKwh, instantKw, meterStatus
                        FROM rawReading
                        WHERE deviceId = ? AND measuredAt BETWEEN ? AND ?
                        ORDER BY measuredAt
                        """,
                SAMPLE_MAPPER, deviceId, from, to
        );
    }

    // 장비별 가장 최근 값. deviceId → 값. 값이 하나도 없는 장비는 빠진다.
    public Map<Long, RawSample> findLatestByDevice() {
        Map<Long, RawSample> latest = new HashMap<>();
        jdbcTemplate.query("""
                        SELECT r.deviceId, r.measuredAt, r.cumulativeKwh, r.instantKw, r.meterStatus
                        FROM rawReading r
                        JOIN (SELECT deviceId, MAX(measuredAt) AS measuredAt FROM rawReading GROUP BY deviceId) last
                          ON last.deviceId = r.deviceId AND last.measuredAt = r.measuredAt
                        """,
                rs -> {
                    latest.put(rs.getLong("deviceId"), SAMPLE_MAPPER.mapRow(rs, 0));
                }
        );
        return latest;
    }
}
