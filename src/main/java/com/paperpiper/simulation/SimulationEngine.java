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
    private Matrix4f testCubeMatrix;

    private final org.joml.Vector3f droneColor = new org.joml.Vector3f(0.2f, 0.6f, 0.9f);
    private final org.joml.Vector3f testCubeColor = new org.joml.Vector3f(1.0f, 0.3f, 0.3f); // Red
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

        // Add test cube (1x1x1) below where front-left propeller will be
        // Half-extents of 0.5 = 1m cube, positioned at (0.5, 0.5, -0.5)
        physicsWorld.createBox(new Vector3f(0.5f, 0.5f, 0.5f), 0f, new Vector3f(0.5f, 0.5f, -0.5f));
        logger.info("Added test collision cube at (0.5, 0.5, -0.5)");

        // Create render meshes
        groundMesh = Mesh.createPlane(10000f, 10000f);
        testCubeMesh = Mesh.createBox(10.0f, 1.0f, 10.0f); // 1x1x1 cube
        testCubeMatrix = new Matrix4f().identity().translate(0.5f, 0.5f, -0.5f);

        // Spawn 100 drones in a 10x10 grid
        int gridSize = 10;
        float spacing = 3.0f; // 3 meters between drones
        float startOffset = -((gridSize - 1) * spacing) / 2.0f;
        
        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                float x = startOffset + col * spacing;
                float z = startOffset + row * spacing;
                float y = 5.0f; // Start 5m above ground
                addDrone(new Vector3f(x, y, z));
            }
        }
        
        setActiveDrone(drones.get(0));

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
    }

    /**
     * Render simulation
     */
    public void render(Renderer renderer) {

        renderer.updateProjection(1280, 720);

        renderer.render();

        renderer.renderGround(groundMesh, groundMatrix, groundColor1, groundColor2, checkerScale);

        // Render test cube
        renderer.renderMesh(testCubeMesh, testCubeMatrix, testCubeColor);

        for (Drone drone : drones) {
            if (drone.getModel() != null) {

                for (MeshData meshData : drone.getModel().getMeshesWithTransforms()) {
                    // Combine drone's model matrix with mesh's local transform
                    Matrix4f combinedMatrix = new Matrix4f(drone.getModelMatrix()).mul(meshData.getLocalTransform());
                    renderer.renderMesh(meshData.getMesh(), combinedMatrix, meshData.getColor());
                }
            }
        }

        renderer.endRender();
    }

    /**
     * Add a new drone to the simulation
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
