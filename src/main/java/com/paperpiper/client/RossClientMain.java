package com.paperpiper.client;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minimal standalone client entrypoint.
 *
 * Usage: RossClientMain <host> <port> <droneId>
 */
public final class RossClientMain {

    private static final Logger logger = LoggerFactory.getLogger(RossClientMain.class);

    private RossClientMain() {
    }

    public static void main(String[] args) {
        if (args.length != 3 && args.length != 4) {
            logger.error("Usage: RossClientMain <host> <port> <droneId> [udpFramePort]");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String droneId = args[2];
        int udpFramePort = args.length == 4 ? Integer.parseInt(args[3]) : 55000;

        DroneSubscriptionClient client = new DroneSubscriptionClient(new TcpRossClient());
        client.addTelemetryListener(telemetry -> logger.info(
                "Telemetry [{}] pos=({},{},{}) vel=({},{},{}) t={}ms",
                telemetry.droneId(),
                telemetry.posX(), telemetry.posY(), telemetry.posZ(),
                telemetry.velX(), telemetry.velY(), telemetry.velZ(),
                telemetry.timestampMillis()
        ));

        client.addFrameListener(frame -> logger.info(
                "Frame [{}] {}x{} format={} bytes={} t={}ms",
                frame.droneId(),
                frame.width(), frame.height(), frame.pixelFormat(),
                frame.payload().length,
                frame.timestampMillis()
        ));

        Runtime.getRuntime().addShutdownHook(new Thread(client::disconnect));

        try {
            client.connect(host, port);
            client.startFrameStreamUdp(udpFramePort);
            client.subscribeToDrone(droneId);
            logger.info("ROSS client connected, subscribed, and listening for frame UDP datagrams on port {}. Press Ctrl+C to exit.", udpFramePort);

            while (client.isConnected()) {
                Thread.sleep(1000L);
            }
        } catch (IOException ex) {
            logger.error("ROSS client failed", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } finally {
            client.disconnect();
        }
    }
}
