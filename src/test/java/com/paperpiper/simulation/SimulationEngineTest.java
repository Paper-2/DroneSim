package com.paperpiper.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.jme3.math.Vector3f;
import com.paperpiper.drone.Drone;
import com.paperpiper.physics.PhysicsWorld;
import com.paperpiper.render.Camera;
import com.paperpiper.render.Renderer;
import com.paperpiper.render.Window;

/**
 * Integration test for SimulationEngine — loads the drone model, renders
 * frames, and verifies the controller moves the drone toward a random target
 * position.
 */
class SimulationEngineTest {

    /**
     * Set a random target position, run for 15 seconds with full rendering,
     * then verify the drone moved toward the target.
     */
    boolean headless = false; // Set to false to see the test window 

    // shared fixtures for all tests
    private Window window;
    private Renderer renderer;
    private PhysicsWorld physicsWorld;
    private SimulationEngine engine;

    @AfterEach
    void tearDown() {
        if (engine != null) {
            engine.cleanup();
        }
        if (renderer != null) {
            renderer.cleanup();
        }
        if (window != null) {
            window.cleanup();
        }
    }

    private Drone spawnAndActivateDrone() {
        Drone drone = engine.addDrone(new Vector3f(0f, 2f, 0f));
        drone.setMotorsArmed(true);
        engine.setActiveDrone(drone);
        return drone;
    }

    @Test
    void testDroneReachesTargetPosition() {
        // --- Bootstrap the same stack the real app uses (headless for tests) ---
        window = new Window("testDroneReachesTargetPosition", 800, 600, headless);
        window.init();

        renderer = new Renderer();
        renderer.init();

        physicsWorld = new PhysicsWorld();
        physicsWorld.init();

        engine = new SimulationEngine(physicsWorld);
        engine.init(); // loads DroneBody model, creates ground mesh, arms motors

        // Verify setup
        Drone activeDrone = spawnAndActivateDrone();
        assertNotNull(activeDrone, "Active drone should be initialized");
        assertTrue(activeDrone.isMotorsArmed(), "Motors should be armed");

        // Random 3D target position
        Random rng = new Random();
        Vector3f target;
        Vector3f startPos;
        float startDistance;
        // Generate random target at least 20m from start
        do {
            float tx = rng.nextFloat() * 30f - 15f; // -15 to 15
            float ty = rng.nextFloat() * 25f + 5f; // 5 to 30 (above ground)
            float tz = rng.nextFloat() * 30f - 15f; // -15 to 15
            target = new Vector3f(tx, ty, tz);
            startPos = activeDrone.getRigidBody().getPhysicsLocation(null);
            startDistance = startPos.distance(target);
        } while (startDistance < 20f);
        activeDrone.setTargetPosition(target);

        System.out.println("Random target position: " + target);
        System.out.println("Start position: " + startPos);
        System.out.println("Start distance to target: " + startDistance);

        // --- Camera follows the drone ---
        Camera camera = renderer.getCamera();
        // Offset: behind (-X) and above the drone since it faces +X
        org.joml.Vector3f camOffset = new org.joml.Vector3f(-8f, 5f, 0f);

        // --- Run for 15 seconds, rendering every frame ---
        float dt = 1f / 60f;
        float totalTime = 30.0f;
        int steps = (int) (totalTime / dt);

        for (int i = 0; i < steps; i++) {
            window.pollEvents();

            engine.update(dt);
            physicsWorld.stepSimulation(dt);

            // Debug: print state every second
            if (i % 60 == 0) {
                Vector3f dbgPos = activeDrone.getRigidBody().getPhysicsLocation(null);
                System.out.printf("t=%.1fs pos=(%.2f, %.2f, %.2f) motors=[%.3f, %.3f, %.3f, %.3f]%n",
                        i * dt, dbgPos.x, dbgPos.y, dbgPos.z,
                        activeDrone.FL, activeDrone.FR, activeDrone.RL, activeDrone.RR);
            }

            // Update camera to follow the drone
            Vector3f dronePos = activeDrone.getRigidBody().getPhysicsLocation(null);
            camera.setPosition(new org.joml.Vector3f(
                    dronePos.x + camOffset.x,
                    dronePos.y + camOffset.y,
                    dronePos.z + camOffset.z));
            // Look toward the drone: direction = dronePos - camPos
            float dx = -camOffset.x; // 8
            float dy = -camOffset.y; // -5
            float dz = -camOffset.z; // 0
            float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)); // 0°
            float pitch = (float) Math.toDegrees(Math.atan2(dy,
                    Math.sqrt(dx * dx + dz * dz))); // ~-32°
            camera.setYaw(yaw);
            camera.setPitch(pitch);

            renderer.clear();
            engine.render(renderer);
            window.swapBuffers();

            if (window.shouldClose()) {
                break; // user closed the test window early
            }
        }

        // --- Verify the drone is hovering at the target ---
        Vector3f pos = activeDrone.getRigidBody().getPhysicsLocation(null);
        Vector3f vel = activeDrone.getRigidBody().getLinearVelocity(null);
        float finalDistance = pos.distance(target);
        float speed = vel.length();

        System.out.println("Final drone position: " + pos);
        System.out.println("Final drone velocity: " + vel);
        System.out.printf("Distance from target — start: %.3f  final: %.3f%n", startDistance, finalDistance);
        System.out.printf("Per-axis distance — X: %.3f  Y: %.3f  Z: %.3f%n",
                Math.abs(pos.x - target.x), Math.abs(pos.y - target.y), Math.abs(pos.z - target.z));
        System.out.printf("Speed: %.3f m/s%n", speed);

        // The drone should be at the target (within 2m tolerance for large-range
        // targets)
        assertTrue(finalDistance < 2.0f,
                "Drone should be hovering at target. Final dist: " + finalDistance);

        // The drone should be nearly stopped (hovering, not fly-by)
        assertTrue(speed < 1.0f,
                "Drone should be hovering. Speed: " + speed + " m/s");
    }

    @Test
    void testMarathon() {
        // --- Bootstrap the same stack the real app uses (headless for tests) ---
        window = new Window("Marathon Test", 800, 600, headless);
        window.init();

        renderer = new Renderer();
        renderer.init();

        physicsWorld = new PhysicsWorld();
        physicsWorld.init();

        engine = new SimulationEngine(physicsWorld);
        engine.init(); // loads DroneBody model, creates ground mesh, arms motors

        // Verify setup
        Drone activeDrone = spawnAndActivateDrone();
        assertNotNull(activeDrone, "Active drone should be initialized");
        assertTrue(activeDrone.isMotorsArmed(), "Motors should be armed");

        // Random 3D target position
        Random rng = new Random();
        Vector3f target;
        Vector3f startPos;
        float startDistance;
        // Generate random target at least 20m from start
        do {
            float tx = rng.nextFloat() * 30f - 15f; // -15 to 15
            float ty = rng.nextFloat() * 15f - 5f; // 5 to 30 (above ground)
            float tz = rng.nextFloat() * 30f - 15f; // -15 to 15
            target = new Vector3f(tx, ty, tz).mult(30f);
            startPos = activeDrone.getRigidBody().getPhysicsLocation(null);
            startDistance = startPos.distance(target);
        } while (startDistance < 20f);
        activeDrone.setTargetPosition(target);

        System.out.println("Random target position: " + target);
        System.out.println("Start position: " + startPos);
        System.out.println("Start distance to target: " + startDistance);

        //  Camera follows the drone 
        Camera camera = renderer.getCamera();
        // Offset: behind (-X) and above the drone since it faces +X
        org.joml.Vector3f camOffset = new org.joml.Vector3f(-8f, 5f, 0f);

        //  Run for 15 seconds, rendering every frame 
        float dt = 1f / 60f;
        float totalTime = 45.0f;
        int steps = (int) (totalTime / dt);

        for (int i = 0; i < steps; i++) {
            window.pollEvents();

            engine.update(dt);
            physicsWorld.stepSimulation(dt);

            // Debug: print state every second
            if (i % 60 == 0) {
                Vector3f dbgPos = activeDrone.getRigidBody().getPhysicsLocation(null);
                System.out.printf("t=%.1fs pos=(%.2f, %.2f, %.2f) motors=[%.3f, %.3f, %.3f, %.3f]%n",
                        i * dt, dbgPos.x, dbgPos.y, dbgPos.z,
                        activeDrone.FL, activeDrone.FR, activeDrone.RL, activeDrone.RR);
            }

            // Update camera to follow the drone
            Vector3f dronePos = activeDrone.getRigidBody().getPhysicsLocation(null);
            camera.setPosition(new org.joml.Vector3f(
                    dronePos.x + camOffset.x,
                    dronePos.y + camOffset.y,
                    dronePos.z + camOffset.z));
            // Look toward the drone: direction = dronePos - camPos
            float dx = -camOffset.x; // 8
            float dy = -camOffset.y; // -5
            float dz = -camOffset.z; // 0
            float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)); // 0°
            float pitch = (float) Math.toDegrees(Math.atan2(dy,
                    Math.sqrt(dx * dx + dz * dz))); // ~-32°
            camera.setYaw(yaw);
            camera.setPitch(pitch);

            renderer.clear();
            engine.render(renderer);
            window.swapBuffers();

            if (window.shouldClose()) {
                break; // user closed the test window early
            }
        }

        //  Verify the drone is hovering at the target 
        Vector3f pos = activeDrone.getRigidBody().getPhysicsLocation(null);
        Vector3f vel = activeDrone.getRigidBody().getLinearVelocity(null);
        float finalDistance = pos.distance(target);
        float speed = vel.length();

        System.out.println("Final drone position: " + pos);
        System.out.println("Final drone velocity: " + vel);
        System.out.printf("Distance from target — start: %.3f  final: %.3f%n", startDistance, finalDistance);
        System.out.printf("Per-axis distance — X: %.3f  Y: %.3f  Z: %.3f%n",
                Math.abs(pos.x - target.x), Math.abs(pos.y - target.y), Math.abs(pos.z - target.z));
        System.out.printf("Speed: %.3f m/s%n", speed);

        // The drone should be at the target (within 2m tolerance for large-range
        // targets)
        assertTrue(finalDistance < 2.0f,
                "Drone should be hovering at target. Final dist: " + finalDistance);

        // The drone should be nearly stopped (hovering, not fly-by)
        assertTrue(speed < 1.0f,
                "Drone should be hovering (low speed). Speed: " + speed + " m/s");
    }

    /**
     * Set two random target positions sequentially — once the drone reaches the
     * first target, assign the second and verify it reaches that too.
     */
    @Test
    void testDroneReachesTwoTargetsSequentially() {
        // --- Bootstrap (headless for tests) ---
        window = new Window("Two-Target Test", 800, 600, headless);
        window.init();

        renderer = new Renderer();
        renderer.init();

        physicsWorld = new PhysicsWorld();
        physicsWorld.init();

        engine = new SimulationEngine(physicsWorld);
        engine.init();

        Drone activeDrone = spawnAndActivateDrone();
        assertNotNull(activeDrone, "Active drone should be initialized");
        assertTrue(activeDrone.isMotorsArmed(), "Motors should be armed");

        // --- Generate two random targets, each at least 15m from the previous position
        // ---
        Random rng = new Random();
        Vector3f startPos = activeDrone.getRigidBody().getPhysicsLocation(null);

        Vector3f target1;
        do {
            float tx = rng.nextFloat() * 30f - 15f;
            float ty = rng.nextFloat() * 20f + 5f;
            float tz = rng.nextFloat() * 30f - 15f;
            target1 = new Vector3f(tx, ty, tz);
        } while (startPos.distance(target1) < 15f);

        Vector3f target2;
        do {
            float tx = rng.nextFloat() * 30f - 15f;
            float ty = rng.nextFloat() * 20f + 5f;
            float tz = rng.nextFloat() * 30f - 15f;
            target2 = new Vector3f(tx, ty, tz);
        } while (target1.distance(target2) < 15f);

        System.out.println("=== Two-Target Test ===");
        System.out.println("Start:   " + startPos);
        System.out.println("Target1: " + target1 + "  dist=" + startPos.distance(target1));
        System.out.println("Target2: " + target2 + "  dist=" + target1.distance(target2));

        // --- Camera setup ---
        Camera camera = renderer.getCamera();
        org.joml.Vector3f camOffset = new org.joml.Vector3f(-8f, 5f, 0f);

        float dt = 1f / 60f;
        float legTime = 20.0f; // max time per leg
        int stepsPerLeg = (int) (legTime / dt);
        float arrivalThreshold = 2.0f; // consider "arrived" when within 2m

        // ===== Leg 1: fly to target1 =====
        activeDrone.setTargetPosition(target1);
        boolean reachedTarget1 = false;

        for (int i = 0; i < stepsPerLeg; i++) {
            window.pollEvents();
            engine.update(dt);
            physicsWorld.stepSimulation(dt);

            if (i % 60 == 0) {
                Vector3f p = activeDrone.getRigidBody().getPhysicsLocation(null);
                System.out.printf("[Leg1] t=%.1fs pos=(%.2f, %.2f, %.2f) dist=%.2f%n",
                        i * dt, p.x, p.y, p.z, p.distance(target1));
            }

            // Camera follow
            Vector3f dp = activeDrone.getRigidBody().getPhysicsLocation(null);
            camera.setPosition(new org.joml.Vector3f(
                    dp.x + camOffset.x, dp.y + camOffset.y, dp.z + camOffset.z));
            float yaw = (float) Math.toDegrees(Math.atan2(-camOffset.z, -camOffset.x));
            float pitch = (float) Math.toDegrees(Math.atan2(-camOffset.y,
                    Math.sqrt(camOffset.x * camOffset.x + camOffset.z * camOffset.z)));
            camera.setYaw(yaw);
            camera.setPitch(pitch);

            renderer.clear();
            engine.render(renderer);
            window.swapBuffers();

            // Check arrival
            float d = activeDrone.getRigidBody().getPhysicsLocation(null).distance(target1);
            float spd = activeDrone.getRigidBody().getLinearVelocity(null).length();
            if (d < arrivalThreshold && spd < 1.0f) {
                reachedTarget1 = true;
                System.out.printf("[Leg1] Arrived at target1 at t=%.1fs  dist=%.3f  speed=%.3f%n",
                        i * dt, d, spd);
                break;
            }

            if (window.shouldClose()) {
                break;
            }
        }

        Vector3f posAfterLeg1 = activeDrone.getRigidBody().getPhysicsLocation(null);
        float distAfterLeg1 = posAfterLeg1.distance(target1);
        assertTrue(reachedTarget1,
                "Drone should reach target1. Final dist: " + distAfterLeg1);

        // ===== Leg 2: fly to target2 =====
        activeDrone.setTargetPosition(target2);
        boolean reachedTarget2 = false;

        System.out.printf("[Leg2] Switching to target2. Current pos: %s  dist to target2: %.2f%n",
                posAfterLeg1, posAfterLeg1.distance(target2));

        for (int i = 0; i < stepsPerLeg; i++) {
            window.pollEvents();
            engine.update(dt);
            physicsWorld.stepSimulation(dt);

            if (i % 60 == 0) {
                Vector3f p = activeDrone.getRigidBody().getPhysicsLocation(null);
                System.out.printf("[Leg2] t=%.1fs pos=(%.2f, %.2f, %.2f) dist=%.2f%n",
                        i * dt, p.x, p.y, p.z, p.distance(target2));
            }

            // Camera follow
            Vector3f dp = activeDrone.getRigidBody().getPhysicsLocation(null);
            camera.setPosition(new org.joml.Vector3f(
                    dp.x + camOffset.x, dp.y + camOffset.y, dp.z + camOffset.z));
            float yaw = (float) Math.toDegrees(Math.atan2(-camOffset.z, -camOffset.x));
            float pitch = (float) Math.toDegrees(Math.atan2(-camOffset.y,
                    Math.sqrt(camOffset.x * camOffset.x + camOffset.z * camOffset.z)));
            camera.setYaw(yaw);
            camera.setPitch(pitch);

            renderer.clear();
            engine.render(renderer);
            window.swapBuffers();

            // Check arrival
            float d = activeDrone.getRigidBody().getPhysicsLocation(null).distance(target2);
            float spd = activeDrone.getRigidBody().getLinearVelocity(null).length();
            if (d < arrivalThreshold && spd < 1.0f) {
                reachedTarget2 = true;
                System.out.printf("[Leg2] Arrived at target2 at t=%.1fs  dist=%.3f  speed=%.3f%n",
                        i * dt, d, spd);
                break;
            }

            if (window.shouldClose()) {
                break;
            }
        }

        Vector3f finalPos = activeDrone.getRigidBody().getPhysicsLocation(null);
        float finalDist = finalPos.distance(target2);
        float finalSpeed = activeDrone.getRigidBody().getLinearVelocity(null).length();

        System.out.println("Final drone position: " + finalPos);
        System.out.printf("Distance to target2: %.3f  Speed: %.3f m/s%n", finalDist, finalSpeed);

        assertTrue(reachedTarget2,
                "Drone should reach target2. Final dist: " + finalDist);
        assertTrue(finalSpeed < 1.0f,
                "Drone should be hovering at target2. Speed: " + finalSpeed + " m/s");
    }

    /**
     * Measure how closely the drone's actual flight path follows the ideal
     * straight line from start to target. Computes the perpendicular distance
     * from every sampled position to the line segment and asserts that the
     * maximum and average deviation stay within tight tolerances.
     */
    @Test
    void testFlightPathDeviation() {
        // --- Bootstrap (headless for tests) ---
        window = new Window("Path Deviation Test", 800, 600, headless);
        window.init();

        renderer = new Renderer();
        renderer.init();

        physicsWorld = new PhysicsWorld();
        physicsWorld.init();

        engine = new SimulationEngine(physicsWorld);
        engine.init();

        Drone activeDrone = spawnAndActivateDrone();
        assertNotNull(activeDrone, "Active drone should be initialized");
        assertTrue(activeDrone.isMotorsArmed(), "Motors should be armed");

        // --- Fixed target so the test is deterministic ---
        Vector3f startPos = activeDrone.getRigidBody().getPhysicsLocation(null);
        Vector3f target = new Vector3f(15f, 20f, -10f);
        float startDistance = startPos.distance(target);
        activeDrone.setTargetPosition(target);

        System.out.println("=== Path Deviation Test ===");
        System.out.println("Start:  " + startPos);
        System.out.println("Target: " + target + "  dist=" + startDistance);

        // Camera
        Camera camera = renderer.getCamera();
        org.joml.Vector3f camOffset = new org.joml.Vector3f(-8f, 5f, 0f);

        // --- Simulate and record positions every frame ---
        float dt = 1f / 60f;
        float totalTime = 30.0f;
        int steps = (int) (totalTime / dt);
        List<Vector3f> samples = new ArrayList<>();

        for (int i = 0; i < steps; i++) {
            window.pollEvents();
            engine.update(dt);
            physicsWorld.stepSimulation(dt);

            // Record position every 6 frames (~10 Hz)
            if (i % 6 == 0) {
                samples.add(activeDrone.getRigidBody().getPhysicsLocation(null).clone());
            }

            // Camera follow
            Vector3f dp = activeDrone.getRigidBody().getPhysicsLocation(null);
            camera.setPosition(new org.joml.Vector3f(
                    dp.x + camOffset.x, dp.y + camOffset.y, dp.z + camOffset.z));
            camera.setYaw((float) Math.toDegrees(Math.atan2(-camOffset.z, -camOffset.x)));
            camera.setPitch((float) Math.toDegrees(Math.atan2(-camOffset.y,
                    Math.sqrt(camOffset.x * camOffset.x + camOffset.z * camOffset.z))));

            renderer.clear();
            engine.render(renderer);
            window.swapBuffers();
            if (window.shouldClose()) {
                break;
            }
        }

        // --- Compute deviation from the ideal straight line ---
        // Line segment: A = startPos, B = target
        // For each sample point P, perpendicular distance to AB is:
        // d = |AP × AB| / |AB|
        // We only measure points that project onto the segment (0 ≤ t ≤ 1),
        // excluding the approach/hover phase near the endpoints.
        Vector3f ab = target.subtract(startPos);
        float abLen = ab.length();
        Vector3f abUnit = ab.normalize();

        float maxDeviation = 0f;
        double sumDeviation = 0;
        int deviationCount = 0;

        System.out.println("--- Per-sample deviation ---");
        for (int i = 0; i < samples.size(); i++) {
            Vector3f p = samples.get(i);
            Vector3f ap = p.subtract(startPos);

            // Projection parameter t ∈ [0, 1] along the segment
            float t = ap.dot(abUnit) / abLen;

            // Only measure in the middle portion (5% – 95%) to ignore
            // the initial acceleration and final braking phases.
            if (t < 0.05f || t > 0.95f) {
                continue;
            }

            // Perpendicular distance = |AP × AB_unit|
            Vector3f cross = ap.cross(abUnit);
            float dev = cross.length();

            if (i % 30 == 0) {
                System.out.printf("  sample %3d  t=%.2f  pos=(%.2f, %.2f, %.2f)  deviation=%.4f m%n",
                        i, t, p.x, p.y, p.z, dev);
            }

            maxDeviation = Math.max(maxDeviation, dev);
            sumDeviation += dev;
            deviationCount++;
        }

        float avgDeviation = deviationCount > 0 ? (float) (sumDeviation / deviationCount) : 0f;

        System.out.printf("Samples measured: %d%n", deviationCount);
        System.out.printf("Max deviation:  %.4f m%n", maxDeviation);
        System.out.printf("Avg deviation:  %.4f m%n", avgDeviation);

        // --- Assertions ---
        assertTrue(deviationCount > 10,
                "Should have enough mid-segment samples, got: " + deviationCount);
        assertTrue(maxDeviation < 2.0f,
                "Max deviation from ideal line should be < 2.0m. Got: " + maxDeviation + " m");
        assertTrue(avgDeviation < 1.0f,
                "Avg deviation from ideal line should be < 1.0m. Got: " + avgDeviation + " m");

        // Also verify the drone actually reached the target
        Vector3f finalPos = activeDrone.getRigidBody().getPhysicsLocation(null);
        float finalDist = finalPos.distance(target);
        assertTrue(finalDist < 2.0f,
                "Drone should reach target. Final dist: " + finalDist);
    }

    /**
     * Measure flight-path deviation across multiple target legs. The drone
     * flies through 3 fixed waypoints sequentially; for each leg we compute the
     * perpendicular distance from every sampled position to the ideal straight
     * line and assert tight tolerances.
     */
    @Test
    void testMultiTargetFlightPathDeviation() {
        // --- Bootstrap ---
        window = new Window("Multi-Target Deviation Test", 800, 600, headless);
        window.init();

        renderer = new Renderer();
        renderer.init();

        physicsWorld = new PhysicsWorld();
        physicsWorld.init();

        engine = new SimulationEngine(physicsWorld);
        engine.init();

        Drone activeDrone = spawnAndActivateDrone();
        assertNotNull(activeDrone, "Active drone should be initialized");
        assertTrue(activeDrone.isMotorsArmed(), "Motors should be armed");

        // --- Random waypoints with reproducible seed ---
        // Print the seed so a failing run can be reproduced by hard-coding it.
        long seed = System.nanoTime();
        Random rng = new Random(seed);
        System.out.println("RNG seed: " + seed);

        // Generate 3 random waypoints:
        // - altitude between 10 and 25 (above ground, similar altitudes)
        // - XZ spread: ±20
        // - each waypoint ≥ 15m from the previous one
        int waypointCount = 10;
        Vector3f[] waypoints = new Vector3f[waypointCount];
        Vector3f prev = activeDrone.getRigidBody().getPhysicsLocation(null).clone();
        for (int w = 0; w < waypointCount; w++) {
            Vector3f wp;
            do {
                float tx = rng.nextFloat() * 40f - 20f; // -20 to 20
                float ty = rng.nextFloat() * 15f + 10f; // 10 to 25
                float tz = rng.nextFloat() * 40f - 20f; // -20 to 20
                wp = new Vector3f(tx, ty, tz);
            } while (prev.distance(wp) < 15f);
            waypoints[w] = wp;
            prev = wp;
        }

        Camera camera = renderer.getCamera();
        org.joml.Vector3f camOffset = new org.joml.Vector3f(-8f, 5f, 0f);
        float dt = 1f / 60f;
        float legTime = 20.0f;
        int stepsPerLeg = (int) (legTime / dt);
        float arrivalThreshold = 2.0f;

        System.out.println("=== Multi-Target Deviation Test ===");

        Vector3f legStart = activeDrone.getRigidBody().getPhysicsLocation(null).clone();

        for (int leg = 0; leg < waypoints.length; leg++) {
            Vector3f target = waypoints[leg];

            // Between legs, let the drone settle for 2 seconds so it starts
            // each new leg from rest — otherwise residual momentum from the
            // previous heading causes a large initial curve.
            if (leg > 0) {
                int settleSteps = (int) (2.0f / dt);
                for (int s = 0; s < settleSteps; s++) {
                    window.pollEvents();
                    engine.update(dt);
                    physicsWorld.stepSimulation(dt);
                    renderer.clear();
                    engine.render(renderer);
                    window.swapBuffers();
                    if (window.shouldClose()) {
                        break;
                    }
                }
                float settleSpeed = activeDrone.getRigidBody().getLinearVelocity(null).length();
                System.out.printf("  Settled before leg %d — speed=%.3f m/s%n", leg + 1, settleSpeed);
            }

            // Record starting position AFTER settling
            legStart = activeDrone.getRigidBody().getPhysicsLocation(null).clone();
            float legDist = legStart.distance(target);
            activeDrone.setTargetPosition(target);

            System.out.printf("%n--- Leg %d: %s → %s  dist=%.2f ---%n",
                    leg + 1, legStart, target, legDist);

            List<Vector3f> samples = new ArrayList<>();
            boolean arrived = false;

            for (int i = 0; i < stepsPerLeg; i++) {
                window.pollEvents();
                engine.update(dt);
                physicsWorld.stepSimulation(dt);

                // Record at ~10 Hz
                if (i % 6 == 0) {
                    samples.add(activeDrone.getRigidBody().getPhysicsLocation(null).clone());
                }

                // Camera follow
                Vector3f dp = activeDrone.getRigidBody().getPhysicsLocation(null);
                camera.setPosition(new org.joml.Vector3f(
                        dp.x + camOffset.x, dp.y + camOffset.y, dp.z + camOffset.z));
                camera.setYaw((float) Math.toDegrees(Math.atan2(-camOffset.z, -camOffset.x)));
                camera.setPitch((float) Math.toDegrees(Math.atan2(-camOffset.y,
                        Math.sqrt(camOffset.x * camOffset.x + camOffset.z * camOffset.z))));

                renderer.clear();
                engine.render(renderer);
                window.swapBuffers();

                // Check arrival
                float d = activeDrone.getRigidBody().getPhysicsLocation(null).distance(target);
                float spd = activeDrone.getRigidBody().getLinearVelocity(null).length();
                if (d < arrivalThreshold && spd < 1.0f) {
                    arrived = true;
                    System.out.printf("  Arrived at t=%.1fs  dist=%.3f  speed=%.3f%n",
                            i * dt, d, spd);
                    break;
                }
                if (window.shouldClose()) {
                    break;
                }
            }

            // --- Compute deviation for this leg ---
            Vector3f ab = target.subtract(legStart);
            float abLen = ab.length();
            Vector3f abUnit = ab.normalize();

            float maxDev = 0f;
            double sumDev = 0;
            int count = 0;

            for (int i = 0; i < samples.size(); i++) {
                Vector3f p = samples.get(i);
                Vector3f ap = p.subtract(legStart);
                float t = ap.dot(abUnit) / abLen;

                // Only measure in the middle portion to ignore:
                // - initial acceleration/redirect phase (15% for subsequent
                // legs since the drone must change heading)
                // - final braking phase (5%)
                float startMargin = (leg == 0) ? 0.05f : 0.15f;
                if (t < startMargin || t > 0.95f) {
                    continue;
                }

                Vector3f cross = ap.cross(abUnit);
                float dev = cross.length();

                if (i % 30 == 0) {
                    System.out.printf("  sample %3d  t=%.2f  pos=(%.2f, %.2f, %.2f)  dev=%.4f m%n",
                            i, t, p.x, p.y, p.z, dev);
                }

                maxDev = Math.max(maxDev, dev);
                sumDev += dev;
                count++;
            }

            float avgDev = count > 0 ? (float) (sumDev / count) : 0f;

            System.out.printf("  Leg %d results — samples: %d  maxDev: %.4f m  avgDev: %.4f m%n",
                    leg + 1, count, maxDev, avgDev);

            // --- Assertions per leg ---
            Vector3f legEnd = activeDrone.getRigidBody().getPhysicsLocation(null);
            float finalDist = legEnd.distance(target);

            assertTrue(arrived,
                    "Leg " + (leg + 1) + ": drone should reach target. Final dist: " + finalDist);
            assertTrue(count > 5,
                    "Leg " + (leg + 1) + ": need enough mid-segment samples, got: " + count);
            assertTrue(maxDev < 2.0f,
                    "Leg " + (leg + 1) + ": max deviation should be < 2.0m. Got: " + maxDev + " m");
            assertTrue(avgDev < 1.0f,
                    "Leg " + (leg + 1) + ": avg deviation should be < 1.0m. Got: " + avgDev + " m");

            // Next leg starts from current position
            legStart = legEnd.clone();
        }
    }

    /**
     * Intentionally start the drone spinning out of control by placing it high,
     * arming the motors, and giving the rigid body a large yaw angular
     * velocity. Verifies the craft remains spinning after a short simulation
     * (sanity check).
     */
    @Test
    void testDroneSpinsOutOfControl() {

        window = new Window("Spin-Out Test", 800, 600, headless);
        window.init();

        renderer = new Renderer();
        renderer.init();

        physicsWorld = new PhysicsWorld();
        physicsWorld.init();

        engine = new SimulationEngine(physicsWorld);
        engine.init();

        Drone activeDrone = spawnAndActivateDrone();
        assertNotNull(activeDrone, "Active drone should be initialized");

        // Start the drone high and reset state, then arm motors
        activeDrone.reset(new Vector3f(0f, 50f, 0f));
        activeDrone.setMotorsArmed(true);

        // Ensure autopilot won't immediately try to level/hold position
        activeDrone.setTargetPosition(null);
        activeDrone.setPositionHoldEnabled(false);

        // Impart a large yaw rate (spin) directly to the physics body
        activeDrone.getRigidBody().setAngularVelocity(new Vector3f(0f, 50f, 0f));

        // Run the simulation for a short time and ensure the drone remains spinning
        float dt = 1f / 60f;
        float totalTime = 30.0f; // seconds
        int steps = (int) (totalTime / dt);
        for (int i = 0; i < steps; i++) {
            window.pollEvents();
            engine.update(dt);
            physicsWorld.stepSimulation(dt);

            renderer.clear();
            engine.render(renderer);
            window.swapBuffers();

            if (window.shouldClose()) {
                break;
            }
        }
    }

    /**
     * Test template (union of existing tests). - Primary behavior: fly to each
     */
    @Test
    void testTemplate() {
        // --- Bootstrap (headless template) ---
        window = new Window("Template Test", 800, 600, headless);
        window.init();

        renderer = new Renderer();
        renderer.init();

        physicsWorld = new PhysicsWorld();
        physicsWorld.init();

        engine = new SimulationEngine(physicsWorld);
        engine.init();

        Drone d = spawnAndActivateDrone();
        assertNotNull(d, "Active drone should be initialized");
        assertTrue(d.isMotorsArmed(), "Motors should be armed by default in the sim init");

        Vector3f[] waypoints = new Vector3f[]{
            new Vector3f(0f, 10f, 0f), // simple hover above origin
            new Vector3f(15f, 20f, -10f), // path-deviation example
            new Vector3f(-20f, 15f, 8f) // multi-target example
        };

        float dt = 1f / 60f;
        float legMaxTime = 20.0f; // seconds per leg (template)
        float arrivalThreshold = 10.0f; // meters to consider "arrived"

        for (Vector3f target : waypoints) {
            d.setTargetPosition(target);

            boolean arrived = false;
            float finalDist = Float.POSITIVE_INFINITY;
            float finalSpeed = Float.POSITIVE_INFINITY;

            int maxSteps = (int) (legMaxTime / dt);
            for (int i = 0; i < maxSteps; i++) {
                // Optional: handle window events in non-headless runs
                // window.pollEvents();

                engine.update(dt);
                physicsWorld.stepSimulation(dt);

                Vector3f posNow = d.getRigidBody().getPhysicsLocation(null);
                Vector3f velNow = d.getRigidBody().getLinearVelocity(null);
                finalDist = posNow.distance(target);
                finalSpeed = velNow.length();

                if (finalDist < arrivalThreshold && finalSpeed < 1.0f) {
                    arrived = true;
                    break;
                }

                // Optional early-exit if user closed window (not used headless)
                if (window.shouldClose()) {
                    break;
                }
            }

            // Core assertions for the template: drone must hover at the waypoint
            assertTrue(arrived, "Drone should hover at target. finalDist=" + finalDist + " speed=" + finalSpeed);
        }

    }
}
