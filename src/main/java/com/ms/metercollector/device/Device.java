package com.ms.metercollector.device;

public record Device(
        long deviceId,
        long buildingId,
        String deviceCode,
        String name,
        String category,
        boolean isMain
) {
}
