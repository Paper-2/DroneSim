package com.paperpiper.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;

import com.paperpiper.ross.FrameData;

class ServerCameraTest {

    @Test
    void capturesFrameWithConfiguredResolution() {
        ServerCamera camera = new ServerCamera(64, 32);

        FrameData frame = camera.capture("drone-1", null, 1);

        assertEquals(64, frame.width());
        assertEquals(32, frame.height());
        assertEquals("RGBA8", frame.pixelFormat());
        assertEquals(64 * 32 * 4, frame.payload().length);
    }

    @Test
    void cameraPoseChangesPixelContent() {
        ServerCamera camera = new ServerCamera(16, 16);

        FrameData frameA = camera.capture("drone-1", null, 2);
        camera.setPose(10f, 20f, 30f, 45f, 10f, -5f);
        FrameData frameB = camera.capture("drone-1", null, 2);

        assertNotEquals(frameA.payload()[0], frameB.payload()[0]);
    }
}
