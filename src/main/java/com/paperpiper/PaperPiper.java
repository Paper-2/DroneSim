package com.paperpiper;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F3;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_TAB;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.paperpiper.physics.PhysicsWorld;
import com.paperpiper.render.Camera;
import com.paperpiper.render.Renderer;
import com.paperpiper.render.Window;
import com.paperpiper.server.LiveSimulationHardwareApi;
import com.paperpiper.server.RossSimulationServer;
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
    //private GameController controller;

    private boolean running = false;
    private boolean mouseCaptured = false;

    // ROSS server integration (enabled with --server flag)
    private boolean serverMode = false;
    private RossSimulationServer rossServer;
    private LiveSimulationHardwareApi liveHardwareApi;

    public static void main(String[] args) {
        logger.info("Starting PaperPiper Drone Simulator...");

        PaperPiper app = new PaperPiper();
        app.parseArgs(args);
        try {
            app.init();
            app.run();
        } catch (Exception e) {
            logger.error("Fatal error in PaperPiper", e);
        } finally {
            app.cleanup();
        }
    }

    private void parseArgs(String[] args) {
        for (String arg : args) {
            if ("--server".equals(arg)) {
                serverMode = true;
            }
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

        // Start ROSS server if --server flag was given
        if (serverMode) {
            try {
                liveHardwareApi = new LiveSimulationHardwareApi(simulation);
                rossServer = new RossSimulationServer(liveHardwareApi, 5000, 20, 10);
                rossServer.start();
                logger.info("ROSS server started on port 5000");
            } catch (Exception e) {
                logger.error("Failed to start ROSS server", e);
                rossServer = null;
            }
        }

        // Initialize controller input
        // controller = new GameController();
        // if (controller.isConnected()) {
        //     logger.info("Controller detected: {}", controller.getName());
        // } else {
        //     logger.info("No controller detected — will scan each frame");
        // }
        running = true;
        logger.info("Initialization complete!");
    }

    private void run() {
        logger.info("Entering main loop...");

        long lastTime = System.nanoTime();
        final double targetFps = 60.0; // Framerate
        final double nsPerTick = 1000000000.0 / targetFps;
        double delta = 0;

        // FPS counter variables
        int frameCount = 0;
        long fpsTimer = System.currentTimeMillis();
        int lastFps = 0;

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

            // Capture framebuffer for ROSS streaming (after render, before swap)
            if (rossServer != null && rossServer.isRunning()) {
                liveHardwareApi.processPendingSpawns();
                liveHardwareApi.refreshDrones();
                byte[] pixels = renderer.captureFramebuffer(window.getWidth(), window.getHeight());
                rossServer.getCamera().supplyLiveFrame(pixels, window.getWidth(), window.getHeight());
            }

            window.swapBuffers();

            // FPS counting
            frameCount++;
            if (System.currentTimeMillis() - fpsTimer >= 1000) {
                lastFps = frameCount;
                frameCount = 0;
                fpsTimer = System.currentTimeMillis();
                window.setTitle(String.format("PaperPiper - Drone Simulator | FPS: %d | Drones: %d",
                        lastFps, simulation.getDrones().size()));
            }
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

        // Disabled because its not meant to be used. But it can be used if the need arises.
/*        controller.update();
        if (controller.isConnected() && simulation.getActiveDrone() != null) {
            var drone = simulation.getActiveDrone();

            drone.setThrottle(controller.getThrottle());
            drone.setPitch(controller.getPitch());
            drone.setRoll(controller.getRoll());
            drone.setYaw(controller.getYaw());

            if (controller.isArmToggled()) {
                drone.setMotorsArmed(!drone.isMotorsArmed());
            }
            if (controller.isResetPressed()) {
                drone.reset(new com.jme3.math.Vector3f(0, 2, 0));
            }
        }*/
    }

    private void cleanup() {
        logger.info("Cleaning up resources...");

        if (rossServer != null) {
            rossServer.stop();
        }

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
