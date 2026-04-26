package com.paperpiper.drone.behavior;

import com.jme3.math.Vector3f;
import com.paperpiper.drone.Drone;

/**
 * Flies the drone in a horizontal circle.
 *
 * <p>
 * Each tick, {@code currentAngle} advances by {@code angularSpeedRad * dt} and
 * the target is set to the corresponding point on the circle. The autopilot
 * will naturally lag slightly, producing smooth banked turns.
 */
public class CirclePatrolBehavior implements DroneBehavior {

    private final Vector3f center;
    private final float radius;
    private final float angularSpeedRad; // radians per second, positive = CCW

    private float currentAngle; // radians

    public CirclePatrolBehavior(Vector3f center, float radius,
            float angularSpeedRad, float initialAngleDeg) {
        this.center = new Vector3f(center);
        this.radius = radius;
        this.angularSpeedRad = angularSpeedRad;
        this.currentAngle = (float) Math.toRadians(initialAngleDeg);
    }

    /**
     * Convenience constructor starts at 0 degrees.
     */
    public CirclePatrolBehavior(Vector3f center, float radius, float angularSpeedRad) {
        this(center, radius, angularSpeedRad, 0f);
    }

    @Override
    public void update(Drone drone, float dt) {
        currentAngle += angularSpeedRad * dt;

        float tx = center.x + (float) Math.cos(currentAngle) * radius;
        float ty = center.y;
        float tz = center.z + (float) Math.sin(currentAngle) * radius;

        drone.setTargetPosition(new Vector3f(tx, ty, tz));
    }

    @Override
    public String getName() {
        return "CirclePatrol";
    }

    public float getCurrentAngleDeg() {
        return (float) Math.toDegrees(currentAngle);
    }

    public Vector3f getCenter() {
        return new Vector3f(center);
    }

    public float getRadius() {
        return radius;
    }
}
