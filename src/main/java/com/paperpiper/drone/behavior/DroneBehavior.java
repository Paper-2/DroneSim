package com.paperpiper.drone.behavior;

import com.paperpiper.drone.Drone;

/**
 * High-level mission layer that runs on top of the PID autopilot.
 * Implementations call {@link Drone#setTargetPosition} (and similar) each tick
 * to produce autonomous flight patterns.
 */
public interface DroneBehavior {

    /**
     * Called once per simulation tick before the drone's physics update.
     */
    void update(Drone drone, float dt);

    /**
     * Human-readable name used in the UI / logging.
     */
    String getName();
}
