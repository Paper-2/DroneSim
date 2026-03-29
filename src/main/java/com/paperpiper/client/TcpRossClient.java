package com.paperpiper.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Line-based TCP implementation for ROSS client transport.
 */
public class TcpRossClient implements RossClient {

    private static final Logger logger = LoggerFactory.getLogger(TcpRossClient.class);

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private volatile boolean connected;
    private Thread readThread;
    private Consumer<String> messageListener = message -> {
    };

    @Override
    public synchronized void connect(String host, int port) throws IOException {
        if (connected) {
            return;
        }

        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        connected = true;

        readThread = new Thread(this::readLoop, "ross-client-reader");
        readThread.setDaemon(true);
        readThread.start();

        logger.info("Connected to ROSS server {}:{}", host, port);
    }

    @Override
    public synchronized void disconnect() {
        connected = false;

        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ex) {
                logger.debug("Socket close failed", ex);
            }
        }

        socket = null;
        reader = null;
        writer = null;

        logger.info("Disconnected from ROSS server");
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public synchronized void send(String message) throws IOException {
        if (!connected || writer == null) {
            throw new IOException("ROSS client is not connected");
        }

        writer.println(message);
        if (writer.checkError()) {
            throw new IOException("Failed to send message to ROSS server");
        }
    }

    @Override
    public void setMessageListener(Consumer<String> listener) {
        this.messageListener = listener != null ? listener : message -> {
        };
    }

    private void readLoop() {
        try {
            String line;
            while (connected && reader != null && (line = reader.readLine()) != null) {
                messageListener.accept(line);
            }
        } catch (IOException ex) {
            if (connected) {
                logger.warn("ROSS client read loop stopped unexpectedly", ex);
            }
        } finally {
            disconnect();
        }
    }
}
