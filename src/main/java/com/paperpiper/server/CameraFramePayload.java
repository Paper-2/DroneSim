package com.paperpiper.server;

/**
 * Frame data produced by the server camera.
 */
public record CameraFramePayload(
        String droneId,
        int width,
        int height,
        String pixelFormat,
        byte[] payload,
        long timestampMillis
        ) {

}
