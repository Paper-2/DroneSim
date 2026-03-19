package com.paperpiper.ross;

/**
 * Wire-format frame payload shared between client and server.
 */
public record FrameData(
        String droneId,
        int width,
        int height,
        String pixelFormat,
        byte[] payload,
        long timestampMillis) {

}
