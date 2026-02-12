package com.paperpiper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.paperpiper.physics.PhysicsWorld;
import com.paperpiper.render.Camera; 
import com.paperpiper.render.Renderer;
import com.paperpiper.render.Window;
import com.paperpiper.simulation.SimulationEngine;

import static org.lwjgl.glfw.GLFW.*;

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
    private boolean mouseCaptured = false;

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
            // renderer.render(); simulation.render() calls renderer.render() internally, so we don't need to call it here

            window.swapBuffers();
        }
    }

    // TODO: input. Should be able to handle keyboard/mouse + controller (steamdeck)
    private void handleInput() {
        float deltaTime = 1.0f / 60.0f;
        Camera camera = renderer.getCamera();

        // Toggle mouse capture with Tab key
        if (window.isKeyPressed(GLFW_KEY_TAB)) {
            // Simple debounce - only toggle once per press
            mouseCaptured = !mouseCaptured;
            window.setCursorCaptured(mouseCaptured);
            if (mouseCaptured) {
                camera.resetMouseState();
            }
            // Wait for key release to prevent rapid toggling
            // 
            while (window.isKeyPressed(GLFW_KEY_TAB)) {
                window.pollEvents();
            }
        }
        // There should be a simpler way to record input. InputManager?
        // Camera movement controls (WASD + Space/Ctrl) 
        boolean forward = window.isKeyPressed(GLFW_KEY_W);
        boolean backward = window.isKeyPressed(GLFW_KEY_S);
        boolean left = window.isKeyPressed(GLFW_KEY_A);
        boolean right = window.isKeyPressed(GLFW_KEY_D);
        boolean up = window.isKeyPressed(GLFW_KEY_SPACE);
        boolean down = window.isKeyPressed(GLFW_KEY_LEFT_CONTROL);
        boolean sprint = window.isKeyPressed(GLFW_KEY_LEFT_SHIFT);

        camera.processKeyboard(forward, backward, left, right, up, down, sprint, deltaTime);

        // Mouse look (only when captured)
        if (mouseCaptured) {
            camera.processMouseMovement(window.getMouseX(), window.getMouseY());
        }

        // Close on ESC key
        if (window.isKeyPressed(GLFW_KEY_ESCAPE)) {
            if (mouseCaptured) {
                mouseCaptured = false;
                window.setCursorCaptured(false);
            } else {
                running = false;
            }
        }

        // Toggle collision shape visualization with F3
        if (window.isKeyPressed(GLFW_KEY_F3)) {
            simulation.toggleCollisionShapesVisible();
            // Wait for key release to prevent rapid toggling
            while (window.isKeyPressed(GLFW_KEY_F3)) {
                window.pollEvents();
            }
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
