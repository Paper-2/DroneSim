package com.paperpiper.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jme3.math.Vector3f;

public class SceneManager {

    private static final Logger logger = LoggerFactory.getLogger(SceneManager.class);

    private static final int BUILTIN_COUNT = 5;

    private final List<SceneConfig> scenes = new ArrayList<>();
    private SceneConfig currentScene;
    private boolean modified = false;

    public SceneManager() {
        scenes.add(new SceneConfig("Empty", "No drones - blank slate"));
        scenes.add(new SceneConfig("Single Hover", "One drone hovering at origin")
                .addDroneAt(0, 2, 0));
        scenes.add(new SceneConfig("Swarm (3)", "Three drones in a row")
                .addDroneAt(-4, 2, 0)
                .addDroneAt(0, 2, 0)
                .addDroneAt(4, 2, 0));
        scenes.add(new SceneConfig("Diamond (4)", "Four drones in a diamond")
                .addDroneAt(0, 2, 4)
                .addDroneAt(4, 2, 0)
                .addDroneAt(0, 2, -4)
                .addDroneAt(-4, 2, 0));
        scenes.add(buildHilbertPatrolScene());
        currentScene = scenes.get(1);
    }

    public List<SceneConfig> getScenes() {
        return Collections.unmodifiableList(scenes);
    }

    public SceneConfig getCurrentScene() {
        return currentScene;
    }

    public boolean isModified() {
        return modified;
    }

    public void markModified() {
        this.modified = true;
    }

    public SceneConfig createScene(String name) {
        SceneConfig cfg = new SceneConfig(name, "");
        scenes.add(cfg);
        logger.info("Created scene '{}'", name);
        return cfg;
    }

    public boolean deleteScene(SceneConfig scene) {
        int idx = scenes.indexOf(scene);
        if (idx < 0) {
            return false;
        }
        if (idx < BUILTIN_COUNT) {
            logger.warn("Cannot delete built-in scene '{}'", scene.getName());
            return false;
        }
        scenes.remove(idx);
        if (currentScene == scene) {
            currentScene = scenes.get(0);
        }
        logger.info("Deleted scene '{}'", scene.getName());
        return true;
    }

    public SceneConfig saveCurrentAs(String name, SimulationEngine simulation) {
        SceneConfig cfg = new SceneConfig(name, "Saved from simulation");
        for (com.paperpiper.drone.Drone d : simulation.getDrones()) {
            com.jme3.math.Vector3f p = d.getPosition();
            cfg.addDroneAt(p.x, p.y, p.z);
        }
        scenes.add(cfg);
        currentScene = cfg;
        modified = false;
        logger.info("Saved scene '{}' ({} drone(s))", name, cfg.getDroneCount());
        return cfg;
    }

    public void setCurrentScene(SceneConfig scene) {
        currentScene = scene;
        modified = false;
    }

    public boolean isBuiltin(SceneConfig scene) {
        int idx = scenes.indexOf(scene);
        return idx >= 0 && idx < BUILTIN_COUNT;
    }


    private static SceneConfig buildHilbertPatrolScene() {
        final int DRONE_COUNT = 100;
        final int CURVE_ORDER = 4;           // 256 waypoints
        final float WORLD_SIZE = 100f;       // 100 × 100 m
        final float ALTITUDE = 15f;        // spawn / patrol altitude

        List<Vector3f> path = HilbertCurve.generate(CURVE_ORDER, WORLD_SIZE, WORLD_SIZE, ALTITUDE);
        int pathSize = path.size();          // 256

        SceneConfig scene = new SceneConfig(
                "Hilbert Patrol (100)",
                "100 drones patrolling a 100×100 m Hilbert curve at 15 m altitude");

        // Spawn positions: evenly spaced along the Hilbert path
        int stride = Math.max(1, pathSize / DRONE_COUNT);
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
