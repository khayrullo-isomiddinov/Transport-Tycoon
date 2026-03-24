package com.team404.tycoon.desktop.assets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Selected PNG for placement, horizontal scroll for the top bar, and asset list.
 */
public class AssetPaletteState {

    private final List<String> resourcePaths = new ArrayList<>();
    private final Map<String, String> displayNames = new HashMap<>();
    private String selectedPath;
    private String hoveredPath;
    private float scrollX;

    public AssetPaletteState(List<String> paths) {
        for (String path : paths) {
            String n = basename(path).toLowerCase();
            if ("upblue.png".equals(n) || "upbluearrow.png".equals(n)) {
                continue;
            }
            resourcePaths.add(path);
        }
        Collections.sort(resourcePaths);
        initDisplayNames();
    }

    public List<String> getResourcePaths() {
        return Collections.unmodifiableList(resourcePaths);
    }

    public String getSelectedPath() {
        return selectedPath;
    }

    public void setSelectedPath(String selectedPath) {
        this.selectedPath = selectedPath;
    }

    public void clearSelection() {
        this.selectedPath = null;
    }

    public String getHoveredPath() {
        return hoveredPath;
    }

    public void setHoveredPath(String hoveredPath) {
        this.hoveredPath = hoveredPath;
    }

    public String displayNameFor(String resourcePath) {
        if (resourcePath == null) {
            return "";
        }
        return displayNames.getOrDefault(resourcePath, prettifyName(resourcePath));
    }

    public float getScrollX() {
        return scrollX;
    }

    public void setScrollX(float scrollX) {
        this.scrollX = Math.max(0f, scrollX);
    }

    public void setScrollX(float scrollX, float maxScrollX) {
        this.scrollX = Math.max(0f, Math.min(Math.max(0f, maxScrollX), scrollX));
    }

    public void addScroll(float delta) {
        setScrollX(scrollX + delta);
    }

    public void addScroll(float delta, float maxScrollX) {
        setScrollX(scrollX + delta, maxScrollX);
    }

    public float maxScrollX(float visibleContentWidth, float thumb, float gap) {
        float contentWidth = resourcePaths.size() * thumb + Math.max(0, resourcePaths.size() - 1) * gap;
        return Math.max(0f, contentWidth - visibleContentWidth);
    }

    private void initDisplayNames() {
        for (String path : resourcePaths) {
            displayNames.put(path, prettifyName(path));
        }
        for (String path : resourcePaths) {
            String n = basename(path).toLowerCase();
            if ("somegreybuilding.png".equals(n)) {
                displayNames.put(path, "Cable Factory");
            }
        }

        List<String> buildingPaths = new ArrayList<>();
        for (String path : resourcePaths) {
            String n = basename(path).toLowerCase();
            if (n.matches("building\\d+\\.png")) {
                buildingPaths.add(path);
            }
        }
        if (buildingPaths.isEmpty()) {
            return;
        }

        List<String> friendly = new ArrayList<>(Arrays.asList(
                "Apartment Block",
                "Town Hall",
                "Civic Center",
                "Market Hall",
                "Office Tower",
                "Residential Tower",
                "City Plaza",
                "Business Hub",
                "Community Hall",
                "Grand Hotel"
        ));
        Collections.shuffle(friendly, new Random(System.nanoTime()));
        for (int i = 0; i < buildingPaths.size(); i++) {
            String p = buildingPaths.get(i);
            String label = friendly.get(i % friendly.size());
            if (i >= friendly.size()) {
                label = label + " " + (1 + (i / friendly.size()));
            }
            displayNames.put(p, label);
        }
    }

    private static String prettifyName(String path) {
        String n = basename(path);
        int dot = n.lastIndexOf('.');
        if (dot > 0) {
            n = n.substring(0, dot);
        }
        n = n.replace('_', ' ').replace('-', ' ').trim();
        String[] parts = n.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            if (part.length() == 1) {
                sb.append(part.toUpperCase());
            } else {
                sb.append(Character.toUpperCase(part.charAt(0)));
                sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }

    private static String basename(String path) {
        int slash = path.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < path.length()) {
            return path.substring(slash + 1);
        }
        return path;
    }
}
