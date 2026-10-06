package com.ms.metercollector.stat;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// 통계는 다시 계산할 때마다 같은 키로 덮어쓴다. 늦게 도착한 원본 값을 반영하기 위함.
@Repository
public class UsageStatRepository {

    private static final RowMapper<UsageStat> STAT_15M_MAPPER = (rs, rowNum) -> {
        LocalDateTime periodStart = rs.getObject("periodStart", LocalDateTime.class);
        BigDecimal avgKw = rs.getBigDecimal("avgKw");
        return new UsageStat(
                rs.getLong("deviceId"),
                periodStart,
                rs.getBigDecimal("startCumulativeKwh"),
                rs.getBigDecimal("endCumulativeKwh"),
                rs.getBigDecimal("usageKwh"),
                avgKw,
                rs.getBigDecimal("maxKw"),
                rs.getBigDecimal("minKw"),
                avgKw,
                avgKw == null ? null : periodStart,
                rs.getInt("sampleCount"),
                rs.getInt("expectedCount"),
                Quality.valueOf(rs.getString("quality"))
        );
    };

    private static final RowMapper<UsageStat> STAT_1H_MAPPER = (rs, rowNum) -> new UsageStat(
            rs.getLong("deviceId"),
            rs.getObject("periodStart", LocalDateTime.class),
            rs.getBigDecimal("startCumulativeKwh"),
            rs.getBigDecimal("endCumulativeKwh"),
            rs.getBigDecimal("usageKwh"),
            rs.getBigDecimal("avgKw"),
            rs.getBigDecimal("maxKw"),
            rs.getBigDecimal("minKw"),
            rs.getBigDecimal("peakDemandKw"),
            null,
            rs.getInt("sampleCount"),
            rs.getInt("expectedCount"),
            Quality.valueOf(rs.getString("quality"))
    );

    private static final RowMapper<UsageStat> STAT_1D_MAPPER = (rs, rowNum) -> new UsageStat(
            rs.getLong("deviceId"),
            rs.getObject("statDate", LocalDate.class).atStartOfDay(),
            rs.getBigDecimal("startCumulativeKwh"),
            rs.getBigDecimal("endCumulativeKwh"),
            rs.getBigDecimal("usageKwh"),
            rs.getBigDecimal("avgKw"),
            rs.getBigDecimal("maxKw"),
            rs.getBigDecimal("minKw"),
            rs.getBigDecimal("peakDemandKw"),
            rs.getObject("peakDemandAt", LocalDateTime.class),
            rs.getInt("sampleCount"),
            rs.getInt("expectedCount"),
            Quality.valueOf(rs.getString("quality"))
    );

    private final JdbcTemplate jdbcTemplate;

    public UsageStatRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert15m(UsageStat stat) {
        jdbcTemplate.update("""
                        INSERT INTO usageStat15m
                            (deviceId, periodStart, startCumulativeKwh, endCumulativeKwh, usageKwh,
                             avgKw, maxKw, minKw, sampleCount, expectedCount, quality)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) AS new
                        ON DUPLICATE KEY UPDATE
                            startCumulativeKwh = new.startCumulativeKwh,
                            endCumulativeKwh = new.endCumulativeKwh,
                            usageKwh = new.usageKwh,
                            avgKw = new.avgKw,
                            maxKw = new.maxKw,
                            minKw = new.minKw,
                            sampleCount = new.sampleCount,
                            expectedCount = new.expectedCount,
                            quality = new.quality
                        """,
                stat.deviceId(), stat.periodStart(),
                stat.startCumulativeKwh(), stat.endCumulativeKwh(), stat.usageKwh(),
                stat.avgKw(), stat.maxKw(), stat.minKw(),
                stat.sampleCount(), stat.expectedCount(), stat.quality().name()
        );
    }

    public void upsert1h(UsageStat stat) {
        jdbcTemplate.update("""
                        INSERT INTO usageStat1h
                            (deviceId, periodStart, startCumulativeKwh, endCumulativeKwh, usageKwh,
                             avgKw, maxKw, minKw, peakDemandKw, sampleCount, expectedCount, quality)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) AS new
                        ON DUPLICATE KEY UPDATE
                            startCumulativeKwh = new.startCumulativeKwh,
                            endCumulativeKwh = new.endCumulativeKwh,
                            usageKwh = new.usageKwh,
                            avgKw = new.avgKw,
                            maxKw = new.maxKw,
                            minKw = new.minKw,
                            peakDemandKw = new.peakDemandKw,
                            sampleCount = new.sampleCount,
                            expectedCount = new.expectedCount,
                            quality = new.quality
                        """,
                stat.deviceId(), stat.periodStart(),
                stat.startCumulativeKwh(), stat.endCumulativeKwh(), stat.usageKwh(),
                stat.avgKw(), stat.maxKw(), stat.minKw(), stat.peakDemandKw(),
                stat.sampleCount(), stat.expectedCount(), stat.quality().name()
        );
    }

    public void upsert1d(UsageStat stat) {
        jdbcTemplate.update("""
                        INSERT INTO usageStat1d
                            (deviceId, statDate, startCumulativeKwh, endCumulativeKwh, usageKwh,
                             avgKw, maxKw, minKw, peakDemandKw, peakDemandAt, sampleCount, expectedCount, quality)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) AS new
                        ON DUPLICATE KEY UPDATE
                            startCumulativeKwh = new.startCumulativeKwh,
                            endCumulativeKwh = new.endCumulativeKwh,
                            usageKwh = new.usageKwh,
                            avgKw = new.avgKw,
                            maxKw = new.maxKw,
                            minKw = new.minKw,
                            peakDemandKw = new.peakDemandKw,
                            peakDemandAt = new.peakDemandAt,
                            sampleCount = new.sampleCount,
                            expectedCount = new.expectedCount,
                            quality = new.quality
                        """,
                stat.deviceId(), stat.periodStart().toLocalDate(),
                stat.startCumulativeKwh(), stat.endCumulativeKwh(), stat.usageKwh(),
                stat.avgKw(), stat.maxKw(), stat.minKw(), stat.peakDemandKw(), stat.peakDemandAt(),
                stat.sampleCount(), stat.expectedCount(), stat.quality().name()
        );
    }

    // periodStart 가 [from, to) 인 15분 통계, 시각 순.
    public List<UsageStat> find15m(long deviceId, LocalDateTime from, LocalDateTime to) {
        return jdbcTemplate.query("""
                        SELECT * FROM usageStat15m
                        WHERE deviceId = ? AND periodStart >= ? AND periodStart < ?
                        ORDER BY periodStart
                        """,
                STAT_15M_MAPPER, deviceId, from, to
        );
    }

    public Optional<UsageStat> find1h(long deviceId, LocalDateTime periodStart) {
        return jdbcTemplate.query(
                "SELECT * FROM usageStat1h WHERE deviceId = ? AND periodStart = ?",
                STAT_1H_MAPPER, deviceId, periodStart
        ).stream().findFirst();
    }

    public Optional<UsageStat> find1d(long deviceId, LocalDate statDate) {
        return jdbcTemplate.query(
                "SELECT * FROM usageStat1d WHERE deviceId = ? AND statDate = ?",
                STAT_1D_MAPPER, deviceId, statDate
        ).stream().findFirst();
    }

    // periodStart 가 [from, to) 인 1시간 통계, 시각 순.
    public List<UsageStat> find1h(long deviceId, LocalDateTime from, LocalDateTime to) {
        return jdbcTemplate.query("""
                        SELECT * FROM usageStat1h
                        WHERE deviceId = ? AND periodStart >= ? AND periodStart < ?
                        ORDER BY periodStart
                        """,
                STAT_1H_MAPPER, deviceId, from, to
        );
    }

    // statDate 가 [from, to] 인 일 통계, 날짜 순.
    public List<UsageStat> find1d(long deviceId, LocalDate from, LocalDate to) {
        return jdbcTemplate.query("""
                        SELECT * FROM usageStat1d
                        WHERE deviceId = ? AND statDate BETWEEN ? AND ?
                        ORDER BY statDate
                        """,
                STAT_1D_MAPPER, deviceId, from, to
        );
    }
}
