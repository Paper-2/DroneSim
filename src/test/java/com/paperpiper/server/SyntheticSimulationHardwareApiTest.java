package com.paperpiper.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SyntheticSimulationHardwareApiTest {

    @Test
    void exposesConfiguredDroneCount() {
        SyntheticSimulationHardwareApi api = new SyntheticSimulationHardwareApi(2);

        assertEquals(2, api.listDroneIds().size());
        assertTrue(api.getDrone("drone-1").isPresent());
        assertTrue(api.getDrone("drone-2").isPresent());
    }
}
