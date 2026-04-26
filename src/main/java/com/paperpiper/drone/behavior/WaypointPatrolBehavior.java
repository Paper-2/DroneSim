package com.paperpiper.drone.behavior;

import java.util.ArrayList;
import java.util.List;

import com.jme3.math.Vector3f;
import com.paperpiper.drone.Drone;

/**
 * Loops the drone through an ordered list of waypoints.
 */
public class WaypointPatrolBehavior implements DroneBehavior {

    public static final float DEFAULT_ARRIVAL_RADIUS = 3.0f;

    private final List<Vector3f> waypoints;
    private final float arrivalRadius;

    private int currentIndex;

    /**
     * @param waypoints ordered patrol route (copied, must not be empty)
     * @param arrivalRadius metres within which a waypoint is considered reached
     */
    public WaypointPatrolBehavior(List<Vector3f> waypoints, float arrivalRadius) {
        if (waypoints == null || waypoints.isEmpty()) {
            throw new IllegalArgumentException("Waypoint list must not be empty");
        }
        this.waypoints = new ArrayList<>(waypoints);
        this.arrivalRadius = arrivalRadius;
        this.currentIndex = 0;
    }

    /**
     * Convenience constructor using the default arrival radius.
     */
    public WaypointPatrolBehavior(List<Vector3f> waypoints) {
        this(waypoints, DEFAULT_ARRIVAL_RADIUS);
    }

    @Override
    public void update(Drone drone, float dt) {
        Vector3f target = waypoints.get(currentIndex);
        drone.setTargetPosition(target);

        Vector3f pos = drone.getPosition();
        if (pos.distance(target) < arrivalRadius) {
            currentIndex = (currentIndex + 1) % waypoints.size();
        }
    }

    @Override
    public String getName() {
        return "WaypointPatrol";
    }

    /**
     * Returns the waypoint the drone is currently flying toward.
     */
    public Vector3f getCurrentWaypoint() {
        return new Vector3f(waypoints.get(currentIndex));
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public int getWaypointCount() {
        return waypoints.size();
    }
}
