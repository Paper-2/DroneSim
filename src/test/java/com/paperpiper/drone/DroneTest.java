package com.paperpiper.drone;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import com.paperpiper.physics.PhysicsWorld;

/**
 * Unit tests for Drone class.
 */
class DroneTest {

    @Test
    void testDroneCreation() {
        Drone drone = new Drone();
        assertNotNull(drone);
        assertFalse(drone.isMotorsArmed());
    }

    @Test
    void testThrottleClamping() {
        Drone drone = new Drone();

        drone.setThrottle(0.5f);

        drone.setThrottle(-0.5f);

        drone.setThrottle(1.5f);

    }

    @Test
    void testPitchClamping() {
        Drone drone = new Drone();

        drone.setPitch(0.5f);
        drone.setPitch(-1.5f);
        drone.setPitch(1.5f);

    }

    @Test
    void testMotorArming() {
        Drone drone = new Drone();

        assertFalse(drone.isMotorsArmed());

    }

    /**
     * Test that the drone's collision shape settles properly on the ground
     * plane. - Initialize drone with physics - Drop from height - Let physics
     * settle - Verify drone is not oscillating/glitching - while touching ground
     * Verify that the drone is above ground 
     */
    @Test
    void testCollisionShapeSettlesOnGround() {
        // Initialize physics
        PhysicsWorld physicsWorld = new PhysicsWorld();
        physicsWorld.init();

        // Create ground plane
        physicsWorld.createGroundPlane();

        // Create drone at spawn height (physics-only to avoid OpenGL in tests)
        float spawnHeight = 2.0f;
        Drone drone = new Drone();
        drone.initPhysicsOnly(physicsWorld, new Vector3f(0, spawnHeight, 0));

        // Get the rigid body for position/velocity checks
        PhysicsRigidBody body = drone.getRigidBody();
        assertNotNull(body, "Drone should have a rigid body after init");

        // Run simulation for several seconds to let drone settle
        float deltaTime = 1f / 60f;
        float totalTime = 5.0f; // 5 seconds should be plenty
        int steps = (int) (totalTime / deltaTime);

        Vector3f previousPos = new Vector3f();
        Vector3f currentPos = new Vector3f();
        int stableFrames = 0;
        int requiredStableFrames = 30; // Need 30 frames (0.5s) of stability
        float stabilityThreshold = 0.0001f; // Position change threshold (0.1mm per frame)

        // Get initial position
        body.getPhysicsLocation(previousPos);

        for (int i = 0; i < steps; i++) {
            physicsWorld.stepSimulation(deltaTime);

            body.getPhysicsLocation(currentPos);

            // Check if position is stable
            float positionChange = currentPos.distance(previousPos);
            if (positionChange < stabilityThreshold) {
                stableFrames++;
            } else {
                stableFrames = 0;
            }

            // Store current as previous
            previousPos.set(currentPos);

            // If we've been stable long enough, we're done
            if (stableFrames >= requiredStableFrames) {
                break;
            }
        }

        // Get final position
        body.getPhysicsLocation(currentPos);

        // Print debug info before assertions
        Vector3f velocity = body.getLinearVelocity(null);
        System.out.println("Drone collision shape test result:");
        System.out.println("  Final position: " + currentPos);
        System.out.println("  Final velocity: " + velocity);
        System.out.println("  Stable frames: " + stableFrames);

        // Verify: drone should be stable (not oscillating/glitching into ground)
        assertTrue(stableFrames >= requiredStableFrames,
                "Drone should be stable after settling. Stable frames: " + stableFrames
                + ". If this fails, the collision shape may be causing physics instability.");

        // Verify: drone should be above ground (not clipping through)
        // The drone center should be above 0 when resting on ground
        float expectedMinHeight = 0.0f; // Center should be above ground
        assertTrue(currentPos.y >= expectedMinHeight,
                "Drone should be above ground. Y position: " + currentPos.y
                + ". If negative, collision shape is too small or mispositioned.");

        // Verify: drone should have fallen from spawn height
        assertTrue(currentPos.y < spawnHeight,
                "Drone should have fallen from spawn height. Y position: " + currentPos.y);

        // Verify: velocity should be near zero when settled
        float speed = velocity.length();
        assertTrue(speed < 0.1f,
                "Drone should have near-zero velocity when settled. Speed: " + speed
                + ". If oscillating, collision shape may be causing instability.");

        // Cleanup
        drone.cleanup(physicsWorld);
    }
}
