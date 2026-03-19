package com.paperpiper.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.paperpiper.hardware.DroneHardwareApi;
import com.paperpiper.hardware.SimulationHardwareApi;
import com.paperpiper.ross.FrameData;
import com.paperpiper.ross.RossCodec;

/**
 * TCP control/telemetry + UDP frame protocol server.
 */
public class RossSimulationServer {

    private static final Logger logger = LoggerFactory.getLogger(RossSimulationServer.class);

    private static final int DEFAULT_FRAME_WIDTH = 128;
    private static final int DEFAULT_FRAME_HEIGHT = 72;

    private final SimulationHardwareApi hardwareApi;
    private final int tcpPort;
    private final int telemetryRateHz;
    private final int frameRateHz;
    private final ServerCamera camera;

    private final CopyOnWriteArrayList<ClientSession> clients = new CopyOnWriteArrayList<>();
    private final AtomicLong frameTick = new AtomicLong();

    private volatile boolean running;
    private ServerSocket tcpServer;
    private DatagramSocket udpSocket;
    private Thread acceptThread;
    private ScheduledExecutorService scheduler;

    public RossSimulationServer(SimulationHardwareApi hardwareApi, int tcpPort, int telemetryRateHz, int frameRateHz) {
        this(hardwareApi, tcpPort, telemetryRateHz, frameRateHz, new ServerCamera(DEFAULT_FRAME_WIDTH, DEFAULT_FRAME_HEIGHT));
    }

    public RossSimulationServer(SimulationHardwareApi hardwareApi, int tcpPort, int telemetryRateHz, int frameRateHz, ServerCamera camera) {
        this.hardwareApi = hardwareApi;
        this.tcpPort = tcpPort;
        this.telemetryRateHz = telemetryRateHz;
        this.frameRateHz = frameRateHz;
        this.camera = camera;
    }

    public synchronized void start() throws IOException {
        if (running) {
            return;
        }

        tcpServer = new ServerSocket(tcpPort);
        udpSocket = new DatagramSocket();
        running = true;

        acceptThread = new Thread(this::acceptLoop, "ross-server-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();

        scheduler = Executors.newScheduledThreadPool(2);
        scheduler.scheduleAtFixedRate(this::broadcastTelemetrySafely, 0, Math.max(1, 1000 / telemetryRateHz), TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(this::broadcastFramesSafely, 0, Math.max(1, 1000 / frameRateHz), TimeUnit.MILLISECONDS);

        logger.info("ROSS simulation server started on TCP {}", tcpPort);
    }

    public synchronized void stop() {
        running = false;

        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }

        for (ClientSession client : clients) {
            client.close();
        }
        clients.clear();

        if (tcpServer != null) {
            try {
                tcpServer.close();
            } catch (IOException ex) {
                logger.debug("Error closing TCP server", ex);
            }
            tcpServer = null;
        }

        if (udpSocket != null) {
            udpSocket.close();
            udpSocket = null;
        }

        logger.info("ROSS simulation server stopped");
    }

    public boolean isRunning() {
        return running;
    }

    public ServerCamera getCamera() {
        return camera;
    }

    private void acceptLoop() {
        while (running && tcpServer != null && !tcpServer.isClosed()) {
            try {
                Socket socket = tcpServer.accept();
                ClientSession session = new ClientSession(socket);
                clients.add(session);

                Thread clientThread = new Thread(() -> clientReadLoop(session), "ross-client-session");
                clientThread.setDaemon(true);
                clientThread.start();
            } catch (IOException ex) {
                if (running) {
                    logger.warn("Accept loop stopped unexpectedly", ex);
                }
                break;
            }
        }
    }

    private void clientReadLoop(ClientSession session) {
        try {
            String line;
            while (running && session.isOpen() && (line = session.reader.readLine()) != null) {
                handleCommand(session, line);
            }
        } catch (IOException ex) {
            if (running) {
                logger.debug("Client read loop ended", ex);
            }
        } finally {
            clients.remove(session);
            session.close();
        }
    }

    private void handleCommand(ClientSession session, String command) {
        String[] parts = command.split("\\|", -1);
        if (parts.length == 0) {
            return;
        }

        switch (parts[0]) {
            case "SUBSCRIBE" -> {
                if (parts.length >= 2) {
                    String droneId = parts[1];
                    if (hardwareApi.getDrone(droneId).isPresent()) {
                        session.subscribedDroneId = droneId;
                        session.send("OK|SUBSCRIBED|" + droneId);
                    } else {
                        session.send("ERROR|UNKNOWN_DRONE|" + droneId);
                    }
                }
            }
            case "UNSUBSCRIBE" -> {
                if (parts.length >= 2) {
                    String droneId = parts[1];
                    if (droneId.equals(session.subscribedDroneId)) {
                        session.subscribedDroneId = null;
                    }
                    session.send("OK|UNSUBSCRIBED|" + droneId);
                }
            }
            case "FRAME_UDP_PORT" -> {
                if (parts.length >= 2) {
                    try {
                        int port = Integer.parseInt(parts[1]);
                        session.udpPort = port;
                        session.send("OK|FRAME_UDP_PORT|" + port);
                    } catch (NumberFormatException ignored) {
                        session.send("ERROR|INVALID_UDP_PORT");
                    }
                }
            }
            case "REQUEST_FRAME" -> {
                if (parts.length >= 2) {
                    String droneId = parts[1];
                    if (parts.length >= 4 && "UDP".equals(parts[2])) {
                        try {
                            session.udpPort = Integer.parseInt(parts[3]);
                        } catch (NumberFormatException ignored) {
                            session.send("ERROR|INVALID_UDP_PORT");
                            return;
                        }
                    }
                    sendFrameDatagram(session, droneId);
                }
            }
            case "CAMERA_SET" -> {
                if (parts.length >= 7) {
                    try {
                        float posX = Float.parseFloat(parts[1]);
                        float posY = Float.parseFloat(parts[2]);
                        float posZ = Float.parseFloat(parts[3]);
                        float yaw = Float.parseFloat(parts[4]);
                        float pitch = Float.parseFloat(parts[5]);
                        float roll = Float.parseFloat(parts[6]);
                        camera.setPose(posX, posY, posZ, yaw, pitch, roll);
                        session.send("OK|CAMERA_SET");
                    } catch (NumberFormatException ignored) {
                        session.send("ERROR|INVALID_CAMERA_SET");
                    }
                }
            }
            case "CAMERA_RESOLUTION" -> {
                if (parts.length >= 3) {
                    try {
                        int width = Integer.parseInt(parts[1]);
                        int height = Integer.parseInt(parts[2]);
                        camera.setResolution(width, height);
                        session.send("OK|CAMERA_RESOLUTION|" + width + "|" + height);
                    } catch (NumberFormatException ignored) {
                        session.send("ERROR|INVALID_CAMERA_RESOLUTION");
                    }
                }
            }
            case "MANUAL_CONTROL" -> {
                if (parts.length >= 5 && session.subscribedDroneId != null) {
                    try {
                        float throttle = Float.parseFloat(parts[1]);
                        float cmdPitch = Float.parseFloat(parts[2]);
                        float cmdRoll = Float.parseFloat(parts[3]);
                        float cmdYaw = Float.parseFloat(parts[4]);
                        hardwareApi.getDrone(session.subscribedDroneId).ifPresent(drone -> {
                            drone.applyManualControl(
                                    new com.paperpiper.hardware.ManualControlCommand(throttle, cmdPitch, cmdRoll, cmdYaw));
                        });
                    } catch (NumberFormatException ignored) {
                        session.send("ERROR|INVALID_MANUAL_CONTROL");
                    }
                }
            }
            case "ARM" -> {
                if (session.subscribedDroneId != null) {
                    boolean armed = parts.length >= 2 && "true".equalsIgnoreCase(parts[1]);
                    hardwareApi.getDrone(session.subscribedDroneId).ifPresent(drone -> drone.setArmed(armed));
                    session.send("OK|ARM|" + armed);
                }
            }
            default ->
                session.send("ERROR|UNKNOWN_COMMAND|" + parts[0]);
        }
    }

    private void broadcastTelemetrySafely() {
        try {
            broadcastTelemetry();
        } catch (Exception ex) {
            logger.warn("Telemetry broadcast failed", ex);
        }
    }

    private void broadcastFramesSafely() {
        try {
            List<String> droneIds = hardwareApi.listDroneIds();
            for (ClientSession session : clients) {
                if (!session.isOpen() || session.udpPort <= 0) {
                    continue;
                }

                String droneId = session.subscribedDroneId;
                if (droneId == null && !droneIds.isEmpty()) {
                    droneId = droneIds.get(0);
                }

                if (droneId != null) {
                    sendFrameDatagram(session, droneId);
                }
            }
        } catch (Exception ex) {
            logger.warn("Frame broadcast failed", ex);
        }
    }

    private void broadcastTelemetry() {
        for (ClientSession session : clients) {
            if (!session.isOpen() || session.subscribedDroneId == null) {
                continue;
            }

            hardwareApi.getDrone(session.subscribedDroneId).ifPresent(droneApi -> {
                String telemetryMessage = RossCodec.encodeTelemetry(droneApi.readTelemetry());
                session.send(telemetryMessage);
            });
        }
    }

    private void sendFrameDatagram(ClientSession session, String droneId) {
        if (udpSocket == null || session.udpPort <= 0) {
            return;
        }

        long tick = frameTick.incrementAndGet();
        var telemetry = hardwareApi.getDrone(droneId).map(DroneHardwareApi::readTelemetry).orElse(null);
        FrameData frame = camera.capture(droneId, telemetry, tick);
        String message = RossCodec.encodeFrame(frame);

        byte[] datagramData = RossCodec.utf8(message);
        DatagramPacket packet = new DatagramPacket(datagramData, datagramData.length, session.remoteAddress, session.udpPort);

        try {
            udpSocket.send(packet);
        } catch (IOException ex) {
            logger.debug("Failed to send UDP frame datagram", ex);
        }
    }

    private static final class ClientSession {

        private final Socket socket;
        private final InetAddress remoteAddress;
        private final BufferedReader reader;
        private final PrintWriter writer;

        private volatile String subscribedDroneId;
        private volatile int udpPort;

        private ClientSession(Socket socket) throws IOException {
            this.socket = socket;
            this.remoteAddress = socket.getInetAddress();
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
        }

        private boolean isOpen() {
            return !socket.isClosed();
        }

        private void send(String message) {
            writer.println(message);
        }

        private void close() {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
