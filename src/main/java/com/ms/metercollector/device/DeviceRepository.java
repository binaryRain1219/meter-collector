package com.ms.metercollector.device;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DeviceRepository {

    private static final RowMapper<Device> ROW_MAPPER = (rs, rowNum) -> new Device(
            rs.getLong("deviceId"),
            rs.getLong("buildingId"),
            rs.getString("deviceCode"),
            rs.getString("name"),
            rs.getString("category"),
            rs.getBoolean("isMain")
    );

    private final JdbcTemplate jdbcTemplate;

    public DeviceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Device> findAll() {
        return jdbcTemplate.query("""
                SELECT deviceId, buildingId, deviceCode, name, category, isMain
                FROM device
                ORDER BY deviceId
                """, ROW_MAPPER);
    }
}
