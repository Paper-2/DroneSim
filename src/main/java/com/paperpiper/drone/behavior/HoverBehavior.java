package com.paperpiper.drone.behavior;

import com.jme3.math.Vector3f;
import com.paperpiper.drone.Drone;

/**
 * Commands the drone to hold a fixed world-space position indefinitely.
 */
public class HoverBehavior implements DroneBehavior {

    private Vector3f hoverPosition;

    public HoverBehavior(Vector3f position) {
        this.hoverPosition = new Vector3f(position);
    }


    public HoverBehavior(float x, float y, float z) {
        this(new Vector3f(x, y, z));
    }

    @Override
    public void update(Drone drone, float dt) {
        drone.setTargetPosition(hoverPosition);
    }

    @Override
    public String getName() {
        return "Hover";
    }

    public Vector3f getHoverPosition() {
        return new Vector3f(hoverPosition);
    }

    public void setHoverPosition(Vector3f position) {
        this.hoverPosition = new Vector3f(position);
    }
}
