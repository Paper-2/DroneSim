package com.paperpiper.hardware;

/**
 * Normalized pilot command channels.
 *
 * throttle: [0, 1] pitch/roll/yaw: [-1, 1]
 */
public record ManualControlCommand(float throttle, float pitch, float roll, float yaw) {

    public ManualControlCommand    {
        throttle = clamp(throttle, 0f, 1f);
        pitch = clamp(pitch, -1f, 1f);
        roll = clamp(roll, -1f, 1f);
        yaw = clamp(yaw, -1f, 1f);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
