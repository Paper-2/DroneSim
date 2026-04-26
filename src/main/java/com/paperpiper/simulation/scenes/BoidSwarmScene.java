package com.paperpiper.simulation.scenes;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.jme3.math.Vector3f;
import com.paperpiper.drone.Drone;
import com.paperpiper.drone.behavior.BoidSwarmBehavior;
import com.paperpiper.simulation.SceneConfig;

/**
 * 30 drones flocking with classic Reynolds boids rules inside a soft 60 m
 * spherical boundary centred 25 m above the origin.
 */
public final class BoidSwarmScene implements SceneFactory {

    public static final int DRONE_COUNT = 30;
    public static final float SPAWN_VOLUME = 30f;     // half-extent of cube the drones spawn in
    public static final float ALTITUDE = 25f;
    public static final float BOUNDS_RADIUS = 60f;    // soft sphere — boids steer back if they drift past this

    public BoidSwarmScene() {
    }

    @Override
    public SceneConfig create() {
        return buildScene();
    }

    public static SceneConfig buildScene() {
        SceneConfig scene = new SceneConfig(
                "Boid Swarm (" + DRONE_COUNT + ")",
                DRONE_COUNT + " drones flocking with separation/alignment/cohesion rules");

        // Spawn drones in a randomized cube around the bounds centre.
        // A fixed seed keeps the scene reproducible
        Random rng = new Random(42L);
        for (int i = 0; i < DRONE_COUNT; i++) {
            float x = (rng.nextFloat() * 2f - 1f) * SPAWN_VOLUME;
            float z = (rng.nextFloat() * 2f - 1f) * SPAWN_VOLUME;
            float y = ALTITUDE + (rng.nextFloat() * 2f - 1f) * (SPAWN_VOLUME * 0.3f);
            scene.addDroneAt(x, Math.max(2f, y), z);
        }

        scene.setPostLoadHook(sim -> {
            List<Drone> drones = sim.getDrones();
            // copies the list so the behavior can iterate over it without
            // concurrent modification issues def not the most efficient but
            // it's just a demo
            List<Drone> swarm = new ArrayList<>(drones);
            Vector3f boundsCenter = new Vector3f(0f, ALTITUDE, 0f);

            for (Drone drone : drones) {
                drone.setMotorsArmed(true);
                drone.setBehavior(new BoidSwarmBehavior(swarm, boundsCenter, BOUNDS_RADIUS));
            }
            if (!drones.isEmpty()) {
                sim.setActiveDrone(drones.get(0));
            }
        });

        return scene;
    }
}
