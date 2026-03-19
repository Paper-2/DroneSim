package com.paperpiper.ross;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import com.paperpiper.hardware.DroneTelemetrySample;
import com.paperpiper.hardware.HardwareVector3;

/**
 * Unified ROSS protocol codec — encodes server-side types to wire format and
 * decodes wire-format strings back into shared DTOs.
 *
 * <p>
 * Wire formats:
 * <pre>
 * TELEMETRY|droneId|posX|posY|posZ|velX|velY|velZ|timestampMillis
 * FRAME|droneId|width|height|pixelFormat|base64Payload|timestampMillis
 * </pre>
 */
public final class RossCodec {

    private static final String DELIM = "\\|";

    private RossCodec() {
    }

    // ---- Encoding (server side) ----
    public static String encodeTelemetry(DroneTelemetrySample telemetry) {
        HardwareVector3 p = telemetry.position();
        HardwareVector3 v = telemetry.linearVelocity();
        return "TELEMETRY|" + telemetry.droneId()
                + "|" + p.x() + "|" + p.y() + "|" + p.z()
                + "|" + v.x() + "|" + v.y() + "|" + v.z()
                + "|" + telemetry.timestampMillis();
    }

    public static String encodeFrame(FrameData frame) {
        String encoded = Base64.getEncoder().encodeToString(frame.payload());
        return "FRAME|" + frame.droneId()
                + "|" + frame.width() + "|" + frame.height()
                + "|" + frame.pixelFormat()
                + "|" + encoded
                + "|" + frame.timestampMillis();
    }

    public static String encodeFrame(String droneId, int width, int height,
            String pixelFormat, byte[] payload, long timestampMillis) {
        return encodeFrame(new FrameData(droneId, width, height, pixelFormat, payload, timestampMillis));
    }

    // ---- Decoding (client side) ----
    public static Optional<TelemetryData> decodeTelemetry(String message) {
        String[] parts = message.split(DELIM, -1);
        if (parts.length != 9 || !"TELEMETRY".equals(parts[0])) {
            return Optional.empty();
        }

        try {
            return Optional.of(new TelemetryData(
                    parts[1],
                    Float.parseFloat(parts[2]),
                    Float.parseFloat(parts[3]),
                    Float.parseFloat(parts[4]),
                    Float.parseFloat(parts[5]),
                    Float.parseFloat(parts[6]),
                    Float.parseFloat(parts[7]),
                    Long.parseLong(parts[8])));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public static Optional<FrameData> decodeFrame(String message) {
        String[] parts = message.split(DELIM, -1);
        if (parts.length != 7 || !"FRAME".equals(parts[0])) {
            return Optional.empty();
        }

        try {
            byte[] payload = Base64.getDecoder().decode(parts[5]);
            return Optional.of(new FrameData(
                    parts[1],
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]),
                    parts[4],
                    payload,
                    Long.parseLong(parts[6])));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    // ---- Utility ----
    public static byte[] utf8(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
