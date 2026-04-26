package com.paperpiper.simulation.scenes;

import com.jme3.math.Vector3f;
import com.paperpiper.drone.behavior.FigureEightBehavior;
import com.paperpiper.simulation.SceneConfig;

/**
 * Single drone flying a figure-eight (Lissajous 1:2) pattern at 18 m altitude.
 */
public final class FigureEightScene implements SceneFactory {

    public static final float SCALE = 25f;
    public static final float ALTITUDE = 18f;
    public static final float LAP_TIME_SEC = 60f;

    public FigureEightScene() {
    }

    @Override
    public SceneConfig create() {
        return buildScene();
    }

    public static SceneConfig buildScene() {
        float speedRad = (float) (2 * Math.PI / LAP_TIME_SEC);
        Vector3f center = new Vector3f(0f, ALTITUDE, 0f);

        // t=0 → (scale, 0) so spawn the drone there
        SceneConfig scene = new SceneConfig(
                "Figure Eight",
                "Single drone tracing a figure-eight pattern ("
                + (int) SCALE + " m half-width, " + (int) LAP_TIME_SEC + " s/lap)");
        scene.addDroneAt(center.x + SCALE, 2f, center.z);

        scene.setPostLoadHook(sim -> {
            if (sim.getDrones().isEmpty()) {
                return;
            }
            var drone = sim.getDrones().get(0);
            drone.setMotorsArmed(true);
            drone.setBehavior(new FigureEightBehavior(center, SCALE, speedRad));
            sim.setActiveDrone(drone);
        });

        return scene;
    }
}
