package com.paperpiper.hardware;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.jme3.math.Vector3f;
import com.paperpiper.drone.Drone;
import com.paperpiper.simulation.SimulationEngine;

/**
 * Exposes simulator drones through the hardware API contract.
 */
public class SimulationHardwareApiAdapter implements SimulationHardwareApi {

    private final SimulationEngine simulationEngine;
    private final Map<Drone, String> droneIds = new IdentityHashMap<>();
    private final Map<Drone, SimulatedDroneHardwareApi> adapters = new IdentityHashMap<>();
    private int nextDroneId = 1;

    public SimulationHardwareApiAdapter(SimulationEngine simulationEngine) {
        this.simulationEngine = simulationEngine;
    }

    @Override
    public synchronized List<String> listDroneIds() {
        syncDrones();
        return adapters.values().stream()
                .map(SimulatedDroneHardwareApi::getDroneId)
                .sorted()
                .toList();
    }

    @Override
    public synchronized Optional<DroneHardwareApi> getDrone(String droneId) {
        syncDrones();

        return adapters.values().stream()
                .filter(adapter -> adapter.getDroneId().equals(droneId))
                .map(adapter -> (DroneHardwareApi) adapter)
                .findFirst();
    }

    @Override
    public synchronized Optional<DroneHardwareApi> getActiveDrone() {
        syncDrones();

        Drone active = simulationEngine.getActiveDrone();
        if (active == null) {
            return Optional.empty();
        }

        return Optional.of(adapters.get(active));
    }

    private void syncDrones() {
        List<Drone> currentDrones = new ArrayList<>(simulationEngine.getDrones());

        droneIds.keySet().removeIf(drone -> !currentDrones.contains(drone));
        adapters.keySet().removeIf(drone -> !currentDrones.contains(drone));

        for (Drone drone : currentDrones) {
            String droneId = droneIds.computeIfAbsent(drone, ignored -> "drone-" + nextDroneId++);
            adapters.computeIfAbsent(drone, ignored -> new SimulatedDroneHardwareApi(droneId, drone));
        }
    }

    @Override
    public synchronized DroneHardwareApi spawnDrone(String droneId) {
        syncDrones();
        // Return existing if already present
        for (SimulatedDroneHardwareApi api : adapters.values()) {
            if (api.getDroneId().equals(droneId)) {
                return api;
            }
        }
        Drone drone = simulationEngine.addDrone(new Vector3f(0, 2, 0));
        drone.setMotorsArmed(true);
        droneIds.put(drone, droneId);
        SimulatedDroneHardwareApi api = new SimulatedDroneHardwareApi(droneId, drone);
        adapters.put(drone, api);
        return api;
    }
}
