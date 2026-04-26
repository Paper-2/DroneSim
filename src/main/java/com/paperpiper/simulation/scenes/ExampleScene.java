package com.paperpiper.simulation.scenes;
import com.paperpiper.simulation.SceneConfig;


/*
Example scene demonstrating basic functionality.
In case I forget or something.
*/
public final class ExampleScene implements SceneFactory {

    public ExampleScene() {
    }

    /*
    The idea is to have an scene which is testing env for drones. you choose
    where to place them and choose their behavior.
    */
    @Override
    public SceneConfig create() {
        return buildScene();
    }

    public static SceneConfig buildScene() {
        // Implement scene configuration here
        return new SceneConfig("Example Scene", "Description of the example scene");
    }
}
