package com.paperpiper.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.paperpiper.ross.FrameData;

/**
 * Standalone entrypoint for protocol server development/testing.
 *
 * Usage: RossServerMain [tcpPort] [droneCount]
 */
public final class RossServerMain {

    private static final Logger logger = LoggerFactory.getLogger(RossServerMain.class);

    private RossServerMain() {
    }

    public static void main(String[] args) {
        int tcpPort = extractIntArg(args, 0, 5000);
        int droneCount = extractIntArg(args, 1, 1);
        boolean headless = hasFlag(args, "--headless");
        boolean enableRos2 = hasFlag(args, "--ros2");

        SyntheticSimulationHardwareApi hardwareApi = new SyntheticSimulationHardwareApi(droneCount);

        RossSimulationServer server = new RossSimulationServer(
                hardwareApi,
                tcpPort,
                20,
                10
        );

        Ros2Bridge ros2Bridge = enableRos2
                ? new Ros2Bridge(hardwareApi, server.getCamera(), 20, 10)
                : null;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop();
            if (ros2Bridge != null) {
                ros2Bridge.close();
            }
        }));

        try {
            server.start();
            if (ros2Bridge != null) {
                ros2Bridge.start();
                logger.info("ROS2 bridge enabled");
            }
            logger.info("Server running. TCP port={}, drones={}, ros2={}. Press Ctrl+C to stop.",
                    tcpPort, droneCount, enableRos2);

            if (!headless) {
                runWithCameraWindow(server, hardwareApi);
            } else {
                while (server.isRunning()) {
                    Thread.sleep(1000L);
                }
            }
        } catch (Exception ex) {
            logger.error("Server failed", ex);
        } finally {
            server.stop();
            if (ros2Bridge != null) {
                ros2Bridge.close();
            }
        }
    }

    private static void runWithCameraWindow(RossSimulationServer server, SyntheticSimulationHardwareApi hardwareApi) throws InterruptedException {
        ServerCameraWindow window = new ServerCameraWindow("ROSS Server Camera");
        window.show();

        long tick = 0L;
        while (server.isRunning() && window.isOpen()) {
            var active = hardwareApi.getActiveDrone().orElse(null);
            String droneId = active != null ? active.getDroneId() : "drone-1";
            var telemetry = active != null ? active.readTelemetry() : null;

            FrameData frame = server.getCamera().capture(droneId, telemetry, tick++);
            window.render(frame);

            Thread.sleep(66L);
        }

        window.close();
        server.stop();
    }

    private static int extractIntArg(String[] args, int index, int defaultValue) {
        if (args.length <= index || args[index].startsWith("--")) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(args[index]);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static boolean hasFlag(String[] args, String flag) {
        for (String arg : args) {
            if (flag.equalsIgnoreCase(arg)) {
                return true;
            }
        }
        return false;
    }
}
