package com.paperpiper.server;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.jme3.math.Vector3f;
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
    private final Map<Drone, LiveDroneHardwareApi> adapters = new ConcurrentHashMap<>();

    /**
     * Pending spawn requests queued by network threads, drained on the GL
     * thread.
     */
    private final ConcurrentLinkedQueue<PendingSpawn> pendingSpawns = new ConcurrentLinkedQueue<>();

    private record PendingSpawn(String droneId, CompletableFuture<DroneHardwareApi> future) {

    }

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

    @Override
    public synchronized DroneHardwareApi spawnDrone(String droneId) {
        // Return existing drone if already spawned
        for (LiveDroneHardwareApi api : adapters.values()) {
            if (api.getDroneId().equals(droneId)) {
                return api;
            }
        }
        // Drone.init() loads OpenGL resources, so it must run on the GL thread.
        // Queue a request and block until the main loop processes it.
        CompletableFuture<DroneHardwareApi> future = new CompletableFuture<>();
        pendingSpawns.add(new PendingSpawn(droneId, future));
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException("Failed to spawn drone on GL thread", e);
        }
    }

    /**
     * Process any pending drone-spawn requests. <b>Must be called from the
     * OpenGL / main thread</b> (e.g. once per frame in the render loop).
     */
    public void processPendingSpawns() {
        PendingSpawn pending;
        while ((pending = pendingSpawns.poll()) != null) {
            // Double-check it wasn't created by another request in the same batch
            String id = pending.droneId();
            DroneHardwareApi existing = null;
            for (LiveDroneHardwareApi api : adapters.values()) {
                if (api.getDroneId().equals(id)) {
                    existing = api;
                    break;
                }
            }
            if (existing != null) {
                pending.future().complete(existing);
                continue;
            }
            Drone drone = simulation.addDrone(new Vector3f(0, 2, 0));
            drone.setMotorsArmed(true);
            LiveDroneHardwareApi api = new LiveDroneHardwareApi(id, drone);
            adapters.put(drone, api);
            pending.future().complete(api);
        }
    }
}
