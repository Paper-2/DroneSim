package com.paperpiper.simulation.scenes;

import java.util.ArrayList;
import java.util.List;

import com.paperpiper.simulation.SceneConfig;

/**
 * In order to avoid having to call each scene's static buildScene() method
 * manually and risk forgetting to add it, I learnt about sealed interfaces and
 * used that to create a single SceneFactory interface that all scenes
 * implement.
 */
public sealed interface SceneFactory
        permits HilbertPatrolScene, CirclePatrolScene, FigureEightScene, WaypointTourScene,
         ExampleScene, BoidSwarmScene {

    SceneConfig create();

    /**
     * Instantiates every permitted implementation and returns the SceneConfig
     * each one builds. All implementations must have a public no-arg
     * constructor.
     */
    static List<SceneConfig> buildAll() {
        List<SceneConfig> out = new ArrayList<>();
        for (Class<?> sub : SceneFactory.class.getPermittedSubclasses()) {
            try {
                SceneFactory factory = (SceneFactory) sub.getDeclaredConstructor().newInstance();
                out.add(factory.create());
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(
                        "Failed to instantiate scene factory " + sub.getName(), e);
            }
        }
        return out;
    }
}
