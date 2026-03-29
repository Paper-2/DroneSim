package com.paperpiper.hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

import com.paperpiper.drone.Drone;

class SimulatedDroneHardwareApiTest {

    @Test
    void readsTelemetryWithoutInitializedRigidBody() {
        Drone drone = new Drone();
        SimulatedDroneHardwareApi api = new SimulatedDroneHardwareApi("drone-test", drone);

        DroneTelemetrySample telemetry = api.readTelemetry();

        assertEquals("drone-test", telemetry.droneId());
        assertNotNull(telemetry.position());
        assertNotNull(telemetry.linearVelocity());
        assertNotNull(telemetry.linearAcceleration());
        assertNotNull(telemetry.angularVelocity());
        assertNotNull(telemetry.orientation());
    }

    @Test
    void returnsMotorOutputSnapshot() {
        Drone drone = new Drone();
        SimulatedDroneHardwareApi api = new SimulatedDroneHardwareApi("drone-test", drone);

        MotorOutputSample outputs = api.readMotorOutputs();

        assertEquals("drone-test", outputs.droneId());
        assertEquals(0.0f, outputs.frontLeft());
        assertEquals(0.0f, outputs.frontRight());
        assertEquals(0.0f, outputs.rearLeft());
        assertEquals(0.0f, outputs.rearRight());
        assertFalse(outputs.motorsArmed());
    }
}
