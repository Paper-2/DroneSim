package com.paperpiper.server;

import java.util.concurrent.atomic.AtomicLong;

import com.paperpiper.hardware.DroneHardwareApi;
import com.paperpiper.hardware.DroneTelemetrySample;
import com.paperpiper.hardware.HardwareQuaternion;
import com.paperpiper.hardware.HardwareVector3;
import com.paperpiper.hardware.ManualControlCommand;
import com.paperpiper.hardware.MotorOutputSample;

/**
 * Pure-software drone model used by the protocol server when no real simulator
 * backend is attached.
 */
public class SyntheticDroneHardwareApi implements DroneHardwareApi {

    private static final float DT_SECONDS = 0.05f;

    private final String droneId;
    private final AtomicLong tick = new AtomicLong(0);

    private volatile boolean armed;
    private volatile boolean positionHoldEnabled;
    private volatile HardwareVector3 targetPosition = new HardwareVector3(0f, 2f, 0f);
    private volatile ManualControlCommand controlCommand = new ManualControlCommand(0.5f, 0f, 0f, 0f);

    public SyntheticDroneHardwareApi(String droneId) {
        this.droneId = droneId;
    }

    @Override
    public String getDroneId() {
        return droneId;
    }

    @Override
    public void setArmed(boolean armed) {
        this.armed = armed;
    }

    @Override
    public void applyManualControl(ManualControlCommand command) {
        this.controlCommand = command;
    }

    @Override
    public void setTargetPosition(HardwareVector3 targetPosition) {
        this.targetPosition = targetPosition;
        this.positionHoldEnabled = true;
    }

    @Override
    public void setPositionHoldEnabled(boolean enabled) {
        this.positionHoldEnabled = enabled;
    }

    @Override
    public DroneTelemetrySample readTelemetry() {
        long currentTick = tick.incrementAndGet();
        float t = currentTick * DT_SECONDS;

        float radius = 5f;
        float baseX = (float) Math.cos(t) * radius;
        float baseY = 2.0f + (float) Math.sin(t * 0.5f) * 0.5f;
        float baseZ = (float) Math.sin(t) * radius;

        float vx = -(float) Math.sin(t) * radius;
        float vy = (float) Math.cos(t * 0.5f) * 0.25f;
        float vz = (float) Math.cos(t) * radius;

        float ax = -(float) Math.cos(t) * radius;
        float ay = -(float) Math.sin(t * 0.5f) * 0.125f;
        float az = -(float) Math.sin(t) * radius;

        float yaw = t * 0.2f;
        float halfYaw = yaw * 0.5f;
        HardwareQuaternion orientation = new HardwareQuaternion(0f, (float) Math.sin(halfYaw), 0f, (float) Math.cos(halfYaw));

        return new DroneTelemetrySample(
                droneId,
                new HardwareVector3(baseX, baseY, baseZ),
                new HardwareVector3(vx, vy, vz),
                new HardwareVector3(ax, ay, az),
                new HardwareVector3(0f, 0.2f, 0f),
                orientation,
                positionHoldEnabled ? targetPosition : null,
                positionHoldEnabled,
                System.currentTimeMillis()
        );
    }

    @Override
    public MotorOutputSample readMotorOutputs() {
        float base = armed ? controlCommand.throttle() : 0f;
        float pitchMix = controlCommand.pitch() * 0.1f;
        float rollMix = controlCommand.roll() * 0.1f;
        float yawMix = controlCommand.yaw() * 0.05f;

        return new MotorOutputSample(
                droneId,
                clamp(base + pitchMix + rollMix + yawMix),
                clamp(base + pitchMix - rollMix - yawMix),
                clamp(base - pitchMix + rollMix - yawMix),
                clamp(base - pitchMix - rollMix + yawMix),
                armed,
                System.currentTimeMillis()
        );
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
