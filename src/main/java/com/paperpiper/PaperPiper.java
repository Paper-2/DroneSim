package com.paperpiper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.paperpiper.physics.PhysicsWorld;
import com.paperpiper.render.Renderer;
import com.paperpiper.render.Window;
import com.paperpiper.simulation.SimulationEngine;

/**
 * PaperPiper - Drone Simulator
 */
public class PaperPiper {

    private static final Logger logger = LoggerFactory.getLogger(PaperPiper.class);

    private Window window;
    private Renderer renderer;
    private PhysicsWorld physicsWorld;
    private SimulationEngine simulation;

    private boolean running = false;

    public static void main(String[] args) {
        logger.info("Starting PaperPiper Drone Simulator...");

        PaperPiper app = new PaperPiper();
        try {
            app.init();
            app.run();
        } catch (Exception e) {
            logger.error("Fatal error in PaperPiper", e);
        } finally {
            app.cleanup();
        }
    }

    private void init() {
        logger.info("Initializing subsystems...");

        window = new Window("PaperPiper - Drone Simulator", 1280, 720);
        window.init();

        // Initialize renderer
        renderer = new Renderer();
        renderer.init();

        // Initialize Bullet
        physicsWorld = new PhysicsWorld();
        physicsWorld.init();

        // Initialize simulation engine
        simulation = new SimulationEngine(physicsWorld);
        simulation.init();

        running = true;
        logger.info("Initialization complete!");
    }

    private void run() {
        logger.info("Entering main loop...");

        long lastTime = System.nanoTime();
        final double targetFps = 60.0; // Framerate
        final double nsPerTick = 1000000000.0 / targetFps;
        double delta = 0;

        while (running && !window.shouldClose()) {
            long now = System.nanoTime();
            delta += (now - lastTime) / nsPerTick;
            lastTime = now;

            window.pollEvents();
            handleInput();

            while (delta >= 1) {
                float dt = (float) (1.0 / targetFps);
                simulation.update(dt);
                physicsWorld.stepSimulation(dt);
                delta--;
            }

            // Render
            renderer.clear();
            simulation.render(renderer);
            renderer.render();

            window.swapBuffers();
        }
    }

    // TODO: input. Should be able to handle keyboard/mouse + controller (steamdeck)
    private void handleInput() {

        // Close on ESC key
        if (window.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE)) {
            running = false;
        }
    }

    
    private void cleanup() {
        logger.info("Cleaning up resources...");

        if (simulation != null) {
            simulation.cleanup();
        }
        if (physicsWorld != null) {
            physicsWorld.cleanup();
        }
        if (renderer != null) {
            renderer.cleanup();
        }
        if (window != null) {
            window.cleanup();
        }

        logger.info("PaperPiper shutdown complete.");
    }
}
