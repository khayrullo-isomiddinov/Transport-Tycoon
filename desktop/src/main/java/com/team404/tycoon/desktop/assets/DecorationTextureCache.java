package com.team404.tycoon.desktop.assets;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads and caches textures (white→transparent) for resource paths.
 */
public class DecorationTextureCache implements Disposable {

    private final Map<String, Texture> cache = new HashMap<>();

    public Texture get(String classpathRelativePath) {
        return cache.computeIfAbsent(classpathRelativePath, p -> {
            FileHandle fh = ResourceFileHandles.resolve(p);
            if (!fh.exists()) {
                throw new IllegalArgumentException("Missing resource: " + p + " (internal + ../resources fallback)");
            }
            return TextureWhiteToTransparent.load(fh);
        });
    }

    public boolean has(String classpathRelativePath) {
        return cache.containsKey(classpathRelativePath);
    }

    @Override
    public void dispose() {
        for (Texture t : cache.values()) {
            t.dispose();
        }
        cache.clear();
    }
}
