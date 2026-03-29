package com.paperpiper.client;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Receives frame messages over datagram transport (UDP).
 */
public interface FrameDatagramReceiver {

    void start(int localPort, Consumer<String> messageListener) throws IOException;

    void stop();

    boolean isRunning();
}
