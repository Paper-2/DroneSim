package com.paperpiper.hardware;

import java.util.List;
import java.util.Optional;

/**
 * Access point for simulated hardware endpoints exposed by the simulator.
 */
public interface SimulationHardwareApi {

    List<String> listDroneIds();

    Optional<DroneHardwareApi> getDrone(String droneId);

    Optional<DroneHardwareApi> getActiveDrone();

    /**
     * Spawn a new drone with the given ID. Returns the API handle for the newly
     * created drone, or the existing one if a drone with that ID already
     * exists.
     */
    DroneHardwareApi spawnDrone(String droneId);
}
