package com.paperpiper.server;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.paperpiper.hardware.DroneHardwareApi;
import com.paperpiper.hardware.SimulationHardwareApi;

/**
 * In-memory hardware API provider used for protocol/server development and
 * integration testing.
 */
public class SyntheticSimulationHardwareApi implements SimulationHardwareApi {

    private final Map<String, DroneHardwareApi> drones = new LinkedHashMap<>();

    public SyntheticSimulationHardwareApi(int droneCount) {
        for (int i = 1; i <= droneCount; i++) {
            String droneId = "drone-" + i;
            drones.put(droneId, new SyntheticDroneHardwareApi(droneId));
        }
    }

    @Override
    public List<String> listDroneIds() {
        return List.copyOf(drones.keySet());
    }

    @Override
    public Optional<DroneHardwareApi> getDrone(String droneId) {
        return Optional.ofNullable(drones.get(droneId));
    }

    @Override
    public Optional<DroneHardwareApi> getActiveDrone() {
        return drones.values().stream().findFirst();
    }
}
