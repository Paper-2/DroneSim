package com.paperpiper.client;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Transport abstraction for a ROSS-compatible client.
 */
public interface RossClient {

    void connect(String host, int port) throws IOException;

    void disconnect();

    boolean isConnected();

    void send(String message) throws IOException;

    void setMessageListener(Consumer<String> listener);
}
