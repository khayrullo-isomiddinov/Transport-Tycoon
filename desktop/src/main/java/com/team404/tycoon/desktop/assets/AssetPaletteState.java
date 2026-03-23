package com.team404.tycoon.desktop.assets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Selected PNG for placement, horizontal scroll for the top bar, and asset list.
 */
public class AssetPaletteState {

    private final List<String> resourcePaths = new ArrayList<>();
    private String selectedPath;
    private float scrollX;

    public AssetPaletteState(List<String> paths) {
        resourcePaths.addAll(paths);
        Collections.sort(resourcePaths);
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

    public float getScrollX() {
        return scrollX;
    }

    public void setScrollX(float scrollX) {
        this.scrollX = Math.max(0f, scrollX);
    }

    public void addScroll(float delta) {
        setScrollX(scrollX + delta);
    }
}
