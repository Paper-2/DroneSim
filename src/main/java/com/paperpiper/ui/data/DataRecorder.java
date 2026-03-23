package com.paperpiper.ui.data;

/**
 * Circular buffer that stores the last N float values for real-time graph rendering.
 */
public class DataRecorder {

    private final float[] buffer;
    private int head = 0;
    private int count = 0;

    public DataRecorder(int capacity) {
        this.buffer = new float[capacity];
    }

    public void record(float value) {
        buffer[head] = value;
        head = (head + 1) % buffer.length;
        if (count < buffer.length) count++;
    }

    /** Returns the buffer array for ImGui.plotLines(). Values are in insertion order. */
    public float[] getOrderedData() {
        float[] result = new float[count];
        for (int i = 0; i < count; i++) {
            int idx = (head - count + i + buffer.length) % buffer.length;
            result[i] = buffer[idx];
        }
        return result;
    }

    public float getLatest() {
        if (count == 0) return 0;
        return buffer[(head - 1 + buffer.length) % buffer.length];
    }

    public int size() { return count; }

    public void clear() {
        head = 0;
        count = 0;
    }
}
