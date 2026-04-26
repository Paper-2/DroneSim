package com.paperpiper.simulation.scenes;

import java.util.List;

import com.jme3.math.Vector3f;
import com.paperpiper.drone.behavior.WaypointPatrolBehavior;
import com.paperpiper.simulation.SceneConfig;

public final class WaypointTourScene implements SceneFactory {

    public WaypointTourScene() {
    }

    @Override
    public SceneConfig create() {
        return buildScene();
    }

    public static SceneConfig buildScene() {
        List<Vector3f> waypoints = List.of(
                new Vector3f(0f, 10f, 20f), // North
                new Vector3f(19f, 20f, 6f), // North-East (higher)
                new Vector3f(12f, 15f, -16f), // South-East
                new Vector3f(-12f, 25f, -16f), // South-West (highest)
                new Vector3f(-19f, 12f, 6f) // North-West
        );

        SceneConfig scene = new SceneConfig(
                "Waypoint Tour",
                "Single drone patrolling a 5-point circuit at varying altitudes");

        // Spawn near the first waypoint (at ground level)
        Vector3f first = waypoints.get(0);
        scene.addDroneAt(first.x, 2f, first.z);

        scene.setPostLoadHook(sim -> {
            if (sim.getDrones().isEmpty()) {
                return;
            }
            var drone = sim.getDrones().get(0);
            drone.setMotorsArmed(true);
            drone.setBehavior(new WaypointPatrolBehavior(waypoints));
            sim.setActiveDrone(drone);
        });

        return scene;
    }
}
