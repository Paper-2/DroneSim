package com.paperpiper.server;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.paperpiper.hardware.DroneTelemetrySample;
import com.paperpiper.hardware.HardwareVector3;

/**
 * Formats server-side protocol payloads for TCP and UDP channels.
 */
public final class RossMessageFormatter {

    private RossMessageFormatter() {
    }

    public static String telemetry(DroneTelemetrySample telemetry) {
        HardwareVector3 p = telemetry.position();
        HardwareVector3 v = telemetry.linearVelocity();
        return "TELEMETRY|" + telemetry.droneId()
                + "|" + p.x() + "|" + p.y() + "|" + p.z()
                + "|" + v.x() + "|" + v.y() + "|" + v.z()
                + "|" + telemetry.timestampMillis();
    }

    public static String frame(String droneId, int width, int height, String pixelFormat, byte[] payload, long timestampMillis) {
        String encoded = Base64.getEncoder().encodeToString(payload);
        return "FRAME|" + droneId
                + "|" + width + "|" + height
                + "|" + pixelFormat
                + "|" + encoded
                + "|" + timestampMillis;
    }

    public static byte[] utf8(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
