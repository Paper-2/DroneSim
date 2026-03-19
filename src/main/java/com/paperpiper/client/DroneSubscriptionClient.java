package com.paperpiper.client;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * High-level ROSS client that subscribes to a specific drone stream and handles
 * telemetry + frame messages from the server.
 */
public class DroneSubscriptionClient {

    private static final Logger logger = LoggerFactory.getLogger(DroneSubscriptionClient.class);

    private final RossClient rossClient;
    private final FrameDatagramReceiver frameDatagramReceiver;
    private final List<Consumer<DroneTelemetry>> telemetryListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<SimulationFrame>> frameListeners = new CopyOnWriteArrayList<>();
    private final FrameStreamBuffer frameStreamBuffer = new FrameStreamBuffer();
    private volatile int frameUdpPort = -1;

    public DroneSubscriptionClient(RossClient rossClient) {
        this(rossClient, new UdpFrameDatagramReceiver());
    }

    DroneSubscriptionClient(RossClient rossClient, FrameDatagramReceiver frameDatagramReceiver) {
        this.rossClient = rossClient;
        this.frameDatagramReceiver = frameDatagramReceiver;
        this.rossClient.setMessageListener(this::onMessage);
    }

    public void connect(String host, int port) throws IOException {
        rossClient.connect(host, port);
        if (frameUdpPort > 0) {
            rossClient.send("FRAME_UDP_PORT|" + frameUdpPort);
        }
    }

    public void disconnect() {
        frameDatagramReceiver.stop();
        frameUdpPort = -1;
        rossClient.disconnect();
    }

    public boolean isConnected() {
        return rossClient.isConnected();
    }

    public void subscribeToDrone(String droneId) throws IOException {
        rossClient.send("SUBSCRIBE|" + droneId);
        logger.info("Subscribed to drone {}", droneId);
    }

    public void unsubscribeFromDrone(String droneId) throws IOException {
        rossClient.send("UNSUBSCRIBE|" + droneId);
        logger.info("Unsubscribed from drone {}", droneId);
    }

    public void requestFrame(String droneId) throws IOException {
        if (frameUdpPort > 0) {
            rossClient.send("REQUEST_FRAME|" + droneId + "|UDP|" + frameUdpPort);
        } else {
            rossClient.send("REQUEST_FRAME|" + droneId);
        }
    }

    public void startFrameStreamUdp(int localPort) throws IOException {
        frameDatagramReceiver.start(localPort, this::onFrameDatagram);
        frameUdpPort = localPort;

        if (rossClient.isConnected()) {
            rossClient.send("FRAME_UDP_PORT|" + localPort);
        }
    }

    public void stopFrameStreamUdp() {
        frameDatagramReceiver.stop();
        frameUdpPort = -1;
    }

    public int getFrameUdpPort() {
        return frameUdpPort;
    }

    public void addTelemetryListener(Consumer<DroneTelemetry> listener) {
        telemetryListeners.add(listener);
    }

    public void removeTelemetryListener(Consumer<DroneTelemetry> listener) {
        telemetryListeners.remove(listener);
    }

    public void addFrameListener(Consumer<SimulationFrame> listener) {
        frameListeners.add(listener);
    }

    public void removeFrameListener(Consumer<SimulationFrame> listener) {
        frameListeners.remove(listener);
    }

    public FrameStreamBuffer getFrameStreamBuffer() {
        return frameStreamBuffer;
    }

    private void onMessage(String message) {
        RossMessageParser.parseTelemetry(message).ifPresent(telemetry -> {
            telemetryListeners.forEach(listener -> listener.accept(telemetry));
        });
    }

    private void onFrameDatagram(String message) {
        RossMessageParser.parseFrame(message).ifPresent(frame -> {
            frameStreamBuffer.acceptFrame(frame);
            frameListeners.forEach(listener -> listener.accept(frame));
        });
    }
}
