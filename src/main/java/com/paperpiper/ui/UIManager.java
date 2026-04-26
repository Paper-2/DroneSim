package com.paperpiper.ui;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import imgui.ImFontConfig;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;

/**
 * Central class that owns the ImGui context and handles its per-frame
 * lifecycle. Every other UI class renders within the frame that UIManager opens
 * and closes.
 */
public class UIManager {

    private static final Logger logger = LoggerFactory.getLogger(UIManager.class);

    private final ImGuiImplGlfw imguiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imguiGl3 = new ImGuiImplGl3();

    /**
     * Initialize ImGui. Call AFTER the GL context is current.
     *
     * @param windowHandle GLFW window handle from Window.getHandle()
     */
    public void init(long windowHandle) {
        ImGui.createContext();

        ImGuiIO io = ImGui.getIO();
        io.addConfigFlags(ImGuiConfigFlags.DockingEnable);

        loadMonocraftFont(io);

        UITheme.apply();

        // Install GLFW callbacks  replaces existing key callback in Window.java
        imguiGlfw.init(windowHandle, true);

        // OpenGL 3.3 core
        imguiGl3.init("#version 330 core");

        logger.info("ImGui initialized (version: {})", ImGui.getVersion());
    }

    private void loadMonocraftFont(ImGuiIO io) {
        final String resourcePath = "/Fonts/Monocraft-nerd-fonts-patched.ttc";
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                logger.warn("Monocraft font not found at {}, using default font", resourcePath);
                return;
            }
            byte[] fontData = is.readAllBytes();
            ImFontConfig config = new ImFontConfig();
            config.setFontDataOwnedByAtlas(false);
            config.setName("Monocraft");
            config.setOversampleH(2);
            config.setOversampleV(2);

            // Build comprehensive glyph ranges to include all Unicode blocks
            short[] glyphRanges = buildComprehensiveGlyphRanges();
            config.setGlyphRanges(glyphRanges);

            io.getFonts().addFontFromMemoryTTF(fontData, 16, config);
            config.destroy();
            logger.info("Loaded Monocraft font ({} bytes) with comprehensive glyph ranges", fontData.length);
        } catch (IOException e) {
            logger.warn("Failed to load Monocraft font: {}", e.getMessage());
        }
    }

    /**
     * Builds glyph ranges covering: ASCII, Latin Extended, Greek, Cyrillic,
     * CJK, Mathematical Operators, Box Drawing, and various symbol blocks.
     */
    private short[] buildComprehensiveGlyphRanges() {
        // Use a list to accumulate ranges dynamically
        java.util.List<Short> ranges = new java.util.ArrayList<>();

        // ASCII + Latin-1 Supplement + Latin Extended-A/B
        addRange(ranges, 0x0020, 0x02B9);  // ASCII through Latin Extended-B

        // Greek and Coptic
        addRange(ranges, 0x0370, 0x03FF);

        // Cyrillic and Cyrillic Supplement
        addRange(ranges, 0x0400, 0x04FF);
        addRange(ranges, 0x0500, 0x052F);

        // Hebrew
        addRange(ranges, 0x0590, 0x05FF);

        // General Punctuation, Superscripts/Subscripts, Currency Symbols
        addRange(ranges, 0x2000, 0x206F);  // General Punctuation
        addRange(ranges, 0x2070, 0x209F);  // Superscripts and Subscripts
        addRange(ranges, 0x20A0, 0x20CF);  // Currency Symbols

        // Letterlike Symbols, Number Forms, Arrows
        addRange(ranges, 0x2100, 0x214F);  // Letterlike Symbols
        addRange(ranges, 0x2150, 0x218F);  // Number Forms
        addRange(ranges, 0x2190, 0x21FF);  // Arrows
        addRange(ranges, 0x2200, 0x22FF);  // Mathematical Operators
        addRange(ranges, 0x2300, 0x23FF);  // Miscellaneous Technical
        addRange(ranges, 0x2400, 0x243F);  // Optical Character Recognition
        addRange(ranges, 0x2440, 0x245F);  // Enclosed Alphanumerics

        // Box Drawing, Block Elements, Geometric Shapes
        addRange(ranges, 0x2500, 0x257F);  // Box Drawing
        addRange(ranges, 0x2580, 0x259F);  // Block Elements
        addRange(ranges, 0x25A0, 0x25FF);  // Geometric Shapes

        // Miscellaneous Symbols, Dingbats
        addRange(ranges, 0x2600, 0x26FF);  // Miscellaneous Symbols
        addRange(ranges, 0x2700, 0x27BF);  // Dingbats

        // Miscellaneous Mathematical Symbols-A, Supplemental Mathematical Operators
        addRange(ranges, 0x27C0, 0x27EF);  // Miscellaneous Math Symbols-A
        addRange(ranges, 0x2A00, 0x2AFF);  // Supplemental Math Operators

        // CJK Unified Ideographs (basic coverage)
        addRange(ranges, 0x4E00, 0x9FFF);

        // Terminate range list
        ranges.add((short) 0);

        short[] result = new short[ranges.size()];
        for (int i = 0; i < ranges.size(); i++) {
            result[i] = ranges.get(i);
        }
        return result;
    }

    /**
     * Helper to add a glyph range [start, end] to the list.
     */
    private void addRange(java.util.List<Short> ranges, int start, int end) {
        ranges.add((short) start);
        ranges.add((short) end);
    }

    /**
     * Call at the START of each UI frame, before any ImGui widget code.
     */
    public void beginFrame() {
        imguiGlfw.newFrame();
        ImGui.newFrame();
    }

    /**
     * Call at the END of each UI frame, after all ImGui widget code.
     */
    public void endFrame() {
        ImGui.render();
        imguiGl3.renderDrawData(ImGui.getDrawData());
    }

    // Makes ImGui ignore mouse input, allowing it to pass through to the scene
    // fixes a bug where if the mouse is locked to the window, ImGui still
    // captures it and highlights buttons
    public void setMousePassthrough(boolean passthrough) {
        ImGuiIO io = ImGui.getIO();
        if (passthrough) {
            io.addConfigFlags(ImGuiConfigFlags.NoMouse);
        } else {
            io.setConfigFlags(io.getConfigFlags() & ~ImGuiConfigFlags.NoMouse);
        }
    }

    /**
     * True if ImGui wants the mouse (user is hovering/clicking a panel).
     */
    public boolean isCapturingMouse() {
        return ImGui.getIO().getWantCaptureMouse();
    }

    /**
     * True if ImGui wants keyboard input (user is typing in a text field).
     */
    public boolean isCapturingKeyboard() {
        return ImGui.getIO().getWantCaptureKeyboard();
    }

    /**
     * Cleanup. Call BEFORE renderer and window cleanup.
     */
    public void cleanup() {
        imguiGl3.dispose();
        imguiGlfw.dispose();
        ImGui.destroyContext();
        logger.info("ImGui cleaned up");
    }
}
