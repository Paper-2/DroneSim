package com.paperpiper.ross;

/**
 * Wire-format telemetry payload shared between client and server.
 */
public record TelemetryData(
        String droneId,
        float posX,
        float posY,
        float posZ,
        float velX,
        float velY,
        float velZ,
        long timestampMillis) {

}
