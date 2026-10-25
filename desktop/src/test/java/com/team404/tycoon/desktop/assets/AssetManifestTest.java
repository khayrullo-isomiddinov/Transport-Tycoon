package com.team404.tycoon.desktop.assets;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Packaged builds can't list classpath directories, so the game falls back to
 * {@code asset-manifest.txt}. Keep it in sync with {@code resources/}.
 */
class AssetManifestTest {

    private static final Path MANIFEST = Paths.get("src/main/resources/resources/asset-manifest.txt");
    private static final Path RESOURCES_DIR = Paths.get("../resources");

    @Test
    void manifestMatchesResourcesDirectory() throws IOException {
        Set<String> manifest = new TreeSet<>();
        for (String line : Files.readAllLines(MANIFEST, StandardCharsets.UTF_8)) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
                manifest.add(line);
            }
        }

        Set<String> onDisk;
        try (Stream<Path> files = Files.list(RESOURCES_DIR)) {
            onDisk = files
                    .map(p -> p.getFileName().toString())
                    .filter(n -> n.toLowerCase(Locale.ROOT).endsWith(".png"))
                    .collect(Collectors.toCollection(TreeSet::new));
        }

        assertEquals(onDisk, manifest,
                "asset-manifest.txt is out of sync with resources/: list every PNG in resources/, one per line");
    }
}
