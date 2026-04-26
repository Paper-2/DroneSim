package com.paperpiper.ui;

import com.paperpiper.physics.PhysicsWorld;
import com.paperpiper.render.Camera;
import com.paperpiper.render.Renderer;
import com.paperpiper.render.Window;
import com.paperpiper.simulation.SceneManager;
import com.paperpiper.simulation.SimulationEngine;
import com.paperpiper.ui.data.DataExporter;
import com.paperpiper.ui.overlay.DroneHUD3D;
import com.paperpiper.ui.panels.EventLogPanel;
import com.paperpiper.ui.panels.FleetPanel;
import com.paperpiper.ui.panels.GraphPanel;
import com.paperpiper.ui.panels.GlyphPreviewPanel;
import com.paperpiper.ui.panels.InspectorPanel;
import com.paperpiper.ui.panels.ScenePanel;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImFloat;

/**
 * Main UI orchestrator. Arranges all panels into a fixed, docked layout
 */
public class UILayout {

    // Layout constants 
    private static final float LEFT_SIDEBAR_WIDTH = 220;
    private static final float RIGHT_SIDEBAR_WIDTH = 260;
    private static final float BOTTOM_PANEL_HEIGHT = 185;
    private static final float VIEWPORT_TOOLBAR_HEIGHT = 28;

    // Panels
    private final FleetPanel fleetPanel = new FleetPanel();
    private final InspectorPanel inspectorPanel = new InspectorPanel();
    private final GraphPanel graphPanel = new GraphPanel();
    private final EventLogPanel eventLogPanel = new EventLogPanel();
    private final DroneHUD3D droneHUD = new DroneHUD3D();
    private final DataExporter dataExporter = new DataExporter();
    private final ScenePanel scenePanel = new ScenePanel();
    private final SceneManager sceneManager = new SceneManager();
    private final GlyphPreviewPanel glyphPreviewPanel = new GlyphPreviewPanel();

    // Sim-control inputs (embedded in right sidebar)
    private final ImFloat spawnX = new ImFloat(0);
    private final ImFloat spawnY = new ImFloat(2);
    private final ImFloat spawnZ = new ImFloat(0);

    // Physics-debug tuning values (embedded in right sidebar)
    private final float[] gravity = {-9.81f};
    private final float[] angleKp = {4.0f};
    private final float[] angleKd = {0.2f};
    private final float[] rateKp = {0.7f};
    private final float[] rateKi = {0.1f};
    private final float[] rateKd = {0.02f};

    public void render(SimulationEngine simulation, Renderer renderer,
            PhysicsWorld physicsWorld, Window window, float deltaTime) {

        int winW = window.getWidth();
        int winH = window.getHeight();
        float menuH = 20;

        // Fixed regions
        renderMenuBar(simulation);
        renderLeftSidebar(simulation, menuH, winH, deltaTime);
        renderViewportToolbar(menuH, winW);
        renderBottomPanel(simulation, winW, winH);
        renderRightSidebar(simulation, physicsWorld, menuH, winW, winH, deltaTime);

        // Data recording
        graphPanel.update(simulation.getActiveDrone());
        if (dataExporter.isRecording()) {
            dataExporter.recordFrame(simulation);
        }

        // 3-D HUD overlay
        float vpLeft = LEFT_SIDEBAR_WIDTH;
        float vpTop = menuH + VIEWPORT_TOOLBAR_HEIGHT;
        float vpRight = winW - RIGHT_SIDEBAR_WIDTH;
        float vpBottom = winH - BOTTOM_PANEL_HEIGHT;

        Camera camera = renderer.getCamera();
        droneHUD.render(simulation, camera, renderer.getProjectionMatrix(),
                winW, winH, vpLeft, vpTop, vpRight, vpBottom);
        glyphPreviewPanel.render();
    }

    // Menu Bar
    private void renderMenuBar(SimulationEngine simulation) {
        if (ImGui.beginMainMenuBar()) {

            if (ImGui.beginMenu("File")) {
                if (!dataExporter.isRecording()) {
                    if (ImGui.menuItem("● Start Recording")) {
                        dataExporter.startRecording();
                        eventLogPanel.addEvent(simulation.getSimulationTime(), "Recording started");
                    }
                } else {
                    if (ImGui.menuItem("⏹ Stop Recording")) {
                        dataExporter.stopRecording();
                        eventLogPanel.addEvent(simulation.getSimulationTime(), "Recording stopped");
                    }
                }
                ImGui.endMenu();
            }

            if (ImGui.beginMenu("View")) {
                if (ImGui.menuItem("◎ 3D Labels", "", droneHUD.isVisible())) {
                    droneHUD.setVisible(!droneHUD.isVisible());
                }
                if (ImGui.menuItem("◆ Glyph Preview", "", glyphPreviewPanel.isVisible())) {
                    glyphPreviewPanel.setVisible(!glyphPreviewPanel.isVisible());
                }
                boolean isNight = UITheme.getMode() == UITheme.Mode.NIGHT;
                if (ImGui.menuItem(isNight ? "☀ Light Mode" : "☽ Night Mode")) {
                    UITheme.toggle();
                }
                ImGui.endMenu();
            }

            if (ImGui.beginMenu("Simulation")) {
                if (ImGui.menuItem(simulation.isPaused() ? "▶ Resume" : "⏸ Pause")) {
                    simulation.setPaused(!simulation.isPaused());
                    eventLogPanel.addEvent(simulation.getSimulationTime(),
                            simulation.isPaused() ? "Simulation paused" : "Simulation resumed");
                }
                ImGui.separator();
                if (ImGui.menuItem("⇵ Reset All")) {
                    simulation.reset();
                    eventLogPanel.addEvent(0, "Simulation reset");
                }
                if (ImGui.menuItem("◇ Toggle Collision Shapes")) {
                    simulation.toggleCollisionShapesVisible();
                }
                ImGui.endMenu();
            }

            if (ImGui.beginMenu("Scene")) {
                var scenes = sceneManager.getScenes();
                for (int i = 0; i < scenes.size(); i++) {
                    var cfg = scenes.get(i);
                    if (ImGui.menuItem(cfg.getName())) {
                        sceneManager.setCurrentScene(cfg);
                        simulation.loadScene(cfg);
                        eventLogPanel.addEvent(simulation.getSimulationTime(),
                                "Scene loaded: " + cfg.getName());
                    }
                }
                ImGui.separator();
                if (ImGui.menuItem("★ Spawn Drone")) {
                    var drone = simulation.addDrone(new com.jme3.math.Vector3f(0, 2, 0));
                    drone.setMotorsArmed(true);
                    sceneManager.markModified();
                    eventLogPanel.addEvent(simulation.getSimulationTime(),
                            "Drone spawned (#" + simulation.getDrones().size() + ")");
                }
                if (simulation.getActiveDrone() != null) {
                    if (ImGui.menuItem("✘ Remove Active Drone")) {
                        int idx = simulation.getDrones().indexOf(simulation.getActiveDrone());
                        simulation.removeDrone(simulation.getActiveDrone());
                        sceneManager.markModified();
                        eventLogPanel.addEvent(simulation.getSimulationTime(),
                                "Drone #" + (idx + 1) + " removed");
                    }
                }
                ImGui.endMenu();
            }

            // Right-aligned sim-control buttons
            ImGui.sameLine(ImGui.getWindowWidth() - 170);

            if (simulation.isPaused()) {
                if (ImGui.smallButton("▶")) {
                    simulation.setPaused(false);
                }
            } else {
                if (ImGui.smallButton("⏸")) {
                    simulation.setPaused(true);
                }
            }
            ImGui.sameLine();
            if (ImGui.smallButton("1x")) {
                simulation.setTimeScale(1.0f);
            }
            ImGui.sameLine();
            if (ImGui.smallButton("⏩ .5x")) {
                simulation.setTimeScale(Math.min(5.0f, simulation.getTimeScale() + 0.5f));
            }
            ImGui.sameLine();
            ImGui.text(String.format("%.1fx", simulation.getTimeScale()));

            ImGui.endMainMenuBar();
        }
    }

    // Left Sidebar
    private void renderLeftSidebar(SimulationEngine simulation,
            float menuH, int winH, float deltaTime) {
        int flags = ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoMove
                | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse;

        ImGui.setNextWindowPos(0, menuH, ImGuiCond.Always);
        ImGui.setNextWindowSize(LEFT_SIDEBAR_WIDTH, winH - menuH);

        if (ImGui.begin("##LeftSidebar", flags)) {
            HexBackground.drawCurrentWindow();
            float availH = ImGui.getContentRegionAvailY();

            // ── Drone List ────────────────────────────────────────────────
            ImGui.text("⚐ Drones");
            ImGui.separator();

            float listH = availH * 0.30f;
            if (ImGui.beginChild("##DroneListChild", 0, listH, true)) {
                fleetPanel.renderCompact(simulation);
            }
            ImGui.endChild();

            ImGui.spacing();

            // ── Inspector ────────────────────────────────────────────────
            ImGui.text("◎ Inspector");
            ImGui.separator();

            float inspH = availH * 0.30f;
            if (ImGui.beginChild("##InspectorChild", 0, inspH, false)) {
                inspectorPanel.render(simulation.getActiveDrone());
            }
            ImGui.endChild();

            ImGui.spacing();

            // ── Attitude / Throttle ───────────────────────────────────────
            ImGui.text("⚡ Attitude");
            ImGui.separator();

            if (ImGui.beginChild("##AttitudeChild", 0, 0, false)) {
                renderAttitudeSection(simulation);
            }
            ImGui.endChild();
        }
        ImGui.end();
    }

    /**
     * Inline throttle/attitude bars – migrated from TelemetryPanel.
     */
    private void renderAttitudeSection(SimulationEngine simulation) {
        var drone = simulation.getActiveDrone();
        if (drone == null) {
            ImGui.textDisabled("✘ No drone selected");
            return;
        }

        float barW = ImGui.getContentRegionAvailX() - 4;

        // Throttle
        ImGui.text("↑ Throttle");
        ImGui.progressBar(drone.getThrottle(), barW, 16,
                String.format("%.0f%%", drone.getThrottle() * 100));

        renderCenteredBar("Pitch", drone.getPitch(), barW);
        renderCenteredBar("Roll", drone.getRoll(), barW);
        renderCenteredBar("Yaw", drone.getYaw(), barW);

        ImGui.spacing();

        // Motor outputs
        ImGui.text("ᛏ Motors");
        boolean armed = drone.isMotorsArmed();
        if (armed) {
            ImGui.pushStyleColor(ImGuiCol.Text, 0.2f, 1.0f, 0.2f, 1.0f);
            ImGui.text("⌘ ARMED");
        } else {
            ImGui.pushStyleColor(ImGuiCol.Text, 1.0f, 0.3f, 0.3f, 1.0f);
            ImGui.text("⌀ DISARMED");
        }
        ImGui.popStyleColor();

        float mW = barW * 0.5f - 2;
        ImGui.progressBar(drone.FL, mW, 14, "FL");
        ImGui.sameLine();
        ImGui.progressBar(drone.FR, mW, 14, "FR");
        ImGui.progressBar(drone.RL, mW, 14, "RL");
        ImGui.sameLine();
        ImGui.progressBar(drone.RR, mW, 14, "RR");
    }

    private static void renderCenteredBar(String label, float value, float width) {
        ImGui.text(label);
        ImGui.sameLine(50);
        float normalized = (value + 1.0f) * 0.5f;
        ImGui.progressBar(normalized, width - 50, 16, String.format("%+.2f", value));
    }

    // Viewport Toolbar
    private void renderViewportToolbar(float menuH, int winW) {
        int flags = ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoMove
                | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse
                | ImGuiWindowFlags.NoScrollbar;

        float toolbarW = winW - LEFT_SIDEBAR_WIDTH - RIGHT_SIDEBAR_WIDTH;
        ImGui.setNextWindowPos(LEFT_SIDEBAR_WIDTH, menuH, ImGuiCond.Always);
        ImGui.setNextWindowSize(toolbarW, VIEWPORT_TOOLBAR_HEIGHT);
    }

    // Bottom Panel (graphs left | event log right)
    private void renderBottomPanel(SimulationEngine simulation, int winW, int winH) {
        int flags = ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoMove
                | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse;

        float bottomY = winH - BOTTOM_PANEL_HEIGHT;
        float bottomW = winW - LEFT_SIDEBAR_WIDTH - RIGHT_SIDEBAR_WIDTH;
        ImGui.setNextWindowPos(LEFT_SIDEBAR_WIDTH, bottomY, ImGuiCond.Always);
        ImGui.setNextWindowSize(bottomW, BOTTOM_PANEL_HEIGHT);

        if (ImGui.begin("##BottomPanel", flags)) {
            HexBackground.drawCurrentWindow();
            float halfW = ImGui.getContentRegionAvailX() * 0.55f;
            float childH = ImGui.getContentRegionAvailY() - 22; // leave room for status

            // Left: telemetry graphs
            if (ImGui.beginChild("##GraphsChild", halfW, childH, true)) {
                graphPanel.renderEmbedded();
            }
            ImGui.endChild();

            ImGui.sameLine();

            // Right: event log
            if (ImGui.beginChild("##EventLogChild", 0, childH, true)) {
                eventLogPanel.render();
            }
            ImGui.endChild();

            // Status bar
            ImGui.separator();
            float t = simulation.getSimulationTime();
            int mins = (int) (t / 60);
            float secs = t - mins * 60;
            String rec = dataExporter.isRecording() ? "  ⏺ REC" : "";
            ImGui.textDisabled(String.format("T+ %02d:%05.2f  |  %.1fx%s",
                    mins, secs, simulation.getTimeScale(), rec));
        }
        ImGui.end();
    }

    // Right Sidebar (Scene Manager + Sim Controls / Physics Debug)
    private void renderRightSidebar(SimulationEngine simulation, PhysicsWorld physicsWorld,
            float menuH, int winW, int winH, float deltaTime) {
        int flags = ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoMove
                | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse;

        ImGui.setNextWindowPos(winW - RIGHT_SIDEBAR_WIDTH, menuH, ImGuiCond.Always);
        ImGui.setNextWindowSize(RIGHT_SIDEBAR_WIDTH, winH - menuH);

        if (ImGui.begin("##RightSidebar", flags)) {
            HexBackground.drawCurrentWindow();
            float availH = ImGui.getContentRegionAvailY();

            // Scene Manager (top ~55 %)
            if (ImGui.beginChild("##SceneMgrChild", 0, availH * 0.55f, false)) {
                scenePanel.render(sceneManager, simulation, deltaTime);
            }
            ImGui.endChild();

            ImGui.separator();

            // Sim Controls + Physics (bottom ~45 %)
            if (ImGui.beginChild("##SimCtrlChild", 0, 0, false)) {
                renderSimControlsEmbedded(simulation, physicsWorld);
            }
            ImGui.endChild();
        }
        ImGui.end();
    }

    /**
     * Embedded simulation controls + physics tuning (replaces floating panels).
     */
    private void renderSimControlsEmbedded(SimulationEngine simulation,
            PhysicsWorld physicsWorld) {
        if (ImGui.beginTabBar("##RightTabs")) {

            if (ImGui.beginTabItem("Controls")) {
                // Pause/Resume
                if (simulation.isPaused()) {
                    if (ImGui.button("▶ Resume", -1, 0)) {
                        simulation.setPaused(false);
                    }
                } else {
                    if (ImGui.button("⏸ Pause", -1, 0)) {
                        simulation.setPaused(true);
                    }
                }

                // Time scale
                ImGui.text("⏳ Time Scale");
                float[] ts = {simulation.getTimeScale()};
                float sliderW = ImGui.getContentRegionAvailX() - 30;
                ImGui.setNextItemWidth(sliderW);
                if (ImGui.sliderFloat("##ts", ts, 0.1f, 5.0f, "%.1fx")) {
                    simulation.setTimeScale(ts[0]);
                }
                ImGui.sameLine();
                if (ImGui.smallButton("1x")) {
                    simulation.setTimeScale(1.0f);
                }

                // Spawn
                ImGui.separator();
                ImGui.text("★ Spawn Drone");
                float fw = (ImGui.getContentRegionAvailX() - 6) / 3f;
                ImGui.setNextItemWidth(fw);
                ImGui.inputFloat("##sx", spawnX, 0, 0, "%.0f");
                ImGui.sameLine();
                ImGui.setNextItemWidth(fw);
                ImGui.inputFloat("##sy", spawnY, 0, 0, "%.0f");
                ImGui.sameLine();
                ImGui.setNextItemWidth(fw);
                ImGui.inputFloat("##sz", spawnZ, 0, 0, "%.0f");

                float bw = (ImGui.getContentRegionAvailX() - 4) / 2f;
                if (ImGui.button("★ Spawn", bw, 0)) {
                    var d = simulation.addDrone(
                            new com.jme3.math.Vector3f(spawnX.get(), spawnY.get(), spawnZ.get()));
                    d.setMotorsArmed(true);
                    sceneManager.markModified();
                }
                ImGui.sameLine();
                if (simulation.getActiveDrone() != null) {
                    ImGui.pushStyleColor(ImGuiCol.Button, 0.55f, 0.12f, 0.12f, 1.0f);
                    if (ImGui.button("✘ Remove", bw, 0)) {
                        simulation.removeDrone(simulation.getActiveDrone());
                        sceneManager.markModified();
                    }
                    ImGui.popStyleColor();
                }

                // Reset
                ImGui.separator();
                if (ImGui.button("⇵ Reset All", -1, 0)) {
                    simulation.reset();
                    sceneManager.markModified();
                }
                if (ImGui.button("◇ Toggle Colliders", -1, 0)) {
                    simulation.toggleCollisionShapesVisible();
                }

                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("Physics")) {
                ImGui.text("⚓ World");
                if (ImGui.sliderFloat("↓ Gravity", gravity, -20.0f, 0.0f, "%.2f")) {
                    physicsWorld.setGravity(gravity[0]);
                }

                ImGui.separator();
                ImGui.text("⚡ Attitude PID");
                ImGui.sliderFloat("Angle Kp", angleKp, 0.0f, 20.0f, "%.2f");
                ImGui.sliderFloat("Angle Kd", angleKd, 0.0f, 2.0f, "%.3f");

                ImGui.separator();
                ImGui.text("◆ Rate PID");
                ImGui.sliderFloat("Rate Kp", rateKp, 0.0f, 5.0f, "%.2f");
                ImGui.sliderFloat("Rate Ki", rateKi, 0.0f, 1.0f, "%.3f");
                ImGui.sliderFloat("Rate Kd", rateKd, 0.0f, 0.5f, "%.3f");

                ImGui.endTabItem();
            }

            ImGui.endTabBar();
        }
    }

    public EventLogPanel getEventLog() {
        return eventLogPanel;
    }

    public SceneManager getSceneManager() {
        return sceneManager;
    }

    public void cleanup() {
        dataExporter.cleanup();
    }
}
