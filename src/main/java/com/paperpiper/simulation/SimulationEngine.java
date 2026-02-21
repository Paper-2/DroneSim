package com.paperpiper.simulation;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jme3.math.Vector3f;
import com.paperpiper.drone.Drone;
import com.paperpiper.physics.PhysicsWorld;
import com.paperpiper.render.Mesh;
import com.paperpiper.render.MeshData;
import com.paperpiper.render.Renderer;

/**
 * Main simulation engine that coordinates drones, physics, and rendering.
 */
public class SimulationEngine {

    private static final Logger logger = LoggerFactory.getLogger(SimulationEngine.class);

    private final PhysicsWorld physicsWorld;

    private List<Drone> drones;
    private Drone activeDrone;

    private Mesh droneMesh;
    private Mesh groundMesh;
    private Mesh testCubeMesh;

    // Flight path recording (for debug trail rendering)
    private final List<org.joml.Vector3f> flightPath = new ArrayList<>();
    private static final float PATH_SAMPLE_INTERVAL = 0.1f;  // record a point every 0.1s
    private float pathSampleTimer = 0f;
    private org.joml.Vector3f startPosition = null;  // origin of the yellow ideal-path line
    private com.jme3.math.Vector3f lastKnownTarget = null;  // tracks target changes

    private final org.joml.Vector3f groundColor1 = new org.joml.Vector3f(0.35f, 0.55f, 0.35f);
    private final org.joml.Vector3f groundColor2 = new org.joml.Vector3f(0.25f, 0.45f, 0.25f);
    private final float checkerScale = 10.0f; // Size of each checker square

    private final Matrix4f groundMatrix;

    private boolean paused = false;
    private float simulationTime = 0;

    public SimulationEngine(PhysicsWorld physicsWorld) {
        this.physicsWorld = physicsWorld;
        this.drones = new ArrayList<>();
        this.groundMatrix = new Matrix4f().identity(); // Ground plane at y=0
    }

    /**
     * Initialize the simulation
     */
    public void init() {
        logger.info("Initializing simulation engine...");

        physicsWorld.createGroundPlane();

        // Create render meshes
        groundMesh = Mesh.createPlane(10000f, 10000f); // Shouldn't be removed future me!

        Drone drone = addDrone(new Vector3f(0, 2, 0));

        drone.setMotorsArmed(true);

        setActiveDrone(drones.get(0));

        // Set controller to move active drone to (0, 10, 0)
        setActiveDroneTargetPosition(new org.joml.Vector3f(0, 10, 0));

        logger.info("Simulation initialized with {} drone(s)", drones.size());
    }

    /**
     * Update simulation
     */
    public void update(float deltaTime) {
        if (paused) {
            return;
        }

        simulationTime += deltaTime;

        for (Drone drone : drones) {
            drone.update(deltaTime);
        }

        // Record flight path of active drone at regular intervals
        if (activeDrone != null) {
            // Detect target changes — update the yellow-line origin to the
            // previous target (or the drone's current position for the first target).
            Vector3f currentTarget = activeDrone.getTargetPosition();
            if (currentTarget != null) {
                if (lastKnownTarget == null) {
                    // First target ever: yellow line starts from the drone's current position
                    Vector3f p = activeDrone.getPosition();
                    startPosition = new org.joml.Vector3f(p.x, p.y, p.z);
                    lastKnownTarget = new Vector3f(currentTarget);
                } else if (!currentTarget.equals(lastKnownTarget)) {
                    // Target changed: yellow line starts from the old target
                    startPosition = new org.joml.Vector3f(
                            lastKnownTarget.x, lastKnownTarget.y, lastKnownTarget.z);
                    lastKnownTarget.set(currentTarget);
                }
            }

            pathSampleTimer += deltaTime;
            if (pathSampleTimer >= PATH_SAMPLE_INTERVAL) {
                pathSampleTimer -= PATH_SAMPLE_INTERVAL;
                Vector3f p = activeDrone.getPosition();
                flightPath.add(new org.joml.Vector3f(p.x, p.y, p.z));
            }
        }
    }

    /**
     * Render simulation
     */
    public void render(Renderer renderer) {

        renderer.updateProjection(1280, 720);

        renderer.render();

        renderer.renderGround(groundMesh, groundMatrix, groundColor1, groundColor2, checkerScale);

        for (Drone drone : drones) {
            if (drone.getModel() != null) {

                for (MeshData meshData : drone.getModel().getMeshesWithTransforms()) {
                    // Combine drone's model matrix with mesh's local transform
                    Matrix4f combinedMatrix = new Matrix4f(drone.getModelMatrix()).mul(meshData.getLocalTransform());

                    // Render collision debug boxes with 50% transparency
                    String meshName = meshData.getMesh().getMeshName();
                    if (meshName != null && (meshName.startsWith("debug_collision_") || meshName.startsWith("collision_"))) {
                        renderer.renderMesh(meshData.getMesh(), combinedMatrix, meshData.getColor(), 0.5f);
                    } else {
                        renderer.renderMesh(meshData.getMesh(), combinedMatrix, meshData.getColor());
                    }
                }
            }
        }

        // Draw debug line from active drone to its target
        if (activeDrone != null && activeDrone.getTargetPosition() != null) {
            com.jme3.math.Vector3f dp = activeDrone.getPosition();
            com.jme3.math.Vector3f tp = activeDrone.getTargetPosition();
            renderer.renderLine(
                    new org.joml.Vector3f(dp.x, dp.y, dp.z),
                    new org.joml.Vector3f(tp.x, tp.y, tp.z),
                    new org.joml.Vector3f(1.0f, 0.2f, 0.2f));  // red line
        }

        // Draw ideal straight-line path from start to target
        if (startPosition != null && activeDrone != null && activeDrone.getTargetPosition() != null) {
            com.jme3.math.Vector3f tp2 = activeDrone.getTargetPosition();
            renderer.renderLine(
                    new org.joml.Vector3f(startPosition),
                    new org.joml.Vector3f(tp2.x, tp2.y, tp2.z),
                    new org.joml.Vector3f(1.0f, 1.0f, 0.2f));  // yellow line
        }

        // Draw flight path trail
        if (flightPath.size() >= 2) {
            renderer.renderLineStrip(flightPath,
                    new org.joml.Vector3f(0.2f, 1.0f, 0.4f));  // green trail
        }

        renderer.endRender();
    }

    /**
     * Add a new drone to the simulation with a vector3f position
     */
    public Drone addDrone(Vector3f position) {
        Drone drone = new Drone();
        drone.init(physicsWorld, position);
        drone.setCollisionShapesVisible(true); // Debug: show collision shapes
        drones.add(drone);
        return drone;
    }

    /**
     * Remove a drone from the simulation
     */
    public void removeDrone(Drone drone) {
        drone.cleanup(physicsWorld);
        drones.remove(drone);
        if (activeDrone == drone) {
            activeDrone = drones.isEmpty() ? null : drones.get(0);
        }
    }

    /**
     * Get active drone (player controlled)
     */
    public Drone getActiveDrone() {
        return activeDrone;
    }

    /**
     * Set active drone
     */
    public void setActiveDrone(Drone drone) {
        if (drones.contains(drone)) {
            this.activeDrone = drone;
        }
    }

    /**
     * Set target position for the active drone to move toward
     */
    public void setActiveDroneTargetPosition(org.joml.Vector3f target) {
        if (activeDrone != null) {
            activeDrone.setTargetPosition(new Vector3f(target.x, target.y, target.z));
        }
    }

    /**
     * Get all drones
     */
    public List<Drone> getDrones() {
        return drones;
    }

    /**
     * Pause/unpause simulation
     */
    public void setPaused(boolean paused) {
        this.paused = paused;
        logger.info("Simulation {}", paused ? "PAUSED" : "RESUMED");
    }

    /**
     * Check if simulation is paused
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * Get simulation time
     */
    public float getSimulationTime() {
        return simulationTime;
    }

    /**
     * Reset simulation
     */
    public void reset() {
        logger.info("Resetting simulation...");
        simulationTime = 0;
        flightPath.clear();
        pathSampleTimer = 0f;
        startPosition = null;
        lastKnownTarget = null;

        int i = 0;
        for (Drone drone : drones) {
            drone.reset(new Vector3f(i * 3, 2, 0));
            i++;
        }
    }

    /**
     * Toggle collision shape visualization for all drones.
     */
    public void toggleCollisionShapesVisible() {
        for (Drone drone : drones) {
            boolean newState = !drone.isCollisionShapesVisible();
            drone.setCollisionShapesVisible(newState);
        }
    }

    public void cleanup() {
        logger.info("Cleaning up simulation...");

        for (Drone drone : drones) {
            drone.cleanup(physicsWorld);
        }
        drones.clear();

        if (droneMesh != null) {
            droneMesh.cleanup();
        }
        if (groundMesh != null) {
            groundMesh.cleanup();
        }
    }
}
