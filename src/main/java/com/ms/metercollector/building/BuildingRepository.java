package com.ms.metercollector.building;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class BuildingRepository {

    private static final RowMapper<Building> ROW_MAPPER = (rs, rowNum) -> new Building(
            rs.getLong("buildingId"),
            rs.getString("buildingCode"),
            rs.getString("name"),
            rs.getBigDecimal("contractKw")
    );

    private final JdbcTemplate jdbcTemplate;

    public BuildingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Building> findAll() {
        return jdbcTemplate.query("""
                SELECT buildingId, buildingCode, name, contractKw
                FROM building
                ORDER BY buildingId
                """, ROW_MAPPER);
    }
}
