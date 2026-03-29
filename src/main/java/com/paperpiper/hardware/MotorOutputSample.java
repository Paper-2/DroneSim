package com.paperpiper.hardware;

/**
 * Per-motor output snapshot in normalized range [0, 1].
 */
public record MotorOutputSample(
        String droneId,
        float frontLeft,
        float frontRight,
        float rearLeft,
        float rearRight,
        boolean motorsArmed,
        long timestampMillis
        ) {

}
