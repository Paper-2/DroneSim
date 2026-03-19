package com.paperpiper.server;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.paperpiper.drone.Drone;
import com.paperpiper.hardware.DroneHardwareApi;
import com.paperpiper.hardware.SimulationHardwareApi;
import com.paperpiper.simulation.SimulationEngine;

/**
 * Adapts a live {@link SimulationEngine} to the {@link SimulationHardwareApi}
 * interface used by the ROSS server.
 */
public class LiveSimulationHardwareApi implements SimulationHardwareApi {

    private final SimulationEngine simulation;
    private final Map<Drone, LiveDroneHardwareApi> adapters = new LinkedHashMap<>();

    public LiveSimulationHardwareApi(SimulationEngine simulation) {
        this.simulation = simulation;
        refreshDrones();
    }

    /**
     * Re-scan the simulation for drones and update the adapter map.
     */
    public void refreshDrones() {
        adapters.clear();
        int index = 1;
        for (Drone drone : simulation.getDrones()) {
            String droneId = "drone-" + index++;
            adapters.put(drone, new LiveDroneHardwareApi(droneId, drone));
        }
    }

    @Override
    public List<String> listDroneIds() {
        return adapters.values().stream()
                .map(DroneHardwareApi::getDroneId)
                .toList();
    }

    @Override
    public Optional<DroneHardwareApi> getDrone(String droneId) {
        return adapters.values().stream()
                .filter(api -> api.getDroneId().equals(droneId))
                .map(DroneHardwareApi.class::cast)
                .findFirst();
    }

    @Override
    public Optional<DroneHardwareApi> getActiveDrone() {
        Drone active = simulation.getActiveDrone();
        if (active == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(adapters.get(active));
    }
}
