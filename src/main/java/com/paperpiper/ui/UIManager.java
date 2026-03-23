package com.paperpiper.ui;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central class that owns the ImGui context and handles its per-frame lifecycle.
 * Every other UI class renders within the frame that UIManager opens and closes.
 */
public class UIManager {

    private static final Logger logger = LoggerFactory.getLogger(UIManager.class);

    private final ImGuiImplGlfw imguiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imguiGl3 = new ImGuiImplGl3();

    /**
     * Initialize ImGui. Call AFTER the GL context is current.
     * @param windowHandle GLFW window handle from Window.getHandle()
     */
    public void init(long windowHandle) {
        ImGui.createContext();

        ImGuiIO io = ImGui.getIO();
        io.addConfigFlags(ImGuiConfigFlags.DockingEnable);

        ImGui.styleColorsDark();

        // Install GLFW callbacks — replaces existing key callback in Window.java
        imguiGlfw.init(windowHandle, true);

        // OpenGL 3.3 core
        imguiGl3.init("#version 330 core");

        logger.info("ImGui initialized (version: {})", ImGui.getVersion());
    }

    /** Call at the START of each UI frame, before any ImGui widget code. */
    public void beginFrame() {
        imguiGlfw.newFrame();
        ImGui.newFrame();
    }

    /** Call at the END of each UI frame, after all ImGui widget code. */
    public void endFrame() {
        ImGui.render();
        imguiGl3.renderDrawData(ImGui.getDrawData());
    }

    /** True if ImGui wants the mouse (user is hovering/clicking a panel). */
    public boolean isCapturingMouse() {
        return ImGui.getIO().getWantCaptureMouse();
    }

    /** True if ImGui wants keyboard input (user is typing in a text field). */
    public boolean isCapturingKeyboard() {
        return ImGui.getIO().getWantCaptureKeyboard();
    }

    /** Cleanup. Call BEFORE renderer and window cleanup. */
    public void cleanup() {
        imguiGl3.dispose();
        imguiGlfw.dispose();
        ImGui.destroyContext();
        logger.info("ImGui cleaned up");
    }
}
