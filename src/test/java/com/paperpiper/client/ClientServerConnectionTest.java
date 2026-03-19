package com.paperpiper.client;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.paperpiper.server.RossSimulationServer;
import com.paperpiper.server.SyntheticSimulationHardwareApi;

class ClientServerConnectionTest {

    private static final int TCP_PORT = 5610;

    private RossSimulationServer server;

    @BeforeEach
    void setUp() throws Exception {
        server = new RossSimulationServer(new SyntheticSimulationHardwareApi(1), TCP_PORT, 20, 5);
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void clientConnectsToServerSuccessfully() throws Exception {
        TcpRossClient client = new TcpRossClient();
        assertFalse(client.isConnected());

        client.connect("127.0.0.1", TCP_PORT);
        assertTrue(client.isConnected());

        client.disconnect();
        assertFalse(client.isConnected());
    }

    @Test
    void clientReceivesTelemetryAfterSubscription() throws Exception {
        TcpRossClient client = new TcpRossClient();
        CountDownLatch telemetryReceived = new CountDownLatch(1);

        client.setMessageListener(message -> {
            if (message.startsWith("TELEMETRY|drone-1|")) {
                telemetryReceived.countDown();
            }
        });

        client.connect("127.0.0.1", TCP_PORT);
        client.send("SUBSCRIBE|drone-1");

        assertTrue(telemetryReceived.await(3, TimeUnit.SECONDS),
                "Should receive telemetry within 3 seconds");

        client.disconnect();
    }

    @Test
    void subscriptionToNewDroneSpawnsIt() throws Exception {
        DroneSubscriptionClient client = new DroneSubscriptionClient(new TcpRossClient());
        client.connect("127.0.0.1", TCP_PORT);

        // Server auto-spawns drones on subscribe — should not throw
        client.subscribeToDrone("brand-new-drone");

        client.disconnect();
    }

    @Test
    void subscriptionToKnownDroneSucceeds() throws Exception {
        DroneSubscriptionClient client = new DroneSubscriptionClient(new TcpRossClient());
        client.connect("127.0.0.1", TCP_PORT);

        // Should not throw — drone-1 exists in the SyntheticSimulationHardwareApi(1)
        client.subscribeToDrone("drone-1");

        client.disconnect();
    }
}
