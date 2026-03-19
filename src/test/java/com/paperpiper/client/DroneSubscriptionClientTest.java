package com.paperpiper.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

class DroneSubscriptionClientTest {

    @Test
    void subscribesAndDispatchesTelemetryAndFrames() throws IOException {
        FakeRossClient transport = new FakeRossClient();
        FakeFrameDatagramReceiver udpReceiver = new FakeFrameDatagramReceiver();
        DroneSubscriptionClient client = new DroneSubscriptionClient(transport, udpReceiver);

        List<DroneTelemetry> telemetryEvents = new ArrayList<>();
        List<SimulationFrame> frameEvents = new ArrayList<>();

        client.addTelemetryListener(telemetryEvents::add);
        client.addFrameListener(frameEvents::add);

        client.connect("localhost", 9999);
        client.startFrameStreamUdp(6001);
        client.subscribeToDrone("drone-alpha");

        String payload = Base64.getEncoder().encodeToString(new byte[]{9, 8, 7});
        transport.emit("TELEMETRY|drone-alpha|1|2|3|0.1|0.2|0.3|42");
        udpReceiver.emit("FRAME|drone-alpha|320|240|RGBA8|" + payload + "|43");

        assertEquals("FRAME_UDP_PORT|6001", transport.sentMessages.get(0));
        assertEquals("SUBSCRIBE|drone-alpha", transport.sentMessages.get(1));
        assertEquals(1, telemetryEvents.size());
        assertEquals("drone-alpha", telemetryEvents.get(0).droneId());

        assertEquals(1, frameEvents.size());
        assertEquals(320, frameEvents.get(0).width());
        assertNotNull(client.getFrameStreamBuffer().getLatestFrame().orElse(null));
    }

    @Test
    void ignoresTcpFrameMessagesWhenUdpIsEnabled() throws IOException {
        FakeRossClient transport = new FakeRossClient();
        FakeFrameDatagramReceiver udpReceiver = new FakeFrameDatagramReceiver();
        DroneSubscriptionClient client = new DroneSubscriptionClient(transport, udpReceiver);

        List<SimulationFrame> frameEvents = new ArrayList<>();
        client.addFrameListener(frameEvents::add);

        client.connect("localhost", 9999);
        client.startFrameStreamUdp(6002);

        String payload = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});
        transport.emit("FRAME|drone-alpha|320|240|RGBA8|" + payload + "|43");

        assertEquals(0, frameEvents.size());
    }

    private static class FakeRossClient implements RossClient {

        private boolean connected;
        private Consumer<String> listener = message -> {
        };
        private final List<String> sentMessages = new ArrayList<>();

        @Override
        public void connect(String host, int port) {
            connected = true;
        }

        @Override
        public void disconnect() {
            connected = false;
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public void send(String message) {
            sentMessages.add(message);
        }

        @Override
        public void setMessageListener(Consumer<String> listener) {
            this.listener = listener;
        }

        void emit(String message) {
            listener.accept(message);
        }
    }

    private static class FakeFrameDatagramReceiver implements FrameDatagramReceiver {

        private boolean running;
        private Consumer<String> listener = message -> {
        };

        @Override
        public void start(int localPort, Consumer<String> messageListener) {
            running = true;
            listener = messageListener;
        }

        @Override
        public void stop() {
            running = false;
        }

        @Override
        public boolean isRunning() {
            return running;
        }

        void emit(String message) {
            listener.accept(message);
        }
    }
}
