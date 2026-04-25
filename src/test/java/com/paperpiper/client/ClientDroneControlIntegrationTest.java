package com.paperpiper.client;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.paperpiper.hardware.DroneHardwareApi;
import com.paperpiper.hardware.MotorOutputSample;
import com.paperpiper.server.RossSimulationServer;
import com.paperpiper.server.SyntheticSimulationHardwareApi;

/**
 * Integration test: a client connects, arms the drone, sends manual control,
 * and the server-side hardware API reflects the change.
 */
class ClientDroneControlIntegrationTest {

    private static final int TCP_PORT = 5620;

    private SyntheticSimulationHardwareApi hardwareApi;
    private RossSimulationServer server;

    @BeforeEach
    void setUp() throws Exception {
        hardwareApi = new SyntheticSimulationHardwareApi(1);
        server = new RossSimulationServer(hardwareApi, TCP_PORT, 20, 5);
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void clientManualControlMovesDrone() throws Exception {
        DroneHardwareApi drone = hardwareApi.getDrone("drone-1").orElseThrow();

        // Before: drone is not armed, motors should be zero
        MotorOutputSample before = drone.readMotorOutputs();
        assertEquals(0f, before.frontLeft(), 0.001f);
        assertEquals(false, before.motorsArmed());

        // Client connects, subscribes, arms, and sends throttle
        DroneSubscriptionClient client = new DroneSubscriptionClient(new TcpRossClient());
        try {
            client.connect("127.0.0.1", TCP_PORT);
            client.subscribeToDrone("drone-1");
            client.sendArm(true);
            client.sendManualControl(0.8f, 0.0f, 0.0f, 0.0f);

            // Give the commands time to propagate over TCP
            Thread.sleep(200);

            // After: motors should reflect the throttle
            MotorOutputSample after = drone.readMotorOutputs();
            assertTrue(after.motorsArmed(), "Drone should be armed");
            assertTrue(after.frontLeft() > 0.5f,
                    "Front-left motor should spin at ~0.8 throttle, got " + after.frontLeft());
            assertTrue(after.frontRight() > 0.5f,
                    "Front-right motor should spin at ~0.8 throttle, got " + after.frontRight());
            assertTrue(after.rearLeft() > 0.5f,
                    "Rear-left motor should spin at ~0.8 throttle, got " + after.rearLeft());
            assertTrue(after.rearRight() > 0.5f,
                    "Rear-right motor should spin at ~0.8 throttle, got " + after.rearRight());
        } finally {
            client.disconnect();
        }
    }

    @Test
    void clientPitchControlAffectsMotorMix() throws Exception {
        DroneHardwareApi drone = hardwareApi.getDrone("drone-1").orElseThrow();

        DroneSubscriptionClient client = new DroneSubscriptionClient(new TcpRossClient());
        try {
            client.connect("127.0.0.1", TCP_PORT);
            client.subscribeToDrone("drone-1");
            client.sendArm(true);
            // Full throttle + full pitch forward → front motors higher than rear
            client.sendManualControl(0.5f, 1.0f, 0.0f, 0.0f);

            Thread.sleep(200);

            MotorOutputSample outputs = drone.readMotorOutputs();
            assertTrue(outputs.motorsArmed());
            assertTrue(outputs.frontLeft() > outputs.rearLeft(),
                    "Front-left should exceed rear-left with positive pitch");
            assertTrue(outputs.frontRight() > outputs.rearRight(),
                    "Front-right should exceed rear-right with positive pitch");
        } finally {
            client.disconnect();
        }
    }
}
