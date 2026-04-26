package com.paperpiper.ui.panels;

import com.paperpiper.drone.Drone;

import imgui.ImGui;
import imgui.flag.ImGuiCol;

public class InspectorPanel {

    private float battery = 100.0f;
    private int lastDroneHash = 0;

    public void render(Drone drone) {
        if (drone == null) {
            ImGui.textDisabled("No drone selected");
            return;
        }

        int hash = System.identityHashCode(drone);
        if (hash != lastDroneHash) {
            battery = 100.0f;
            lastDroneHash = hash;
        }

        var pos = drone.getPosition();
        var vel = drone.getVelocity();

        ImGui.text(String.format("Position: %.1f, %.1f, %.1f", pos.x, pos.y, pos.z));

        ImGui.text(String.format("Velocity: %.2f m/s", vel.length()));
        ImGui.text(String.format("  (%.1f, %.1f, %.1f)", vel.x, vel.y, vel.z));

        ImGui.spacing();

        if (drone.isMotorsArmed()) {

            // temporary battery drain based on motor power (throttle) - for demo purposes only
            float drain = (drone.FL + drone.FR + drone.RL + drone.RR) * 0.002f;
            battery = Math.max(0, battery - drain);
        }

        int batColor;
        if (battery > 50) {
            batColor = ImGui.colorConvertFloat4ToU32(0.2f, 1.0f, 0.2f, 1.0f);
        } else if (battery > 20) {
            batColor = ImGui.colorConvertFloat4ToU32(1.0f, 1.0f, 0.2f, 1.0f);
        } else {
            batColor = ImGui.colorConvertFloat4ToU32(1.0f, 0.2f, 0.2f, 1.0f);
        }
        ImGui.pushStyleColor(ImGuiCol.Text, batColor);
        ImGui.text(String.format("⚡ Battery: %.0f%%", battery));
        ImGui.popStyleColor();

        ImGui.spacing();

        if (drone.isMotorsArmed()) {
            ImGui.pushStyleColor(ImGuiCol.Text, 0.2f, 1.0f, 0.2f, 1.0f);
            ImGui.text("⌘ ARMED");
            ImGui.popStyleColor();
        } else {
            ImGui.pushStyleColor(ImGuiCol.Text, 1.0f, 0.2f, 0.2f, 1.0f);
            ImGui.text("⌀ DISARMED");
            ImGui.popStyleColor();
        }

        ImGui.text(String.format("FL:%.2f FR:%.2f", drone.FL, drone.FR));
        ImGui.text(String.format("RL:%.2f RR:%.2f", drone.RL, drone.RR));
    }
}
