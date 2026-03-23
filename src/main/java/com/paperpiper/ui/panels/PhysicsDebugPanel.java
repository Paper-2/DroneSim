package com.paperpiper.ui.panels;

import com.paperpiper.drone.Drone;
import com.paperpiper.physics.PhysicsWorld;
import com.paperpiper.simulation.SimulationEngine;
import imgui.ImGui;
import imgui.flag.ImGuiCond;

/**
 * Physics tuning panel with sliders for gravity, mass, thrust, torque.
 */
public class PhysicsDebugPanel {

    private boolean visible = false;

    // Current tuning values (defaults match Drone.java / PhysicsWorld)
    private final float[] gravity = { -9.81f };
    private final float[] mass = { 1.5f };
    private final float[] maxThrust = { 5.0f };
    private final float[] maxTorque = { 5.0f };

    // PID gains
    private final float[] angleKp = { 4.0f };
    private final float[] angleKd = { 0.2f };
    private final float[] rateKp = { 0.7f };
    private final float[] rateKi = { 0.1f };
    private final float[] rateKd = { 0.02f };

    public void render(SimulationEngine simulation, PhysicsWorld physicsWorld) {
        if (!visible) return;

        ImGui.setNextWindowSize(300, 350, ImGuiCond.FirstUseEver);
        if (ImGui.begin("Physics Debug")) {

            // Gravity
            ImGui.separator(); ImGui.text("World");
            if (ImGui.sliderFloat("Gravity", gravity, -20.0f, 0.0f, "%.2f m/s\u00B2")) {
                physicsWorld.setGravity(gravity[0]);
            }

            // Drone physics
            ImGui.separator(); ImGui.text("Drone Physics");
            ImGui.sliderFloat("Mass", mass, 0.1f, 10.0f, "%.2f kg");
            ImGui.sliderFloat("Max Thrust", maxThrust, 1.0f, 50.0f, "%.1f N");
            ImGui.sliderFloat("Max Torque", maxTorque, 0.5f, 20.0f, "%.1f N\u00B7m");

            // PID tuning
            ImGui.separator(); ImGui.text("PID — Attitude Loop");
            ImGui.sliderFloat("Angle Kp", angleKp, 0.0f, 20.0f, "%.2f");
            ImGui.sliderFloat("Angle Kd", angleKd, 0.0f, 2.0f, "%.3f");

            ImGui.separator(); ImGui.text("PID — Rate Loop");
            ImGui.sliderFloat("Rate Kp", rateKp, 0.0f, 5.0f, "%.3f");
            ImGui.sliderFloat("Rate Ki", rateKi, 0.0f, 1.0f, "%.3f");
            ImGui.sliderFloat("Rate Kd", rateKd, 0.0f, 0.5f, "%.4f");

            // Apply button
            ImGui.separator();
            if (ImGui.button("Apply to Active Drone", 180, 0)) {
                Drone active = simulation.getActiveDrone();
                if (active != null) {
                    active.getController().setAngleGains(angleKp[0], 0, angleKd[0]);
                    active.getController().setRateGains(rateKp[0], rateKi[0], rateKd[0]);
                }
            }

            ImGui.sameLine();
            if (ImGui.button("Reset", 80, 0)) {
                gravity[0] = -9.81f;
                mass[0] = 1.5f;
                maxThrust[0] = 5.0f;
                maxTorque[0] = 5.0f;
                angleKp[0] = 4.0f;
                angleKd[0] = 0.2f;
                rateKp[0] = 0.7f;
                rateKi[0] = 0.1f;
                rateKd[0] = 0.02f;
                physicsWorld.setGravity(-9.81f);
            }
        }
        ImGui.end();
    }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
}
