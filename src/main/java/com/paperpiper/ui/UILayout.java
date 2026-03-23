package com.paperpiper.ui;

import com.paperpiper.physics.PhysicsWorld;
import com.paperpiper.render.Camera;
import com.paperpiper.render.Renderer;
import com.paperpiper.render.Window;
import com.paperpiper.simulation.SimulationEngine;
import com.paperpiper.ui.data.DataExporter;
import com.paperpiper.ui.overlay.DroneHUD3D;
import com.paperpiper.ui.panels.*;
import imgui.ImGui;
import imgui.flag.ImGuiCol;

/**
 * Main UI orchestrator. Owns all panels, renders the menu bar,
 * and dispatches to each panel's render method.
 */
public class UILayout {

    private final TelemetryPanel telemetryPanel = new TelemetryPanel();
    private final FleetPanel fleetPanel = new FleetPanel();
    private final GraphPanel graphPanel = new GraphPanel();
    private final SimControlPanel simControlPanel = new SimControlPanel();
    private final PhysicsDebugPanel physicsDebugPanel = new PhysicsDebugPanel();
    private final DroneHUD3D droneHUD = new DroneHUD3D();
    private final DataExporter dataExporter = new DataExporter();

    public void render(SimulationEngine simulation, Renderer renderer,
                       PhysicsWorld physicsWorld, Window window) {

        // Main menu bar
        if (ImGui.beginMainMenuBar()) {
            if (ImGui.beginMenu("Panels")) {
                if (ImGui.menuItem("Telemetry", "", telemetryPanel.isVisible()))
                    telemetryPanel.setVisible(!telemetryPanel.isVisible());
                if (ImGui.menuItem("Fleet Overview", "", fleetPanel.isVisible()))
                    fleetPanel.setVisible(!fleetPanel.isVisible());
                if (ImGui.menuItem("Graphs", "", graphPanel.isVisible()))
                    graphPanel.setVisible(!graphPanel.isVisible());
                if (ImGui.menuItem("Sim Controls", "", simControlPanel.isVisible()))
                    simControlPanel.setVisible(!simControlPanel.isVisible());
                if (ImGui.menuItem("Physics Debug", "", physicsDebugPanel.isVisible()))
                    physicsDebugPanel.setVisible(!physicsDebugPanel.isVisible());
                if (ImGui.menuItem("3D Labels", "", droneHUD.isVisible()))
                    droneHUD.setVisible(!droneHUD.isVisible());
                ImGui.endMenu();
            }

            if (ImGui.beginMenu("Recording")) {
                if (!dataExporter.isRecording()) {
                    if (ImGui.menuItem("Start Recording")) {
                        dataExporter.startRecording();
                    }
                } else {
                    ImGui.pushStyleColor(ImGuiCol.Text, 1.0f, 0.2f, 0.2f, 1.0f);
                    if (ImGui.menuItem("\u25CF REC — Stop")) {
                        dataExporter.stopRecording();
                    }
                    ImGui.popStyleColor();
                }
                ImGui.endMenu();
            }

            ImGui.endMainMenuBar();
        }

        // Update graph data recorder
        graphPanel.update(simulation.getActiveDrone());

        // Record telemetry if active
        if (dataExporter.isRecording()) {
            dataExporter.recordFrame(simulation);
        }

        // Render panels
        telemetryPanel.render(simulation.getActiveDrone());
        fleetPanel.render(simulation);
        graphPanel.render();
        simControlPanel.render(simulation);
        physicsDebugPanel.render(simulation, physicsWorld);

        // 3D HUD overlay
        Camera camera = renderer.getCamera();
        droneHUD.render(simulation, camera,
                renderer.getProjectionMatrix(),
                window.getWidth(), window.getHeight());
    }

    public void cleanup() {
        dataExporter.cleanup();
    }
}
