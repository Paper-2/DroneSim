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
}
