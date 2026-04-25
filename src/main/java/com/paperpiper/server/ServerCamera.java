package com.paperpiper.server;

import java.util.concurrent.atomic.AtomicReference;

import com.paperpiper.hardware.DroneTelemetrySample;
import com.paperpiper.hardware.HardwareVector3;
import com.paperpiper.ross.FrameData;

/**
 * Server-side virtual camera used to generate outbound frame streams.
 *
 * <p>
 * Supports two modes:
 * <ul>
 * <li><b>Live mode</b> — call {@link #supplyLiveFrame(byte[], int, int)} from
 * the render thread to feed real framebuffer pixels. Subsequent
 * {@link #capture} calls return those pixels.</li>
 * <li><b>Synthetic mode</b> (default) — if no live frame has been supplied,
 * {@link #capture} generates a procedural colour pattern.</li>
 * </ul>
 */
public class ServerCamera {

    private static final String PIXEL_FORMAT = "GRAY8";

    private volatile int width;
    private volatile int height;

    private volatile float posX;
    private volatile float posY;
    private volatile float posZ;

    private volatile float yawDeg;
    private volatile float pitchDeg;
    private volatile float rollDeg;

    /**
     * Holds the most recent live framebuffer pixels (or null).
     */
    private final AtomicReference<FrameData> liveFrame = new AtomicReference<>();

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

    /**
     * Supply a live framebuffer capture (RGBA pixels, top-to-bottom). Called
     * from the render thread after {@code glReadPixels}.
     */
    public void supplyLiveFrame(byte[] rgbaPixels, int frameWidth, int frameHeight) {
        liveFrame.set(new FrameData("live", frameWidth, frameHeight, PIXEL_FORMAT, rgbaPixels, System.currentTimeMillis()));
    }

    public FrameData capture(String droneId, DroneTelemetrySample telemetry, long frameTick) {
        // If a live frame is available, downscale and convert to grayscale.
        // Downscale uses nearest-neighbour sampling to fit the configured camera
        // resolution, keeping UDP payloads well under the 65 KB datagram limit.
        // Grayscale conversion uses the ITU-R BT.601 luma formula.
        //   Source: ITU-R Recommendation BT.601-7
        //   https://www.itu.int/rec/R-REC-BT.601
        FrameData live = liveFrame.get();
        if (live != null) {
            int targetW = this.width;
            int targetH = this.height;
            byte[] src = live.payload();
            int srcW = live.width();
            int srcH = live.height();

            byte[] gray = new byte[targetW * targetH];
            for (int y = 0; y < targetH; y++) {
                int srcY = y * srcH / targetH;
                for (int x = 0; x < targetW; x++) {
                    int srcX = x * srcW / targetW;
                    int srcIdx = (srcY * srcW + srcX) * 4;
                    int r = src[srcIdx] & 0xFF;
                    int g = src[srcIdx + 1] & 0xFF;
                    int b = src[srcIdx + 2] & 0xFF;
                    // ITU-R BT.601 luma
                    gray[y * targetW + x] = (byte) ((r * 77 + g * 150 + b * 29) >> 8);
                }
            }
            return new FrameData(droneId, targetW, targetH, PIXEL_FORMAT, gray, System.currentTimeMillis());
        }

        // Fallback: synthetic pattern
        return captureSynthetic(droneId, telemetry, frameTick);
    }

    private FrameData captureSynthetic(String droneId, DroneTelemetrySample telemetry, long frameTick) {
        int frameWidth = width;
        int frameHeight = height;

        // Synthetic frames are also grayscale (1 byte per pixel)
        byte[] pixels = new byte[frameWidth * frameHeight];

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
                int r = (x + tick + yawBand) & 0xFF;
                int g = (y + (tick * 2) + pitchBand) & 0xFF;
                int b = (distanceBand + rollBand + x + y) & 0xFF;
                // ITU-R BT.601 luma (same coefficients as live path)
                pixels[y * frameWidth + x] = (byte) ((r * 77 + g * 150 + b * 29) >> 8);
            }
        }

        return new FrameData(
                droneId,
                frameWidth,
                frameHeight,
                PIXEL_FORMAT,
                pixels,
                System.currentTimeMillis()
        );
    }
}
