package com.paperpiper.hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class ManualControlCommandTest {

    @Test
    void clampsChannelsToExpectedRanges() {
        ManualControlCommand command = new ManualControlCommand(2.0f, -3.0f, 5.0f, -9.0f);

        assertEquals(1.0f, command.throttle());
        assertEquals(-1.0f, command.pitch());
        assertEquals(1.0f, command.roll());
        assertEquals(-1.0f, command.yaw());
    }
}
