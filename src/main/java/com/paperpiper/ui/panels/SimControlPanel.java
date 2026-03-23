package com.paperpiper.ui.panels;

import com.paperpiper.simulation.SimulationEngine;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.type.ImFloat;

/**
 * Simulation controls: pause/resume, time scale, spawn/remove drones, debug toggles.
 */
public class SimControlPanel {

    private boolean visible = true;
    private final ImFloat spawnX = new ImFloat(0);
    private final ImFloat spawnY = new ImFloat(2);
    private final ImFloat spawnZ = new ImFloat(0);

    public void render(SimulationEngine simulation) {
        if (!visible) return;

        ImGui.setNextWindowSize(280, 300, ImGuiCond.FirstUseEver);
        if (ImGui.begin("Sim Controls")) {

            // Pause / Resume
            if (simulation.isPaused()) {
                if (ImGui.button("\u25B6 Resume", 120, 0)) {
                    simulation.setPaused(false);
                }
            } else {
                if (ImGui.button("\u23F8 Pause", 120, 0)) {
                    simulation.setPaused(true);
                }
            }

            // Simulation time
            float t = simulation.getSimulationTime();
            int mins = (int) (t / 60);
            float secs = t - mins * 60;
            ImGui.sameLine();
            ImGui.text(String.format("T+ %02d:%05.2f", mins, secs));

            // Time scale slider
            ImGui.separator(); ImGui.text("Time Scale");
            float[] ts = { simulation.getTimeScale() };
            if (ImGui.sliderFloat("##timescale", ts, 0.1f, 5.0f, "%.1fx")) {
                simulation.setTimeScale(ts[0]);
            }
            ImGui.sameLine();
            if (ImGui.smallButton("1x")) {
                simulation.setTimeScale(1.0f);
            }

            // Spawn drone
            ImGui.separator(); ImGui.text("Spawn Drone");
            ImGui.setNextItemWidth(60); ImGui.inputFloat("X##spawn", spawnX, 0, 0, "%.0f");
            ImGui.sameLine();
            ImGui.setNextItemWidth(60); ImGui.inputFloat("Y##spawn", spawnY, 0, 0, "%.0f");
            ImGui.sameLine();
            ImGui.setNextItemWidth(60); ImGui.inputFloat("Z##spawn", spawnZ, 0, 0, "%.0f");
            if (ImGui.button("Spawn", 80, 0)) {
                var drone = simulation.addDrone(new com.jme3.math.Vector3f(spawnX.get(), spawnY.get(), spawnZ.get()));
                drone.setMotorsArmed(true);
            }

            // Remove active drone
            ImGui.sameLine();
            if (simulation.getActiveDrone() != null) {
                ImGui.pushStyleColor(ImGuiCol.Button, 0.6f, 0.15f, 0.15f, 1.0f);
                if (ImGui.button("Remove", 80, 0)) {
                    simulation.removeDrone(simulation.getActiveDrone());
                }
                ImGui.popStyleColor();
            }

            // Reset
            ImGui.separator(); ImGui.text("Reset");
            if (ImGui.button("Reset All Drones", 160, 0)) {
                simulation.reset();
            }

            // Debug toggles
            ImGui.separator(); ImGui.text("Debug");
            if (ImGui.button("Toggle Collision Shapes")) {
                simulation.toggleCollisionShapesVisible();
            }
        }
        ImGui.end();
    }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
}
