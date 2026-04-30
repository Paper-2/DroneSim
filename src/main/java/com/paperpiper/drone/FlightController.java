package com.paperpiper.drone;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public interface FlightController {

    void update(float throttle,
            float rollInput,
            float pitchInput,
            float yawInput,
            Quaternion orientation,
            Vector3f angularVelocity,
            Vector3f position,
            Vector3f velocity,
            Vector3f acceleration,
            float deltaTime);

    float getMotorFL();

    float getMotorFR();

    float getMotorRL();

    float getMotorRR();

    /**
     * World-space target the controller is currently flying toward, or
     * {@code null}.
     */
    Vector3f getTargetPosition();

    /**
     * Sets the world-space target the controller should fly toward.
     */
    void setTargetPosition(Vector3f target);

    /**
     * True if position-hold is engaged.
     */
    boolean isPositionHoldEnabled();

    /**
     * Enables/disables position-hold mode.
     */
    void setPositionHoldEnabled(boolean enabled);

    /**
     * Reset all internal state Called on disarm so the controller starts fresh
     * on the next arm.
     */
    void reset();

    default void setFlightMode(FlightMode mode) {
        // no-op default
    }
}
