package com.paperpiper.client;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.paperpiper.ross.FrameData;

/**
 * Thread-safe latest-frame holder for renderer integration.
 */
public class FrameStreamBuffer {

    private final AtomicReference<FrameData> latestFrame = new AtomicReference<>();

    public void acceptFrame(FrameData frame) {
        latestFrame.set(frame);
    }

    public Optional<FrameData> getLatestFrame() {
        return Optional.ofNullable(latestFrame.get());
    }
}
