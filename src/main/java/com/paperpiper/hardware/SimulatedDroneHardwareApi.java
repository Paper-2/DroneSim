package com.paperpiper.hardware;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.paperpiper.drone.Drone;

/**
 * Simulation-backed implementation of a single drone hardware endpoint.
 */
public class SimulatedDroneHardwareApi implements DroneHardwareApi {

    private final String droneId;
    private final Drone drone;

    public SimulatedDroneHardwareApi(String droneId, Drone drone) {
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
    public void setTargetPosition(HardwareVector3 targetPosition) {
        drone.setTargetPosition(new Vector3f(targetPosition.x(), targetPosition.y(), targetPosition.z()));
    }

    @Override
    public void setPositionHoldEnabled(boolean enabled) {
        drone.setPositionHoldEnabled(enabled);
    }

    @Override
    public DroneTelemetrySample readTelemetry() {
        Vector3f position = drone.getPosition();
        Vector3f velocity = drone.getVelocity();
        Vector3f acceleration = drone.getAcceleration();

        Vector3f angularVelocity = new Vector3f();
        Quaternion orientation = new Quaternion();
        if (drone.getRigidBody() != null) {
            angularVelocity = drone.getRigidBody().getAngularVelocity(null);
            orientation = drone.getRigidBody().getPhysicsRotation(null);
        }

        Vector3f targetPosition = drone.getTargetPosition();

        return new DroneTelemetrySample(
                droneId,
                toHardwareVector(position),
                toHardwareVector(velocity),
                toHardwareVector(acceleration),
                toHardwareVector(angularVelocity),
                new HardwareQuaternion(orientation.getX(), orientation.getY(), orientation.getZ(), orientation.getW()),
                targetPosition != null ? toHardwareVector(targetPosition) : null,
                drone.isPositionHoldEnabled(),
                System.currentTimeMillis()
        );
    }

    @Override
    public MotorOutputSample readMotorOutputs() {
        return new MotorOutputSample(
                droneId,
                drone.FL,
                drone.FR,
                drone.RL,
                drone.RR,
                drone.isMotorsArmed(),
                System.currentTimeMillis()
        );
    }

    private static HardwareVector3 toHardwareVector(Vector3f value) {
        return new HardwareVector3(value.x, value.y, value.z);
    }
}
