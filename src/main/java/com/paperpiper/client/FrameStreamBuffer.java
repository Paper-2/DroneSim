package com.paperpiper.client;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe latest-frame holder for renderer integration.
 */
public class FrameStreamBuffer {

    private final AtomicReference<SimulationFrame> latestFrame = new AtomicReference<>();

    public void acceptFrame(SimulationFrame frame) {
        latestFrame.set(frame);
    }

    public Optional<SimulationFrame> getLatestFrame() {
        return Optional.ofNullable(latestFrame.get());
    }
}
