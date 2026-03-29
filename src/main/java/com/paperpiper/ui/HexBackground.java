package com.paperpiper.ui;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;

public class HexBackground {

    private static final float HEX_SIZE = 30.0f;

    public static void draw(ImDrawList drawList, float x0, float y0, float x1, float y1) {
        float w = HEX_SIZE * (float) Math.sqrt(3);
        float h = HEX_SIZE * 2.0f;
        float rowH = h * 0.75f;

        int row = 0;
        for (float cy = y0; cy - HEX_SIZE < y1; cy += rowH, row++) {
            float offsetX = (row % 2 == 1) ? w * 0.5f : 0;
            for (float cx = x0 + offsetX; cx - w < x1; cx += w) {
                drawHexTriangles(drawList, cx, cy, row);
            }
        }
    }

    private static void drawHexTriangles(ImDrawList dl, float cx, float cy, int row) {
        float[] vx = new float[6];
        float[] vy = new float[6];
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI / 6 + i * Math.PI / 3;
            vx[i] = cx + HEX_SIZE * (float) Math.cos(angle);
            vy[i] = cy + HEX_SIZE * (float) Math.sin(angle);
        }

        for (int i = 0; i < 6; i++) {
            int next = (i + 1) % 6;
            int col = ((row + i) % 2 == 0) ? UITheme.HEX_FILL_A : UITheme.HEX_FILL_B;
            dl.addTriangleFilled(cx, cy, vx[i], vy[i], vx[next], vy[next], col);
            dl.addTriangle(cx, cy, vx[i], vy[i], vx[next], vy[next], UITheme.HEX_EDGE, 1.0f);
        }
    }

    public static void drawCurrentWindow() {
        ImDrawList dl = ImGui.getWindowDrawList();
        ImVec2 min = new ImVec2();
        ImVec2 max = new ImVec2();
        ImGui.getWindowPos(min);
        ImGui.getWindowSize(max);
        draw(dl, min.x, min.y, min.x + max.x, min.y + max.y);
    }
}
