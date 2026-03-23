package com.paperpiper.ui.data;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.paperpiper.drone.Drone;
import com.paperpiper.simulation.SimulationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Records simulation telemetry data to CSV files for analysis.
 */
public class DataExporter {

    private static final Logger logger = LoggerFactory.getLogger(DataExporter.class);
    private static final Path LOG_DIR = Paths.get("logs");

    private BufferedWriter writer;
    private boolean recording = false;
    private String currentFile;

    public void startRecording() {
        try {
            Files.createDirectories(LOG_DIR);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            currentFile = "telemetry_" + timestamp + ".csv";
            Path filePath = LOG_DIR.resolve(currentFile);

            writer = Files.newBufferedWriter(filePath);
            writer.write("time,drone_id,x,y,z,vx,vy,vz,speed,throttle,armed,fl,fr,rl,rr");
            writer.newLine();
            recording = true;
            logger.info("Recording started: {}", filePath);
        } catch (IOException e) {
            logger.error("Failed to start recording", e);
        }
    }

    public void recordFrame(SimulationEngine simulation) {
        if (!recording || writer == null) return;

        try {
            float simTime = simulation.getSimulationTime();
            List<Drone> drones = simulation.getDrones();

            for (int i = 0; i < drones.size(); i++) {
                Drone drone = drones.get(i);
                var pos = drone.getPosition();
                var vel = drone.getVelocity();
                writer.write(String.format("%.3f,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%s,%.4f,%.4f,%.4f,%.4f",
                        simTime, i,
                        pos.x, pos.y, pos.z,
                        vel.x, vel.y, vel.z,
                        vel.length(),
                        drone.getThrottle(),
                        drone.isMotorsArmed() ? "1" : "0",
                        drone.FL, drone.FR, drone.RL, drone.RR));
                writer.newLine();
            }
            writer.flush();
        } catch (IOException e) {
            logger.error("Error writing telemetry data", e);
            stopRecording();
        }
    }

    public void stopRecording() {
        if (writer != null) {
            try {
                writer.close();
                logger.info("Recording stopped: {}", currentFile);
            } catch (IOException e) {
                logger.error("Error closing recording file", e);
            }
            writer = null;
        }
        recording = false;
    }

    public boolean isRecording() { return recording; }
    public String getCurrentFile() { return currentFile; }

    public void cleanup() {
        stopRecording();
    }
}
