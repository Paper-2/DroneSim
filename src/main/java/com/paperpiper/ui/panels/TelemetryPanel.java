package com.paperpiper.ui.panels;

import com.paperpiper.drone.Drone;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;

/**
 * Per-drone telemetry display: position, velocity, altitude, control inputs,
 * motor state.
 */
public class TelemetryPanel {

    private boolean visible = false;

    private float minAltitude = Float.MAX_VALUE;
    private float maxAltitude = -Float.MAX_VALUE;
    private int lastDroneHash = 0;

    public void render(Drone drone) {
        if (!visible || drone == null) {
            return;
        }

        ImGui.setNextWindowSize(280, 380, ImGuiCond.FirstUseEver);
        if (ImGui.begin("Telemetry")) {

            var pos = drone.getPosition();
            var vel = drone.getVelocity();
            float speed = vel.length();
            float altitude = pos.y;

            // Reset min/max when active drone changes
            int hash = System.identityHashCode(drone);
            if (hash != lastDroneHash) {
                minAltitude = altitude;
                maxAltitude = altitude;
                lastDroneHash = hash;
            }
            minAltitude = Math.min(minAltitude, altitude);
            maxAltitude = Math.max(maxAltitude, altitude);

            // Position
            ImGui.separator();
            ImGui.text("Position");
            ImGui.text(String.format("X: %8.2f m", pos.x));
            ImGui.text(String.format("Y: %8.2f m", pos.y));
            ImGui.text(String.format("Z: %8.2f m", pos.z));

            // Altitude with color coding
            ImGui.separator();
            ImGui.text("Altitude");
            int altColor;
            if (altitude > 5.0f) {
                altColor = ImGui.colorConvertFloat4ToU32(0.2f, 1.0f, 0.2f, 1.0f); // green
            } else if (altitude > 1.0f) {
                altColor = ImGui.colorConvertFloat4ToU32(1.0f, 1.0f, 0.2f, 1.0f); // yellow
            } else {
                altColor = ImGui.colorConvertFloat4ToU32(1.0f, 0.2f, 0.2f, 1.0f); // red
            }
            ImGui.pushStyleColor(ImGuiCol.Text, altColor);
            ImGui.text(String.format("ALT: %.2f m", altitude));
            ImGui.popStyleColor();
            ImGui.text(String.format("Min: %.2f  Max: %.2f", minAltitude, maxAltitude));

            // Velocity
            ImGui.separator();
            ImGui.text("Velocity");
            ImGui.text(String.format("Vx: %7.2f m/s", vel.x));
            ImGui.text(String.format("Vy: %7.2f m/s", vel.y));
            ImGui.text(String.format("Vz: %7.2f m/s", vel.z));
            ImGui.text(String.format("Speed: %.2f m/s", speed));

            // Control inputs
            ImGui.separator();
            ImGui.text("Controls");
            ImGui.text("Throttle:");
            ImGui.sameLine();
            ImGui.progressBar(drone.getThrottle(), 150, 18, String.format("%.0f%%", drone.getThrottle() * 100));

            renderCenteredBar("Pitch", drone.getPitch());
            renderCenteredBar("Roll ", drone.getRoll());
            renderCenteredBar("Yaw  ", drone.getYaw());

            // Motor state
            ImGui.separator();
            ImGui.text("Motors");
            if (drone.isMotorsArmed()) {
                ImGui.pushStyleColor(ImGuiCol.Text, 0.2f, 1.0f, 0.2f, 1.0f);
                ImGui.text("ARMED");
                ImGui.popStyleColor();
            } else {
                ImGui.pushStyleColor(ImGuiCol.Text, 1.0f, 0.2f, 0.2f, 1.0f);
                ImGui.text("DISARMED");
                ImGui.popStyleColor();
            }
            ImGui.text(String.format("FL: %.2f  FR: %.2f", drone.FL, drone.FR));
            ImGui.text(String.format("RL: %.2f  RR: %.2f", drone.RL, drone.RR));
        }
        ImGui.end();
    }

    private void renderCenteredBar(String label, float value) {
        ImGui.text(label + ":");
        ImGui.sameLine();
        // Map -1..1 to 0..1 for the progress bar
        float normalized = (value + 1.0f) * 0.5f;
        ImGui.progressBar(normalized, 150, 18, String.format("%+.2f", value));
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
