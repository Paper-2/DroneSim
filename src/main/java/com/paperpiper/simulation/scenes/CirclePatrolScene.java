package com.paperpiper.simulation.scenes;

import com.jme3.math.Vector3f;
import com.paperpiper.drone.behavior.CirclePatrolBehavior;
import com.paperpiper.simulation.SceneConfig;

/**
 * Single drone flying a horizontal circle at 15 m altitude.
 */
public final class CirclePatrolScene implements SceneFactory {

    public static final float RADIUS = 20f;
    public static final float ALTITUDE = 15f;
    public static final float LAP_TIME_SEC = 30f;

    public CirclePatrolScene() {
    }

    @Override
    public SceneConfig create() {
        return buildScene();
    }

    public static SceneConfig buildScene() {
        float angularSpeed = (float) (2 * Math.PI / LAP_TIME_SEC);
        Vector3f center = new Vector3f(0f, ALTITUDE, 0f);

        // Spawn the drone at angle 0 (on the +X edge of the circle)
        SceneConfig scene = new SceneConfig(
                "Circle Patrol",
                "Single drone flying a horizontal circle (r=" + RADIUS + " m, " + LAP_TIME_SEC + " s/lap)");
        scene.addDroneAt(center.x + RADIUS, 2f, center.z);

        scene.setPostLoadHook(sim -> {
            if (sim.getDrones().isEmpty()) {
                return;
            }
            var drone = sim.getDrones().get(0);
            drone.setMotorsArmed(true);
            // Start at 0° so the drone does not need to reposition before patrolling
            drone.setBehavior(new CirclePatrolBehavior(center, RADIUS, angularSpeed, 0f));
            sim.setActiveDrone(drone);
        });

        return scene;
    }
}
