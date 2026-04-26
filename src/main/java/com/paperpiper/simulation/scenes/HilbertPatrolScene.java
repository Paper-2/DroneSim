package com.paperpiper.simulation.scenes;

import java.util.List;

import com.jme3.math.Vector3f;
import com.paperpiper.simulation.HilbertCurve;
import com.paperpiper.simulation.SceneConfig;

/**
 * 100 drones patrolling a 100×100 m Hilbert curve at 15 m altitude.
 */
public final class HilbertPatrolScene implements SceneFactory {

    public static final int DRONE_COUNT = 100;
    public static final int CURVE_ORDER = 4;     // 256 waypoints
    public static final float GRID_DIMENSION = 100f; // 100 × 100 m
    public static final float ALTITUDE = 15f;

    public HilbertPatrolScene() {
    }

    @Override
    public SceneConfig create() {
        return buildScene();
    }

    public static SceneConfig buildScene() {
        List<Vector3f> path = HilbertCurve.generate(CURVE_ORDER, GRID_DIMENSION, GRID_DIMENSION, ALTITUDE);
        int pathSize = path.size(); // 256
        int stride = Math.max(1, pathSize / DRONE_COUNT);

        SceneConfig scene = new SceneConfig(
                "Hilbert Patrol (" + DRONE_COUNT + ")",
                DRONE_COUNT + " drones patrolling a " + GRID_DIMENSION + "×" + GRID_DIMENSION + " m Hilbert curve at " + ALTITUDE + " m altitude");

        for (int i = 0; i < DRONE_COUNT; i++) {
            Vector3f spawnPos = path.get((i * stride) % pathSize);
            scene.addDroneAt(spawnPos.x, spawnPos.y, spawnPos.z);
        }

        scene.setPostLoadHook(sim -> {
            List<com.paperpiper.drone.Drone> drones = sim.getDrones();
            for (int i = 0; i < drones.size(); i++) {
                int startIndex = (i * stride) % pathSize;
                drones.get(i).setWaypointQueue(path, startIndex);
            }
        });

        return scene;
    }
}
