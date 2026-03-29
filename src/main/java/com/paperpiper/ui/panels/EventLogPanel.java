package com.paperpiper.ui.panels;

import java.util.ArrayList;
import java.util.List;

import imgui.ImGui;

public class EventLogPanel {

    private static final int MAX_EVENTS = 500;
    private final List<String> events = new ArrayList<>();
    private boolean scrollToBottom = false;

    public void addEvent(float simTime, String message) {
        int mins = (int) (simTime / 60);
        float secs = simTime - mins * 60;
        events.add(String.format("[%02d:%05.2f] %s", mins, secs, message));
        if (events.size() > MAX_EVENTS) {
            events.remove(0);
        }
        scrollToBottom = true;
    }

    public void render() {
        float footerH = ImGui.getFrameHeightWithSpacing();

        if (ImGui.beginChild("##EventLogScroll", 0, -footerH, false)) {
            for (String event : events) {
                ImGui.textWrapped(event);
            }
            if (scrollToBottom) {
                ImGui.setScrollHereY(1.0f);
                scrollToBottom = false;
            }
        }
        ImGui.endChild();

        if (ImGui.smallButton("Clear")) {
            events.clear();
        }
        ImGui.sameLine();
        ImGui.text(String.format("%d events", events.size()));
    }

    public void clear() {
        events.clear();
    }
}
