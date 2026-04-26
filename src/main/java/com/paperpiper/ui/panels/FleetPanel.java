package com.paperpiper.ui.panels;

import java.util.List;

import com.paperpiper.drone.Drone;
import com.paperpiper.simulation.SimulationEngine;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiTableFlags;

/**
 * Fleet overview table showing all drones with sortable columns.
 */
public class FleetPanel {

    private boolean visible = false;

    public void render(SimulationEngine simulation) {
        if (!visible) {
            return;
        }

        ImGui.setNextWindowSize(500, 350, ImGuiCond.FirstUseEver);
        if (ImGui.begin("Fleet Overview")) {

            List<Drone> drones = simulation.getDrones();
            Drone activeDrone = simulation.getActiveDrone();

            int flags = ImGuiTableFlags.Borders | ImGuiTableFlags.RowBg
                    | ImGuiTableFlags.ScrollY | ImGuiTableFlags.Resizable;

            if (ImGui.beginTable("fleet_table", 7, flags)) {
                ImGui.tableSetupScrollFreeze(0, 1);
                ImGui.tableSetupColumn("#");
                ImGui.tableSetupColumn("X");
                ImGui.tableSetupColumn("Y");
                ImGui.tableSetupColumn("Z");
                ImGui.tableSetupColumn("Speed");
                ImGui.tableSetupColumn("Throttle");
                ImGui.tableSetupColumn("Armed");
                ImGui.tableHeadersRow();

                float totalAlt = 0, totalSpeed = 0;
                int armedCount = 0;

                for (int i = 0; i < drones.size(); i++) {
                    Drone drone = drones.get(i);
                    var pos = drone.getPosition();
                    var vel = drone.getVelocity();
                    float speed = vel.length();
                    boolean isActive = (drone == activeDrone);

                    totalAlt += pos.y;
                    totalSpeed += speed;
                    if (drone.isMotorsArmed()) {
                        armedCount++;
                    }

                    ImGui.tableNextRow();

                    // Highlight active drone row
                    if (isActive) {
                        ImGui.tableSetBgColor(imgui.flag.ImGuiTableBgTarget.RowBg0,
                                ImGui.colorConvertFloat4ToU32(0.15f, 0.35f, 0.55f, 0.65f));
                    }

                    ImGui.tableNextColumn();
                    // Click to select
                    if (ImGui.selectable(String.valueOf(i), isActive, imgui.flag.ImGuiSelectableFlags.SpanAllColumns)) {
                        simulation.setActiveDrone(drone);
                    }
                    ImGui.tableNextColumn();
                    ImGui.text(String.format("%.1f", pos.x));
                    ImGui.tableNextColumn();
                    ImGui.text(String.format("%.1f", pos.y));
                    ImGui.tableNextColumn();
                    ImGui.text(String.format("%.1f", pos.z));
                    ImGui.tableNextColumn();
                    ImGui.text(String.format("%.1f", speed));
                    ImGui.tableNextColumn();
                    ImGui.text(String.format("%.0f%%", drone.getThrottle() * 100));
                    ImGui.tableNextColumn();
                    ImGui.text(drone.isMotorsArmed() ? "YES" : "NO");
                }
                ImGui.endTable();

                // Footer summary
                int n = drones.size();
                if (n > 0) {
                    ImGui.separator();
                    ImGui.text(String.format("Drones: %d | Avg Alt: %.1f | Avg Spd: %.1f | Armed: %d",
                            n, totalAlt / n, totalSpeed / n, armedCount));
                }
            }
        }
        ImGui.end();
    }

    /**
     * Compact list view for the left sidebar.
     */
    public void renderCompact(SimulationEngine simulation) {
        java.util.List<com.paperpiper.drone.Drone> drones = simulation.getDrones();
        com.paperpiper.drone.Drone activeDrone = simulation.getActiveDrone();

        for (int i = 0; i < drones.size(); i++) {
            com.paperpiper.drone.Drone drone = drones.get(i);
            boolean isActive = (drone == activeDrone);

            String label = String.format("%s Drone %02d", isActive ? "●" : "○", i + 1);
            if (ImGui.selectable(label, isActive)) {
                simulation.setActiveDrone(drone);
            }
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
