package com.paperpiper.client;

/**
 * Telemetry payload for a single drone.
 */
public record DroneTelemetry(
        String droneId,
        float posX,
        float posY,
        float posZ,
        float velX,
        float velY,
        float velZ,
        long timestampMillis
        ) {

}
