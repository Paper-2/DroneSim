package com.paperpiper.client;

/**
 * Frame payload streamed by the simulation server.
 */
public record SimulationFrame(
        String droneId,
        int width,
        int height,
        String pixelFormat,
        byte[] payload,
        long timestampMillis
        ) {

}
