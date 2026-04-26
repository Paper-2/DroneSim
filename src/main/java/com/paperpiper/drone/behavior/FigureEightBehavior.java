package com.paperpiper.drone.behavior;

import com.jme3.math.Vector3f;
import com.paperpiper.drone.Drone;

// Draws an 8-shaped path in the XZ plane around a given centre point.
public class FigureEightBehavior implements DroneBehavior {

    private final Vector3f center;
    private final float scale;       
    private final float speedRad; 

    private float t; // current parameter (radians)


    public FigureEightBehavior(Vector3f center, float scale, float speedRad) {
        this.center = new Vector3f(center);
        this.scale = scale;
        this.speedRad = speedRad;
        this.t = 0f;
    }

    @Override
    public void update(Drone drone, float dt) {
        t += speedRad * dt;

        float tx = center.x + scale * (float) Math.cos(t);
        float ty = center.y;
        float tz = center.z + scale * (float) Math.sin(2.0 * t);

        drone.setTargetPosition(new Vector3f(tx, ty, tz));
    }

    @Override
    public String getName() {
        return "FigureEight";
    }

    public Vector3f getCenter() {
        return new Vector3f(center);
    }

    public float getScale() {
        return scale;
    }
}
