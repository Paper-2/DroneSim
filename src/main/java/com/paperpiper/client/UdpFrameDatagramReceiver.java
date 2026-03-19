package com.paperpiper.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UDP receiver for line-based frame datagrams.
 */
public class UdpFrameDatagramReceiver implements FrameDatagramReceiver {

    private static final Logger logger = LoggerFactory.getLogger(UdpFrameDatagramReceiver.class);
    private static final int MAX_PACKET_SIZE = 65507;

    private DatagramSocket socket;
    private Thread receiveThread;
    private volatile boolean running;

    @Override
    public synchronized void start(int localPort, Consumer<String> messageListener) throws IOException {
        if (running) {
            return;
        }

        socket = new DatagramSocket(localPort);
        running = true;

        receiveThread = new Thread(() -> receiveLoop(messageListener), "udp-frame-receiver");
        receiveThread.setDaemon(true);
        receiveThread.start();

        logger.info("Started UDP frame receiver on port {}", localPort);
    }

    @Override
    public synchronized void stop() {
        running = false;
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void receiveLoop(Consumer<String> messageListener) {
        byte[] buffer = new byte[MAX_PACKET_SIZE];

        while (running && socket != null && !socket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String message = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
                messageListener.accept(message);
            } catch (IOException ex) {
                if (running) {
                    logger.warn("UDP frame receiver stopped unexpectedly", ex);
                }
                break;
            }
        }

        stop();
    }
}
