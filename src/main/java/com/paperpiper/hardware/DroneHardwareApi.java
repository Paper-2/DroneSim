package com.paperpiper.hardware;

/**
 * Hardware abstraction for one drone.
 *
 * This contract is designed to be consumed by higher-level controller without
 * depending directly on simulation internals.
 */
public interface DroneHardwareApi {

    String getDroneId();

    void setArmed(boolean armed);

    void applyManualControl(ManualControlCommand command);

    void setTargetPosition(HardwareVector3 targetPosition);

    void setPositionHoldEnabled(boolean enabled);

    DroneTelemetrySample readTelemetry();

    MotorOutputSample readMotorOutputs();
}
