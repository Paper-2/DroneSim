package com.paperpiper.ui.panels;

import com.paperpiper.drone.Drone;
import com.paperpiper.ui.data.DataRecorder;
import imgui.ImGui;
import imgui.flag.ImGuiCond;

/**
 * Real-time time-series graphs for altitude, speed, and throttle.
 */
public class GraphPanel {

    private boolean visible = true;

    private static final int BUFFER_SIZE = 300; // ~5 seconds at 60fps

    private final DataRecorder altitudeData = new DataRecorder(BUFFER_SIZE);
    private final DataRecorder speedData = new DataRecorder(BUFFER_SIZE);
    private final DataRecorder throttleData = new DataRecorder(BUFFER_SIZE);

    private int lastDroneHash = 0;

    /** Call once per frame to record data from the active drone. */
    public void update(Drone drone) {
        if (drone == null) return;

        // Reset buffers when active drone changes
        int hash = System.identityHashCode(drone);
        if (hash != lastDroneHash) {
            altitudeData.clear();
            speedData.clear();
            throttleData.clear();
            lastDroneHash = hash;
        }

        altitudeData.record(drone.getPosition().y);
        speedData.record(drone.getVelocity().length());
        throttleData.record(drone.getThrottle());
    }

    public void render() {
        if (!visible) return;

        ImGui.setNextWindowSize(350, 320, ImGuiCond.FirstUseEver);
        if (ImGui.begin("Graphs")) {

            float width = ImGui.getContentRegionAvailX();
            float graphHeight = 60;

            // Altitude graph
            float[] altData = altitudeData.getOrderedData();
            String altOverlay = altData.length > 0 ? String.format("%.1f m", altData[altData.length - 1]) : "N/A";
            ImGui.text("Altitude");
            ImGui.plotLines("##alt", altData, altData.length, 0, altOverlay, 0, 50, width, graphHeight);

            ImGui.spacing();

            // Speed graph
            float[] spdData = speedData.getOrderedData();
            String spdOverlay = spdData.length > 0 ? String.format("%.1f m/s", spdData[spdData.length - 1]) : "N/A";
            ImGui.text("Speed");
            ImGui.plotLines("##spd", spdData, spdData.length, 0, spdOverlay, 0, 20, width, graphHeight);

            ImGui.spacing();

            // Throttle graph
            float[] thrData = throttleData.getOrderedData();
            String thrOverlay = thrData.length > 0 ? String.format("%.0f%%", thrData[thrData.length - 1] * 100) : "N/A";
            ImGui.text("Throttle");
            ImGui.plotLines("##thr", thrData, thrData.length, 0, thrOverlay, 0, 1, width, graphHeight);
        }
        ImGui.end();
    }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
}
