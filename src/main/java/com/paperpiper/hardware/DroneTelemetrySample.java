package com.paperpiper.hardware;

/**
 * Simulated onboard state snapshot for one drone.
 */
public record DroneTelemetrySample(
        String droneId,
        HardwareVector3 position,
        HardwareVector3 linearVelocity,
        HardwareVector3 linearAcceleration,
        HardwareVector3 angularVelocity,
        HardwareQuaternion orientation,
        HardwareVector3 targetPosition,
        boolean positionHoldEnabled,
        long timestampMillis
        ) {

}
