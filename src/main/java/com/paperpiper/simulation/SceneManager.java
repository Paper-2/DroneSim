package com.paperpiper.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.paperpiper.simulation.scenes.SceneFactory;

public class SceneManager {

    private static final Logger logger = LoggerFactory.getLogger(SceneManager.class);

    private final List<SceneConfig> scenes = new ArrayList<>();
    private final Set<SceneConfig> builtinScenes = new HashSet<>();
    private SceneConfig currentScene;
    private boolean modified = false;

    public SceneManager() {
        // Inline scenes  simple enough to not warrant their own class
        addBuiltin(new SceneConfig("Empty", "No drones - blank slate"));
        addBuiltin(new SceneConfig("Single Hover", "One drone hovering at origin")
                .addDroneAt(0, 2, 0));
        addBuiltin(new SceneConfig("Swarm (3)", "Three drones in a row")
                .addDroneAt(-4, 2, 0)
                .addDroneAt(0, 2, 0)
                .addDroneAt(4, 2, 0));
        addBuiltin(new SceneConfig("Diamond (4)", "Four drones in a diamond")
                .addDroneAt(0, 2, 4)
                .addDroneAt(4, 2, 0)
                .addDroneAt(0, 2, -4)
                .addDroneAt(-4, 2, 0));

        // Discover scenes declared in SceneFactory's sealed permits clause
        for (SceneConfig cfg : SceneFactory.buildAll()) {
            addBuiltin(cfg);
        }

        currentScene = scenes.get(1);
    }

    private void addBuiltin(SceneConfig cfg) {
        scenes.add(cfg);
        builtinScenes.add(cfg);
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
        if (!scenes.contains(scene)) {
            return false;
        }
        if (builtinScenes.contains(scene)) {
            logger.warn("Cannot delete built-in scene '{}'", scene.getName());
            return false;
        }
        scenes.remove(scene);
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
        return builtinScenes.contains(scene);
    }

}
