package com.paperpiper.ui.overlay;

import java.util.List;

import com.paperpiper.drone.Drone;
import com.paperpiper.render.Camera;
import com.paperpiper.simulation.SimulationEngine;
import imgui.ImDrawList;
import imgui.ImGui;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Renders floating text labels above each drone in the 3D viewport.
 * Labels are 2D text drawn at screen positions computed by projecting
 * the drone's world position through the camera's view-projection matrix.
 */
public class DroneHUD3D {

    private boolean visible = true;

    private static final float LABEL_OFFSET_Y = 0.5f;
    private static final float MAX_LABEL_DISTANCE = 50.0f;

    public void render(SimulationEngine simulation, Camera camera,
                       Matrix4f projectionMatrix, int windowWidth, int windowHeight) {
        if (!visible) return;

        List<Drone> drones = simulation.getDrones();
        Drone activeDrone = simulation.getActiveDrone();

        Matrix4f viewMatrix = camera.getViewMatrix();
        Matrix4f viewProj = new Matrix4f();
        projectionMatrix.mul(viewMatrix, viewProj);

        ImDrawList drawList = ImGui.getForegroundDrawList();

        for (int i = 0; i < drones.size(); i++) {
            Drone drone = drones.get(i);
            var worldPos = drone.getPosition();

            // Distance from camera for fade effect
            var camPos = camera.getPosition();
            float dx = worldPos.x - camPos.x;
            float dy = worldPos.y - camPos.y;
            float dz = worldPos.z - camPos.z;
            float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

            if (distance > MAX_LABEL_DISTANCE) continue;

            // Project to screen
            Vector4f clip = new Vector4f(
                    worldPos.x,
                    worldPos.y + LABEL_OFFSET_Y,
                    worldPos.z,
                    1.0f
            );
            viewProj.transform(clip);

            if (clip.w <= 0) continue; // behind camera

            float ndcX = clip.x / clip.w;
            float ndcY = clip.y / clip.w;

            float screenX = (ndcX + 1.0f) * 0.5f * windowWidth;
            float screenY = (1.0f - ndcY) * 0.5f * windowHeight;

            // Clamp to screen bounds
            if (screenX < -50 || screenX > windowWidth + 50) continue;
            if (screenY < -50 || screenY > windowHeight + 50) continue;

            // Fade alpha based on distance
            float alpha = 1.0f - (distance / MAX_LABEL_DISTANCE);
            alpha = Math.max(0.2f, alpha);

            // Color: active drone = cyan, others = white
            boolean isActive = (drone == activeDrone);
            int color = isActive
                    ? ImGui.colorConvertFloat4ToU32(0, 1, 1, alpha)
                    : ImGui.colorConvertFloat4ToU32(1, 1, 1, alpha * 0.7f);

            int bgColor = ImGui.colorConvertFloat4ToU32(0, 0, 0, alpha * 0.4f);

            // Label text
            String line1 = String.format("#%d", i);
            String line2 = String.format("%.1fm  %.1f m/s", worldPos.y, drone.getVelocity().length());

            // Background rect
            var size1 = ImGui.calcTextSize(line1);
            var size2 = ImGui.calcTextSize(line2);
            float textWidth = Math.max(size1.x, size2.x);
            float padding = 4;
            drawList.addRectFilled(
                    screenX - padding,
                    screenY - padding,
                    screenX + textWidth + padding,
                    screenY + 28 + padding,
                    bgColor, 4
            );

            // Text
            drawList.addText(screenX, screenY, color, line1);
            drawList.addText(screenX, screenY + 14, color, line2);
        }
    }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
}
