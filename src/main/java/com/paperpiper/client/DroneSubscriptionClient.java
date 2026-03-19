package com.paperpiper.client;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.paperpiper.ross.FrameData;
import com.paperpiper.ross.RossCodec;
import com.paperpiper.ross.TelemetryData;

/**
 * High-level ROSS client that subscribes to a specific drone stream and handles
 * telemetry + frame messages from the server.
 */
public class DroneSubscriptionClient {

    private static final Logger logger = LoggerFactory.getLogger(DroneSubscriptionClient.class);

    private final RossClient rossClient;
    private final FrameDatagramReceiver frameDatagramReceiver;
    private final List<Consumer<TelemetryData>> telemetryListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<FrameData>> frameListeners = new CopyOnWriteArrayList<>();
    private final FrameStreamBuffer frameStreamBuffer = new FrameStreamBuffer();
    private volatile int frameUdpPort = -1;
    private volatile CompletableFuture<String> pendingSubscription;

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

    /**
     * Subscribes to a drone and waits for the server to confirm.
     *
     * @throws IOException if the subscription is rejected or the server does
     * not respond within 5 seconds
     */
    public void subscribeToDrone(String droneId) throws IOException {
        pendingSubscription = new CompletableFuture<>();
        rossClient.send("SUBSCRIBE|" + droneId);

        try {
            String result = pendingSubscription.get(5, TimeUnit.SECONDS);
            if (result.startsWith("ERROR")) {
                throw new IOException("Subscription rejected by server: " + result);
            }
            logger.info("Subscribed to drone {}", droneId);
        } catch (TimeoutException ex) {
            throw new IOException("Server did not respond to SUBSCRIBE within 5 seconds");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException("Subscription interrupted", ex);
        } catch (ExecutionException ex) {
            throw new IOException("Subscription failed", ex.getCause());
        } finally {
            pendingSubscription = null;
        }
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

    public void addTelemetryListener(Consumer<TelemetryData> listener) {
        telemetryListeners.add(listener);
    }

    public void removeTelemetryListener(Consumer<TelemetryData> listener) {
        telemetryListeners.remove(listener);
    }

    public void addFrameListener(Consumer<FrameData> listener) {
        frameListeners.add(listener);
    }

    public void removeFrameListener(Consumer<FrameData> listener) {
        frameListeners.remove(listener);
    }

    public FrameStreamBuffer getFrameStreamBuffer() {
        return frameStreamBuffer;
    }

    public void sendManualControl(float throttle, float pitch, float roll, float yaw) throws IOException {
        rossClient.send("MANUAL_CONTROL|" + throttle + "|" + pitch + "|" + roll + "|" + yaw);
    }

    public void sendArm(boolean armed) throws IOException {
        rossClient.send("ARM|" + armed);
    }

    public void sendCameraOffset(float yawOffset, float pitchOffset, float distance) throws IOException {
        rossClient.send("CAMERA_OFFSET|" + yawOffset + "|" + pitchOffset + "|" + distance);
    }

    private void onMessage(String message) {
        // Handle subscription ack/error
        CompletableFuture<String> pending = pendingSubscription;
        if (pending != null && (message.startsWith("OK|SUBSCRIBED|") || message.startsWith("ERROR|UNKNOWN_DRONE|"))) {
            pending.complete(message);
            return;
        }

        RossCodec.decodeTelemetry(message).ifPresent(telemetry -> {
            telemetryListeners.forEach(listener -> listener.accept(telemetry));
        });
    }

    private void onFrameDatagram(String message) {
        RossCodec.decodeFrame(message).ifPresent(frame -> {
            frameStreamBuffer.acceptFrame(frame);
            frameListeners.forEach(listener -> listener.accept(frame));
        });
    }
}
