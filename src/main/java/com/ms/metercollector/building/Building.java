package com.ms.metercollector.building;

import java.math.BigDecimal;

public record Building(
        long buildingId,
        String buildingCode,
        String name,
        BigDecimal contractKw
) {
}
