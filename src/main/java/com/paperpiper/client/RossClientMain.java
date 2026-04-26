package com.paperpiper.client;

import java.io.IOException;

import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import org.lwjgl.glfw.GLFWErrorCallback;
import static org.lwjgl.system.MemoryUtil.NULL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.paperpiper.input.GameController;

/**
 * ROSS client with a display window and gamepad input.
 *
 * Usage: RossClientMain &lt;host&gt; &lt;port&gt; &lt;droneId&gt;
 * [udpFramePort]
 *
 * Opens a Swing window showing the camera feed and polls a game controller for
 * manual drone control (throttle/pitch/roll/yaw) sent via ROSS protocol.
 */
public final class RossClientMain {

    private static final Logger logger = LoggerFactory.getLogger(RossClientMain.class);

    /**
     * How often we send control commands (Hz).
     */
    private static final int CONTROL_RATE_HZ = 30;

    private RossClientMain() {
    }

    public static void main(String[] args) {
        if (args.length < 3 || args.length > 4) {
            logger.error("Usage: RossClientMain <host> <port> <droneId> [udpFramePort]");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String droneId = args[2];
        int udpFramePort = args.length == 4 ? Integer.parseInt(args[3]) : 55000;

        // --- GLFW init (needed for controller polling) ---
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            logger.error("Failed to initialize GLFW");
            return;
        }

        // Hidden 1x1 window – we only need a GLFW context for joystick polling
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        long glfwWindow = glfwCreateWindow(1, 1, "ROSS Input", NULL, NULL);
        if (glfwWindow == NULL) {
            logger.error("Failed to create GLFW context window");
            glfwTerminate();
            return;
        }

        // Disable cursor so Steam Deck trackpads/gyro don't move the system mouse
        glfwSetInputMode(glfwWindow, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        GameController controller = new GameController();
        if (controller.isConnected()) {
            logger.info("Controller detected: {}", controller.getName());
        } else {
            logger.info("No controller detected  will scan each frame");
        }

        // --- Network client ---
        DroneSubscriptionClient client = new DroneSubscriptionClient(new TcpRossClient());

        // --- Display window (Swing) ---
        ClientDisplayWindow displayWindow = new ClientDisplayWindow("ROSS Client  " + droneId);

        client.addTelemetryListener(telemetry -> logger.debug(
                "Telemetry [{}] pos=({},{},{}) vel=({},{},{}) t={}ms",
                telemetry.droneId(),
                telemetry.posX(), telemetry.posY(), telemetry.posZ(),
                telemetry.velX(), telemetry.velY(), telemetry.velZ(),
                telemetry.timestampMillis()
        ));

        client.addFrameListener(frame -> displayWindow.render(frame));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            client.disconnect();
            displayWindow.close();
        }));

        try {
            client.connect(host, port);
            client.startFrameStreamUdp(udpFramePort);
            client.subscribeToDrone(droneId); // throws if server rejects
            displayWindow.show();

            logger.info("ROSS client connected to {}:{}, drone={}, UDP frames on port {}. Controller: {}",
                    host, port, droneId, udpFramePort,
                    controller.isConnected() ? controller.getName() : "none");

            long controlIntervalMs = 1000L / CONTROL_RATE_HZ;
            boolean wasArmToggled = false;

            // Main loop: poll controller input → send commands, display frames
            while (client.isConnected() && displayWindow.isOpen()) {
                glfwPollEvents();
                controller.update();

                if (controller.isConnected()) {
                    client.sendManualControl(
                            controller.getThrottle(),
                            controller.getPitch(),
                            controller.getRoll(),
                            controller.getYaw()
                    );

                    client.sendCameraOffset(
                            controller.getCameraYawOffset(),
                            controller.getCameraPitchOffset(),
                            controller.getCameraDistance()
                    );

                    if (controller.isArmToggled()) {
                        // Toggle arm state  for simplicity we just send true/false alternating
                        wasArmToggled = !wasArmToggled;
                        client.sendArm(wasArmToggled);
                        logger.info("Arm toggled: {}", wasArmToggled);
                    }
                }

                Thread.sleep(controlIntervalMs);
            }
        } catch (IOException ex) {
            logger.error("ROSS client failed: {}", ex.getMessage());
            System.exit(1);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } finally {
            client.disconnect();
            displayWindow.close();
            glfwDestroyWindow(glfwWindow);
            glfwTerminate();
        }
    }
}
