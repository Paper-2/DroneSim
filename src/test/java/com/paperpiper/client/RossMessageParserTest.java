package com.paperpiper.client;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class RossMessageParserTest {

    @Test
    void parsesTelemetryMessage() {
        String message = "TELEMETRY|drone-1|1.5|2.0|3.25|0.1|0.2|0.3|1710000000000";

        var telemetry = RossMessageParser.parseTelemetry(message);

        assertTrue(telemetry.isPresent());
        assertEquals("drone-1", telemetry.get().droneId());
        assertEquals(1.5f, telemetry.get().posX());
        assertEquals(2.0f, telemetry.get().posY());
        assertEquals(3.25f, telemetry.get().posZ());
        assertEquals(1710000000000L, telemetry.get().timestampMillis());
    }

    @Test
    void parsesFrameMessage() {
        byte[] payload = new byte[]{1, 2, 3, 4};
        String base64 = Base64.getEncoder().encodeToString(payload);
        String message = "FRAME|drone-2|640|480|RGBA8|" + base64 + "|1710000000123";

        var frame = RossMessageParser.parseFrame(message);

        assertTrue(frame.isPresent());
        assertEquals("drone-2", frame.get().droneId());
        assertEquals(640, frame.get().width());
        assertEquals(480, frame.get().height());
        assertEquals("RGBA8", frame.get().pixelFormat());
        assertArrayEquals(payload, frame.get().payload());
        assertEquals(1710000000123L, frame.get().timestampMillis());
    }

    @Test
    void rejectsMalformedMessages() {
        assertTrue(RossMessageParser.parseTelemetry("TELEMETRY|missing|fields").isEmpty());
        assertTrue(RossMessageParser.parseFrame("FRAME|drone|640|480|RGBA8|not-base64|123").isEmpty());
    }
}
