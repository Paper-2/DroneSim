package com.paperpiper.drone.behavior;

import java.util.List;

import com.jme3.math.Vector3f;
import com.paperpiper.drone.Drone;

/**
 * Reynolds boids flocking behavior.
 *
 * Each tick the position of each boid is updated by the physics engine, then
 * the behavior is applied to determine the new target position
 */
public class BoidSwarmBehavior implements DroneBehavior {

    // Tunable weights for each rule (defaults give pleasant flocking).
    public static final float DEFAULT_NEIGHBOR_RADIUS = 12f;
    public static final float DEFAULT_SEPARATION_RADIUS = 4f;
    public static final float DEFAULT_SEPARATION_WEIGHT = 2.0f;
    public static final float DEFAULT_ALIGNMENT_WEIGHT = 1.0f;
    public static final float DEFAULT_COHESION_WEIGHT = 0.8f;
    public static final float DEFAULT_BOUNDS_WEIGHT = 1.5f;
    public static final float DEFAULT_LOOK_AHEAD = 6f;   // metres — how far ahead to project the target

    private final List<Drone> swarm;
    private final Vector3f boundsCenter;
    private final float boundsRadius; // soft sphere boundary (metres)

    private final float neighborRadius;
    private final float separationRadius;
    private final float separationWeight;
    private final float alignmentWeight;
    private final float cohesionWeight;
    private final float boundsWeight;
    private final float lookAhead;

    public BoidSwarmBehavior(List<Drone> swarm, Vector3f boundsCenter, float boundsRadius) {
        this(swarm, boundsCenter, boundsRadius,
                DEFAULT_NEIGHBOR_RADIUS, DEFAULT_SEPARATION_RADIUS,
                DEFAULT_SEPARATION_WEIGHT, DEFAULT_ALIGNMENT_WEIGHT,
                DEFAULT_COHESION_WEIGHT, DEFAULT_BOUNDS_WEIGHT,
                DEFAULT_LOOK_AHEAD);
    }

    public BoidSwarmBehavior(List<Drone> swarm, Vector3f boundsCenter, float boundsRadius,
            float neighborRadius, float separationRadius,
            float separationWeight, float alignmentWeight,
            float cohesionWeight, float boundsWeight,
            float lookAhead) {
        this.swarm = swarm;
        this.boundsCenter = new Vector3f(boundsCenter);
        this.boundsRadius = boundsRadius;
        this.neighborRadius = neighborRadius;
        this.separationRadius = separationRadius;
        this.separationWeight = separationWeight;
        this.alignmentWeight = alignmentWeight;
        this.cohesionWeight = cohesionWeight;
        this.boundsWeight = boundsWeight;
        this.lookAhead = lookAhead;
    }

    @Override
    public void update(Drone self, float dt) {
        Vector3f myPos = self.getPosition();
        Vector3f myVel = self.getVelocity();

        Vector3f separation = new Vector3f();
        Vector3f alignment = new Vector3f();
        Vector3f cohesion = new Vector3f();

        int neighborCount = 0;
        int separationCount = 0;

        for (Drone other : swarm) {
            if (other == self) {
                continue;
            }
            Vector3f otherPos = other.getPosition();
            Vector3f offset = otherPos.subtract(myPos);
            float dist = offset.length();
            if (dist <= 0.0001f || dist > neighborRadius) {
                continue;
            }

            // Cohesion accumulates neighbor positions
            cohesion.addLocal(otherPos);
            // Alignment accumulates neighbor velocities
            alignment.addLocal(other.getVelocity());
            neighborCount++;

            // Separation: weighted away-vector from too-close neighbors
            if (dist < separationRadius) {
                // -offset / dist² → away from neighbor, weighted stronger when closer
                Vector3f away = offset.mult(-1f / (dist * dist));
                separation.addLocal(away);
                separationCount++;
            }
        }

        Vector3f steering = new Vector3f();

        if (neighborCount > 0) {
            cohesion.divideLocal(neighborCount).subtractLocal(myPos);
            alignment.divideLocal(neighborCount);
            steering.addLocal(cohesion.multLocal(cohesionWeight));
            steering.addLocal(alignment.multLocal(alignmentWeight));
        }
        if (separationCount > 0) {
            steering.addLocal(separation.multLocal(separationWeight));
        }

        // Soft bounds — pull back toward centre when outside the sphere
        Vector3f fromCenter = myPos.subtract(boundsCenter);
        float distFromCenter = fromCenter.length();
        if (distFromCenter > boundsRadius) {
            float overshoot = distFromCenter - boundsRadius;
            Vector3f pullBack = fromCenter.normalize().mult(-overshoot * boundsWeight);
            steering.addLocal(pullBack);
        }

        // Project the target ahead of the drone in the steering direction so the
        // autopilot has somewhere to chase.
        Vector3f heading = steering.length() > 0.01f ? steering.normalize()
                : (myVel.length() > 0.01f ? myVel.normalize() : new Vector3f(1, 0, 0));
        Vector3f target = myPos.add(heading.mult(lookAhead));

        self.setTargetPosition(target);
    }

    @Override
    public String getName() {
        return "Boid Swarm";
    }
}
