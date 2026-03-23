package com.team404.tycoon.desktop.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Resolves {@code resources/*.png} for both packaged runs (internal) and IDE runs (repo folder).
 */
public final class ResourceFileHandles {

    private static File cachedResourcesDir;

    private ResourceFileHandles() {
    }

    /**
     * Finds {@code team-404/resources} (or {@code resources}) relative to {@code user.dir}.
     */
    public static File findResourcesDirectoryOnDisk() {
        if (cachedResourcesDir != null && cachedResourcesDir.isDirectory()) {
            return cachedResourcesDir;
        }
        String userDir = System.getProperty("user.dir", ".");
        File[] roots = {
                new File(userDir, "resources"),
                new File(userDir, "../resources"),
                new File(userDir, "../../resources"),
        };
        for (File root : roots) {
            File abs = root.getAbsoluteFile();
            if (!abs.isDirectory()) {
                continue;
            }
            File[] pngs = abs.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".png"));
            if (pngs != null && pngs.length > 0) {
                cachedResourcesDir = abs;
                return abs;
            }
        }
        return null;
    }

    public static FileHandle resolve(String classpathRelativePath) {
        FileHandle internal = Gdx.files.internal(classpathRelativePath);
        if (internal.exists()) {
            return internal;
        }
        String name = stripResourcesPrefix(classpathRelativePath);
        File dir = findResourcesDirectoryOnDisk();
        if (dir != null) {
            File abs = new File(dir, name);
            if (abs.isFile()) {
                return Gdx.files.absolute(abs.getAbsolutePath());
            }
        }
        return internal;
    }

    public static List<String> listPngPathsFromDisk() {
        File dir = findResourcesDirectoryOnDisk();
        if (dir == null) {
            return Collections.emptyList();
        }
        File[] files = dir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".png"));
        if (files == null) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        for (File f : files) {
            out.add("resources/" + f.getName());
        }
        Collections.sort(out);
        return out;
    }

    public static List<String> listPngPathsFromManifest() {
        FileHandle mf = Gdx.files.internal("resources/asset-manifest.txt");
        if (!mf.exists()) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        for (String line : mf.readString().split("\\R")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (!line.toLowerCase(Locale.ROOT).endsWith(".png")) {
                continue;
            }
            out.add(line.contains("/") ? line : "resources/" + line);
        }
        Collections.sort(out);
        return out;
    }

    private static String stripResourcesPrefix(String classpathRelativePath) {
        String p = classpathRelativePath;
        if (p.startsWith("resources/")) {
            return p.substring("resources/".length());
        }
        return p;
    }
}
