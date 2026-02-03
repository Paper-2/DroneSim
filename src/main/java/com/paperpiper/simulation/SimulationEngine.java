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
    
    
    private final org.joml.Vector3f droneColor = new org.joml.Vector3f(0.2f, 0.6f, 0.9f);
    private final org.joml.Vector3f groundColor = new org.joml.Vector3f(0.3f, 0.5f, 0.3f);
    
    
    private Matrix4f groundMatrix;
    
    
    private boolean paused = false;
    private float simulationTime = 0;
    
    public SimulationEngine(PhysicsWorld physicsWorld) {
        this.physicsWorld = physicsWorld;
        this.drones = new ArrayList<>();
        this.groundMatrix = new Matrix4f().identity();
    }
    
    /**
     * Initialize the simulation
     */
    public void init() {
        logger.info("Initializing simulation engine...");
        
        
        physicsWorld.createGroundPlane();
        
        
        droneMesh = Mesh.createCube(1.0f); 
        groundMesh = Mesh.createPlane(100f, 100f);
        
        
        Drone drone = new Drone();
        drone.init(physicsWorld, new Vector3f(0, 2, 0));
        drones.add(drone);
        activeDrone = drone;
        
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
        
        
        renderer.renderMesh(groundMesh, groundMatrix, groundColor);
        
        
        for (Drone drone : drones) {
            renderer.renderMesh(droneMesh, drone.getModelMatrix(), droneColor);
        }
        
        
        renderer.endRender();
    }
    
    /**
     * Add a new drone to the simulation
     */
    public Drone addDrone(Vector3f position) {
        Drone drone = new Drone();
        drone.init(physicsWorld, position);
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
