package com.paperpiper.server;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class RossSimulationServerTest {

    @Test
    void servesTelemetryAfterSubscription() throws Exception {
        RossSimulationServer server = new RossSimulationServer(new SyntheticSimulationHardwareApi(1), 5601, 20, 5);

        try {
            server.start();

            try (Socket socket = new Socket("127.0.0.1", 5601);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8)) {

                writer.println("SUBSCRIBE|drone-1");

                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
                boolean gotTelemetry = false;

                while (System.nanoTime() < deadline) {
                    if (reader.ready()) {
                        String line = reader.readLine();
                        if (line != null && line.startsWith("TELEMETRY|drone-1|")) {
                            gotTelemetry = true;
                            break;
                        }
                    } else {
                        Thread.sleep(20L);
                    }
                }

                assertTrue(gotTelemetry);
            }
        } finally {
            server.stop();
        }
    }

    @Test
    void appliesServerCameraResolutionForUdpFrames() throws Exception {
        RossSimulationServer server = new RossSimulationServer(new SyntheticSimulationHardwareApi(1), 5602, 20, 5);

        try {
            server.start();

            try (Socket socket = new Socket("127.0.0.1", 5602);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 PrintWriter writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
                 DatagramSocket udpSocket = new DatagramSocket(5603)) {

                udpSocket.setSoTimeout(2000);
                writer.println("SUBSCRIBE|drone-1");
                writer.println("FRAME_UDP_PORT|5603");
                writer.println("CAMERA_RESOLUTION|32|24");
                writer.println("REQUEST_FRAME|drone-1|UDP|5603");

                byte[] buffer = new byte[65507];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);

                String message = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
                assertTrue(message.startsWith("FRAME|drone-1|32|24|RGBA8|"));

                boolean gotAck = false;
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
                while (System.nanoTime() < deadline) {
                    if (reader.ready()) {
                        String line = reader.readLine();
                        if (line != null && line.startsWith("OK|CAMERA_RESOLUTION|32|24")) {
                            gotAck = true;
                            break;
                        }
                    } else {
                        Thread.sleep(20L);
                    }
                }

                assertTrue(gotAck);
            }
        } finally {
            server.stop();
        }
    }
}
