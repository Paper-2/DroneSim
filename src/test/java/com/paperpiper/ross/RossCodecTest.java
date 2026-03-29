package com.paperpiper.ross;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class RossCodecTest {

    @Test
    void decodesTelemetryMessage() {
        String message = "TELEMETRY|drone-1|1.5|2.0|3.25|0.1|0.2|0.3|1710000000000";

        var telemetry = RossCodec.decodeTelemetry(message);

        assertTrue(telemetry.isPresent());
        assertEquals("drone-1", telemetry.get().droneId());
        assertEquals(1.5f, telemetry.get().posX());
        assertEquals(2.0f, telemetry.get().posY());
        assertEquals(3.25f, telemetry.get().posZ());
        assertEquals(1710000000000L, telemetry.get().timestampMillis());
    }

    @Test
    void decodesFrameMessage() {
        byte[] payload = new byte[]{1, 2, 3, 4};
        String base64 = Base64.getEncoder().encodeToString(payload);
        String message = "FRAME|drone-2|640|480|RGBA8|" + base64 + "|1710000000123";

        var frame = RossCodec.decodeFrame(message);

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
        assertTrue(RossCodec.decodeTelemetry("TELEMETRY|missing|fields").isEmpty());
        assertTrue(RossCodec.decodeFrame("FRAME|drone|640|480|RGBA8|not-base64|123").isEmpty());
    }

    @Test
    void encodeFrameRoundTrips() {
        byte[] pixels = new byte[]{10, 20, 30, 40};
        FrameData original = new FrameData("drone-3", 2, 1, "RGBA8", pixels, 999L);

        String encoded = RossCodec.encodeFrame(original);
        var decoded = RossCodec.decodeFrame(encoded);

        assertTrue(decoded.isPresent());
        assertEquals("drone-3", decoded.get().droneId());
        assertEquals(2, decoded.get().width());
        assertEquals(1, decoded.get().height());
        assertArrayEquals(pixels, decoded.get().payload());
        assertEquals(999L, decoded.get().timestampMillis());
    }
}
