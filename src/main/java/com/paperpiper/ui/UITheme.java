package com.paperpiper.ui;

import imgui.ImGui;
import imgui.ImGuiStyle;
import imgui.flag.ImGuiCol;

/**
 * Centralized color-scheme manager for the ImGui UI. Supports Light and Night
 * profiles, switchable at runtime.
 */
public final class UITheme {

    public enum Mode {
        LIGHT, NIGHT
    }

    private static Mode currentMode = Mode.NIGHT;

    private UITheme() {
    }

    // ── Active palette (set by apply) ────────────────────────────────────
    public static float[] BG;
    public static float[] TEXT;
    public static float[] TEXT_DIM;
    public static float[] BORDER;
    public static float[] FRAME;
    public static float[] FRAME_HOVER;
    public static float[] FRAME_ACTIVE;
    public static float[] BUTTON;
    public static float[] BUTTON_HOVER;
    public static float[] BUTTON_ACTIVE;
    public static float[] ACCENT;
    public static int HEX_FILL_A;
    public static int HEX_FILL_B;
    public static int HEX_EDGE;

    // ── Light profile ────────────────────────────────────────────────────
    private static final float[] L_BG = {1.00f, 1.00f, 1.00f, 1.00f};
    private static final float[] L_TEXT = {0.10f, 0.10f, 0.10f, 1.00f};
    private static final float[] L_TEXT_DIM = {0.50f, 0.50f, 0.50f, 1.00f};
    private static final float[] L_BORDER = {0.78f, 0.78f, 0.78f, 1.00f};
    private static final float[] L_FRAME = {0.92f, 0.92f, 0.92f, 1.00f};
    private static final float[] L_FRAME_HOVER = {0.85f, 0.85f, 0.85f, 1.00f};
    private static final float[] L_FRAME_ACTIVE = {0.78f, 0.78f, 0.78f, 1.00f};
    private static final float[] L_BUTTON = {0.88f, 0.88f, 0.88f, 1.00f};
    private static final float[] L_BUTTON_HOVER = {0.78f, 0.78f, 0.78f, 1.00f};
    private static final float[] L_BUTTON_ACTIVE = {0.65f, 0.65f, 0.65f, 1.00f};
    private static final float[] L_ACCENT = {0.35f, 0.55f, 0.82f, 1.00f};
    private static final int L_HEX_A = 0xFFF0F0F0;
    private static final int L_HEX_B = 0xFFF7F7F7;
    private static final int L_HEX_EDGE = 0xFFD6D6D6;

    // ── Night profile ────────────────────────────────────────────────────
    private static final float[] N_BG = {0.12f, 0.12f, 0.14f, 1.00f};
    private static final float[] N_TEXT = {0.90f, 0.90f, 0.92f, 1.00f};
    private static final float[] N_TEXT_DIM = {0.50f, 0.50f, 0.55f, 1.00f};
    private static final float[] N_BORDER = {0.30f, 0.30f, 0.35f, 1.00f};
    private static final float[] N_FRAME = {0.18f, 0.18f, 0.22f, 1.00f};
    private static final float[] N_FRAME_HOVER = {0.24f, 0.24f, 0.28f, 1.00f};
    private static final float[] N_FRAME_ACTIVE = {0.30f, 0.30f, 0.35f, 1.00f};
    private static final float[] N_BUTTON = {0.22f, 0.22f, 0.26f, 1.00f};
    private static final float[] N_BUTTON_HOVER = {0.30f, 0.30f, 0.35f, 1.00f};
    private static final float[] N_BUTTON_ACTIVE = {0.38f, 0.38f, 0.42f, 1.00f};
    private static final float[] N_ACCENT = {0.40f, 0.60f, 0.90f, 1.00f};
    private static final int N_HEX_A = 0x001E1E22;
    private static final int N_HEX_B = 0x99242428;
    private static final int N_HEX_EDGE = 0xFF3A3A40;

    public static Mode getMode() {
        return currentMode;
    }

    /**
     * Apply the given mode. Safe to call at any time after
     * ImGui.createContext().
     */
    public static void apply(Mode mode) {
        currentMode = mode;
        boolean light = (mode == Mode.LIGHT);

        BG = light ? L_BG : N_BG;
        TEXT = light ? L_TEXT : N_TEXT;
        TEXT_DIM = light ? L_TEXT_DIM : N_TEXT_DIM;
        BORDER = light ? L_BORDER : N_BORDER;
        FRAME = light ? L_FRAME : N_FRAME;
        FRAME_HOVER = light ? L_FRAME_HOVER : N_FRAME_HOVER;
        FRAME_ACTIVE = light ? L_FRAME_ACTIVE : N_FRAME_ACTIVE;
        BUTTON = light ? L_BUTTON : N_BUTTON;
        BUTTON_HOVER = light ? L_BUTTON_HOVER : N_BUTTON_HOVER;
        BUTTON_ACTIVE = light ? L_BUTTON_ACTIVE : N_BUTTON_ACTIVE;
        ACCENT = light ? L_ACCENT : N_ACCENT;
        HEX_FILL_A = light ? L_HEX_A : N_HEX_A;
        HEX_FILL_B = light ? L_HEX_B : N_HEX_B;
        HEX_EDGE = light ? L_HEX_EDGE : N_HEX_EDGE;

        if (light) {
            ImGui.styleColorsLight();
        } else {
            ImGui.styleColorsDark();
        }
        ImGuiStyle s = ImGui.getStyle();

        set(s, ImGuiCol.WindowBg, BG);
        set(s, ImGuiCol.ChildBg, BG);
        set(s, ImGuiCol.PopupBg, BG);
        set(s, ImGuiCol.MenuBarBg, light ? 0.94f : 0.16f, light ? 0.94f : 0.16f, light ? 0.94f : 0.18f, 1f);

        set(s, ImGuiCol.Text, TEXT);
        set(s, ImGuiCol.TextDisabled, TEXT_DIM);

        set(s, ImGuiCol.Border, BORDER);
        set(s, ImGuiCol.BorderShadow, 0f, 0f, 0f, 0f);

        set(s, ImGuiCol.FrameBg, FRAME);
        set(s, ImGuiCol.FrameBgHovered, FRAME_HOVER);
        set(s, ImGuiCol.FrameBgActive, FRAME_ACTIVE);

        float tb = light ? 0.90f : 0.15f;
        float tba = light ? 0.85f : 0.20f;
        float tbc = light ? 0.95f : 0.10f;
        set(s, ImGuiCol.TitleBg, tb, tb, tb, 1f);
        set(s, ImGuiCol.TitleBgActive, tba, tba, tba, 1f);
        set(s, ImGuiCol.TitleBgCollapsed, tbc, tbc, tbc, 1f);

        float sbg = light ? 0.96f : 0.14f;
        float sg = light ? 0.70f : 0.35f;
        float sgh = light ? 0.55f : 0.45f;
        float sga = light ? 0.45f : 0.55f;
        set(s, ImGuiCol.ScrollbarBg, sbg, sbg, sbg, 1f);
        set(s, ImGuiCol.ScrollbarGrab, sg, sg, sg, 1f);
        set(s, ImGuiCol.ScrollbarGrabHovered, sgh, sgh, sgh, 1f);
        set(s, ImGuiCol.ScrollbarGrabActive, sga, sga, sga, 1f);

        set(s, ImGuiCol.Button, BUTTON);
        set(s, ImGuiCol.ButtonHovered, BUTTON_HOVER);
        set(s, ImGuiCol.ButtonActive, BUTTON_ACTIVE);

        float h = light ? 0.88f : 0.22f;
        float hh = light ? 0.80f : 0.28f;
        float ha = light ? 0.72f : 0.34f;
        set(s, ImGuiCol.Header, h, h, h, 1f);
        set(s, ImGuiCol.HeaderHovered, hh, hh, hh, 1f);
        set(s, ImGuiCol.HeaderActive, ha, ha, ha, 1f);

        set(s, ImGuiCol.Separator, BORDER);
        float sh = light ? 0.55f : 0.40f;
        float sa = light ? 0.40f : 0.50f;
        set(s, ImGuiCol.SeparatorHovered, sh, sh, sh, 1f);
        set(s, ImGuiCol.SeparatorActive, sa, sa, sa, 1f);

        float t = light ? 0.90f : 0.16f;
        float th = light ? 0.80f : 0.24f;
        float ta = light ? 0.95f : 0.20f;
        set(s, ImGuiCol.Tab, t, t, t, 1f);
        set(s, ImGuiCol.TabHovered, th, th, th, 1f);
        set(s, ImGuiCol.TabActive, ta, ta, ta, 1f);

        float cm = light ? 0.20f : 0.80f;
        set(s, ImGuiCol.CheckMark, cm, cm, cm, 1f);
        set(s, ImGuiCol.SliderGrab, light ? 0.50f : 0.45f, light ? 0.50f : 0.45f, light ? 0.50f : 0.50f, 1f);
        set(s, ImGuiCol.SliderGrabActive, light ? 0.35f : 0.55f, light ? 0.35f : 0.55f, light ? 0.35f : 0.60f, 1f);

        float pl = light ? 0.30f : 0.70f;
        set(s, ImGuiCol.PlotLines, pl, pl, pl, 1f);
        set(s, ImGuiCol.PlotHistogram, ACCENT);
    }

    /**
     * Apply with the default Night mode.
     */
    public static void apply() {
        apply(Mode.NIGHT);
    }

    /**
     * Toggle between Light and Night.
     */
    public static void toggle() {
        apply(currentMode == Mode.LIGHT ? Mode.NIGHT : Mode.LIGHT);
    }

    // ── helpers ──────────────────────────────────────────────────────────
    private static void set(ImGuiStyle s, int col, float[] rgba) {
        s.setColor(col, rgba[0], rgba[1], rgba[2], rgba[3]);
    }

    private static void set(ImGuiStyle s, int col,
            float r, float g, float b, float a) {
        s.setColor(col, r, g, b, a);
    }
}
