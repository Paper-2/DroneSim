package com.paperpiper.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import com.jme3.math.Vector3f;

public class SceneConfig {

    private String name;
    private String description;
    private final List<Vector3f> droneSpawnPositions = new ArrayList<>();
    private Consumer<SimulationEngine> postLoadHook = null;

    public SceneConfig(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public SceneConfig addDroneAt(float x, float y, float z) {
        droneSpawnPositions.add(new Vector3f(x, y, z));
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Vector3f> getDroneSpawnPositions() {
        return Collections.unmodifiableList(droneSpawnPositions);
    }

    public int getDroneCount() {
        return droneSpawnPositions.size();
    }


    public void setPostLoadHook(Consumer<SimulationEngine> hook) {
        this.postLoadHook = hook;
    }

    public Consumer<SimulationEngine> getPostLoadHook() {
        return postLoadHook;
    }
}
