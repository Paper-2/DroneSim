package com.paperpiper.server;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.paperpiper.drone.Drone;
import com.paperpiper.hardware.DroneHardwareApi;
import com.paperpiper.hardware.DroneTelemetrySample;
import com.paperpiper.hardware.HardwareQuaternion;
import com.paperpiper.hardware.HardwareVector3;
import com.paperpiper.hardware.ManualControlCommand;
import com.paperpiper.hardware.MotorOutputSample;

/**
 * Adapts a live {@link Drone} instance from the simulation engine to the
 * {@link DroneHardwareApi} interface used by the ROSS server.
 */
public class LiveDroneHardwareApi implements DroneHardwareApi {

    private final String droneId;
    private final Drone drone;

    public LiveDroneHardwareApi(String droneId, Drone drone) {
        this.droneId = droneId;
        this.drone = drone;
    }

    @Override
    public String getDroneId() {
        return droneId;
    }

    @Override
    public void setArmed(boolean armed) {
        drone.setMotorsArmed(armed);
    }

    @Override
    public void applyManualControl(ManualControlCommand command) {
        drone.setThrottle(command.throttle());
        drone.setPitch(command.pitch());
        drone.setRoll(command.roll());
        drone.setYaw(command.yaw());
    }

    @Override
    public void setTargetPosition(HardwareVector3 target) {
        drone.setTargetPosition(new Vector3f(target.x(), target.y(), target.z()));
    }

    @Override
    public void setPositionHoldEnabled(boolean enabled) {
        drone.setPositionHoldEnabled(enabled);
    }

    @Override
    public DroneTelemetrySample readTelemetry() {
        Vector3f pos = drone.getPosition();
        Vector3f vel = drone.getVelocity();
        Vector3f acc = drone.getAcceleration();

        // Angular velocity from the rigid body
        Vector3f angVel = new Vector3f();
        if (drone.getRigidBody() != null) {
            drone.getRigidBody().getAngularVelocity(angVel);
        }

        // Orientation quaternion
        Quaternion rot = new Quaternion();
        if (drone.getRigidBody() != null) {
            drone.getRigidBody().getPhysicsRotation(rot);
        }

        Vector3f target = drone.getTargetPosition();
        HardwareVector3 targetHw = target != null
                ? new HardwareVector3(target.x, target.y, target.z)
                : null;

        return new DroneTelemetrySample(
                droneId,
                new HardwareVector3(pos.x, pos.y, pos.z),
                new HardwareVector3(vel.x, vel.y, vel.z),
                new HardwareVector3(acc.x, acc.y, acc.z),
                new HardwareVector3(angVel.x, angVel.y, angVel.z),
                new HardwareQuaternion(rot.getX(), rot.getY(), rot.getZ(), rot.getW()),
                targetHw,
                drone.isPositionHoldEnabled(),
                System.currentTimeMillis()
        );
    }

    @Override
    public MotorOutputSample readMotorOutputs() {
        return new MotorOutputSample(
                droneId,
                drone.FL, drone.FR, drone.RL, drone.RR,
                drone.isMotorsArmed(),
                System.currentTimeMillis()
        );
    }
}
