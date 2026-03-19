package com.paperpiper.server;

import com.paperpiper.hardware.DroneTelemetrySample;
import com.paperpiper.hardware.HardwareVector3;

/**
 * Server-side virtual camera used to generate outbound frame streams.
 */
public class ServerCamera {

    private static final String PIXEL_FORMAT = "RGBA8";

    private volatile int width;
    private volatile int height;

    private volatile float posX;
    private volatile float posY;
    private volatile float posZ;

    private volatile float yawDeg;
    private volatile float pitchDeg;
    private volatile float rollDeg;

    public ServerCamera(int width, int height) {
        this.width = width;
        this.height = height;
        this.posX = 0f;
        this.posY = 3f;
        this.posZ = -6f;
    }

    public synchronized void setPose(float posX, float posY, float posZ, float yawDeg, float pitchDeg, float rollDeg) {
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.yawDeg = yawDeg;
        this.pitchDeg = pitchDeg;
        this.rollDeg = rollDeg;
    }

    public synchronized void setResolution(int width, int height) {
        this.width = Math.max(16, width);
        this.height = Math.max(16, height);
    }

    public CameraFramePayload capture(String droneId, DroneTelemetrySample telemetry, long frameTick) {
        int frameWidth = width;
        int frameHeight = height;

        byte[] pixels = new byte[frameWidth * frameHeight * 4];

        float subjectX = 0f;
        float subjectY = 0f;
        float subjectZ = 0f;
        if (telemetry != null) {
            HardwareVector3 pos = telemetry.position();
            subjectX = pos.x();
            subjectY = pos.y();
            subjectZ = pos.z();
        }

        float dx = subjectX - posX;
        float dy = subjectY - posY;
        float dz = subjectZ - posZ;
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        int distanceBand = (int) Math.min(255, distance * 20f);

        int yawBand = ((int) (yawDeg * 2f) & 0xFF);
        int pitchBand = ((int) (pitchDeg * 2f) & 0xFF);
        int rollBand = ((int) (rollDeg * 2f) & 0xFF);

        int tick = (int) (frameTick & 0xFF);

        for (int y = 0; y < frameHeight; y++) {
            for (int x = 0; x < frameWidth; x++) {
                int index = (y * frameWidth + x) * 4;
                pixels[index] = (byte) ((x + tick + yawBand) & 0xFF);
                pixels[index + 1] = (byte) ((y + (tick * 2) + pitchBand) & 0xFF);
                pixels[index + 2] = (byte) ((distanceBand + rollBand + x + y) & 0xFF);
                pixels[index + 3] = (byte) 0xFF;
            }
        }

        return new CameraFramePayload(
                droneId,
                frameWidth,
                frameHeight,
                PIXEL_FORMAT,
                pixels,
                System.currentTimeMillis()
        );
    }
}
