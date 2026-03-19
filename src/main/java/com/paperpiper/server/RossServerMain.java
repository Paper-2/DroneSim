package com.paperpiper.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standalone entrypoint for protocol server development/testing.
 *
 * Usage:
 *   RossServerMain [tcpPort] [droneCount]
 */
public final class RossServerMain {

    private static final Logger logger = LoggerFactory.getLogger(RossServerMain.class);

    private RossServerMain() {
    }

    public static void main(String[] args) {
        int tcpPort = extractIntArg(args, 0, 5000);
        int droneCount = extractIntArg(args, 1, 1);
        boolean headless = hasFlag(args, "--headless");

        SyntheticSimulationHardwareApi hardwareApi = new SyntheticSimulationHardwareApi(droneCount);

        RossSimulationServer server = new RossSimulationServer(
            hardwareApi,
                tcpPort,
                20,
                10
        );

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        try {
            server.start();
            logger.info("Server running. TCP port={}, drones={}. Press Ctrl+C to stop.", tcpPort, droneCount);

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

            CameraFramePayload frame = server.getCamera().capture(droneId, telemetry, tick++);
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
