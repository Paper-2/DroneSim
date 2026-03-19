package com.paperpiper.client;

import java.util.Base64;
import java.util.Optional;

/**
 * Parser for line-based ROSS protocol messages.
 *
 * Telemetry format:
 * TELEMETRY|droneId|posX|posY|posZ|velX|velY|velZ|timestampMillis
 *
 * Frame format:
 * FRAME|droneId|width|height|pixelFormat|base64Payload|timestampMillis
 */
public final class RossMessageParser {

    private RossMessageParser() {
    }

    public static Optional<DroneTelemetry> parseTelemetry(String message) {
        String[] parts = message.split("\\|", -1);
        if (parts.length != 9 || !"TELEMETRY".equals(parts[0])) {
            return Optional.empty();
        }

        try {
            return Optional.of(new DroneTelemetry(
                    parts[1],
                    Float.parseFloat(parts[2]),
                    Float.parseFloat(parts[3]),
                    Float.parseFloat(parts[4]),
                    Float.parseFloat(parts[5]),
                    Float.parseFloat(parts[6]),
                    Float.parseFloat(parts[7]),
                    Long.parseLong(parts[8])
            ));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public static Optional<SimulationFrame> parseFrame(String message) {
        String[] parts = message.split("\\|", -1);
        if (parts.length != 7 || !"FRAME".equals(parts[0])) {
            return Optional.empty();
        }

        try {
            byte[] payload = Base64.getDecoder().decode(parts[5]);
            return Optional.of(new SimulationFrame(
                    parts[1],
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]),
                    parts[4],
                    payload,
                    Long.parseLong(parts[6])
            ));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
