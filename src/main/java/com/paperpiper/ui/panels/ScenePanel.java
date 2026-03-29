package com.paperpiper.ui.panels;

import com.paperpiper.simulation.SceneConfig;
import com.paperpiper.simulation.SceneManager;
import com.paperpiper.simulation.SimulationEngine;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiSelectableFlags;
import imgui.type.ImString;

public class ScenePanel {

    private final ImString newSceneName = new ImString("New Scene", 64);
    private boolean showNewSceneDialog = false;

    private String statusMessage = "";
    private float statusTimer = 0f;

    public void render(SceneManager sceneManager, SimulationEngine simulation, float deltaTime) {
        if (statusTimer > 0) {
            statusTimer -= deltaTime;
        }

        SceneConfig current = sceneManager.getCurrentScene();

        ImGui.text("Scene Manager");
        ImGui.separator();

        ImGui.textDisabled("Active:");
        ImGui.sameLine();
        ImGui.pushStyleColor(ImGuiCol.Text, 0.35f, 0.85f, 0.35f, 1.0f);
        ImGui.text(current != null ? current.getName() : "\u2014");
        ImGui.popStyleColor();

        if (sceneManager.isModified()) {
            ImGui.sameLine();
            ImGui.pushStyleColor(ImGuiCol.Text, 1.0f, 0.75f, 0.2f, 1.0f);
            ImGui.text(" *");
            ImGui.popStyleColor();
        }

        ImGui.spacing();

        float listH = ImGui.getContentRegionAvailY() - (showNewSceneDialog ? 64 : 42);
        if (ImGui.beginChild("##SceneList", 0, listH, true)) {
            var scenes = sceneManager.getScenes();
            for (int i = 0; i < scenes.size(); i++) {
                SceneConfig cfg = scenes.get(i);
                boolean isActive = (cfg == current);

                if (isActive) {
                    ImGui.pushStyleColor(ImGuiCol.Text, 0.35f, 0.85f, 0.35f, 1.0f);
                }
                String rowLabel = (isActive ? "\u25CF " : "\u25CB ") + cfg.getName();
                ImGui.selectable(rowLabel + "##scene" + i, isActive,
                        ImGuiSelectableFlags.SpanAllColumns);
                if (isActive) {
                    ImGui.popStyleColor();
                }

                if (ImGui.isItemClicked()) {
                    sceneManager.setCurrentScene(cfg);
                }

                if (ImGui.isItemHovered()) {
                    String desc = cfg.getDescription();
                    String tip = (desc != null && !desc.isEmpty() ? desc + "  |  " : "")
                            + cfg.getDroneCount() + " drone(s)";
                    ImGui.setTooltip(tip);
                }

                float btnW = 42;
                ImGui.sameLine(ImGui.getContentRegionAvailX() - btnW + ImGui.getScrollX());
                if (ImGui.smallButton("Load##" + i)) {
                    sceneManager.setCurrentScene(cfg);
                    simulation.loadScene(cfg);
                    flash("Loaded: " + cfg.getName());
                }
            }
        }
        ImGui.endChild();

        ImGui.spacing();

        if (ImGui.smallButton("+ New")) {
            showNewSceneDialog = !showNewSceneDialog;
        }
        ImGui.sameLine();
        if (ImGui.smallButton("Save As")) {
            String baseName = current != null ? current.getName() : "Scene";
            sceneManager.saveCurrentAs(baseName + " *", simulation);
            flash("Saved!");
        }
        if (current != null && !sceneManager.isBuiltin(current)) {
            ImGui.sameLine();
            ImGui.pushStyleColor(ImGuiCol.Button, 0.55f, 0.12f, 0.12f, 1.0f);
            if (ImGui.smallButton("Delete")) {
                sceneManager.deleteScene(current);
                flash("Deleted");
            }
            ImGui.popStyleColor();
        }

        if (statusTimer > 0) {
            ImGui.sameLine();
            ImGui.pushStyleColor(ImGuiCol.Text, 0.35f, 0.85f, 0.35f, 1.0f);
            ImGui.text(statusMessage);
            ImGui.popStyleColor();
        }

        if (showNewSceneDialog) {
            ImGui.separator();
            ImGui.setNextItemWidth(100);
            ImGui.inputText("##newname", newSceneName, ImGuiInputTextFlags.None);
            ImGui.sameLine();
            if (ImGui.smallButton("OK")) {
                String n = newSceneName.get().trim();
                if (!n.isEmpty()) {
                    sceneManager.createScene(n);
                    flash("Created: " + n);
                }
                showNewSceneDialog = false;
            }
            ImGui.sameLine();
            if (ImGui.smallButton("X")) {
                showNewSceneDialog = false;
            }
        }
    }

    private void flash(String msg) {
        statusMessage = msg;
        statusTimer = 3.0f;
    }
}
