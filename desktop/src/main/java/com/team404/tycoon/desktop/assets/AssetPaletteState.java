package com.team404.tycoon.desktop.assets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Selected PNG for placement, horizontal scroll for the top bar, asset list, and category filter.
 */
public class AssetPaletteState {

    public static final String CATEGORY_ROADS      = "roads";
    public static final String CATEGORY_STRUCTURES = "structures";
    public static final String CATEGORY_DECOR      = "decor";

    public static final String[] CATEGORIES       = {CATEGORY_ROADS, CATEGORY_STRUCTURES, CATEGORY_DECOR};
    public static final String[] CATEGORY_LABELS  = {"Roads", "Structures", "Decor"};

    private final List<String> resourcePaths = new ArrayList<>();
    private final Map<String, String> displayNames = new HashMap<>();
    private String selectedPath;
    private String hoveredPath;
    private String selectedCategory;
    private float scrollX;
    private boolean dropdownOpen;

    public AssetPaletteState(List<String> paths) {
        for (String path : paths) {
            if (!isExcluded(path)) {
                resourcePaths.add(path);
            }
        }
        Collections.sort(resourcePaths);
        initDisplayNames();
        this.selectedCategory = CATEGORY_ROADS;
    }

    // ── category filtering ────────────────────────────────────────────────────

    public String getSelectedCategory() {
        return selectedCategory;
    }

    public void setSelectedCategory(String category) {
        this.selectedCategory = category;
        this.scrollX = 0f;
    }

    public void toggleCategory(String category) {
        if (category != null && category.equals(selectedCategory)) {
            selectedCategory = null;
        } else {
            selectedCategory = category;
        }
        this.scrollX = 0f;
    }

    public boolean isDropdownOpen() {
        return dropdownOpen;
    }

    public void toggleDropdown() {
        this.dropdownOpen = !this.dropdownOpen;
    }

    public void closeDropdown() {
        this.dropdownOpen = false;
    }

    /** All non-excluded paths (full list regardless of category). */
    public List<String> getResourcePaths() {
        return Collections.unmodifiableList(resourcePaths);
    }

    /** Paths visible in the current category (used by the scrollable strip). */
    public List<String> getVisiblePaths() {
        if (selectedCategory == null) {
            return Collections.unmodifiableList(resourcePaths);
        }
        List<String> result = new ArrayList<>();
        for (String path : resourcePaths) {
            if (selectedCategory.equals(categoryOf(path))) {
                result.add(path);
            }
        }
        return Collections.unmodifiableList(result);
    }

    // ── selection ─────────────────────────────────────────────────────────────

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

    // ── scroll ────────────────────────────────────────────────────────────────

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
        int count = getVisiblePaths().size();
        float contentWidth = count * thumb + Math.max(0, count - 1) * gap;
        return Math.max(0f, contentWidth - visibleContentWidth);
    }

    // ── classification ────────────────────────────────────────────────────────

    private static boolean isExcluded(String path) {
        String n = basename(path).toLowerCase();
        // Navigation arrow sprites (used for route display, not placeable)
        if (n.contains("blue") || n.contains("arrow")) {
            return true;
        }
        // Raw terrain textures (not buildable items)
        if (n.equals("grass.png") || n.equals("groundgrey.png")
                || n.equals("asphalt.png") || n.equals("asphalt with sth.png")
                || n.equals("bigaphaslt.png") || n.equals("black asphalt.png")) {
            return true;
        }
        // Intersection PNGs replaced by directional corner/turn sprites
        if (n.contains("intersection")) {
            return true;
        }
        // Vehicle sprites (rendered programmatically, not placed by player)
        if (n.startsWith("car-")) {
            return true;
        }
        return false;
    }

    public static String categoryOf(String path) {
        String n = basename(path).toLowerCase();
        if (n.contains("highway") || n.contains("-and-")
                || n.contains("to-") || n.equals("+.png")
                || n.contains("trafficlights") || n.contains("traffic lights")) {
            return CATEGORY_ROADS;
        }
        if (n.contains("building") || n.contains("village") || n.contains("garage")
                || n.contains("teplitsa") || n.contains("playground")
                || n.contains("somegrey") || n.contains("pink")) {
            return CATEGORY_STRUCTURES;
        }
        if (n.contains("tree")) {
            return CATEGORY_DECOR;
        }
        // Uncategorised items fall into structures by default
        return CATEGORY_STRUCTURES;
    }

    // ── display names ─────────────────────────────────────────────────────────

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
