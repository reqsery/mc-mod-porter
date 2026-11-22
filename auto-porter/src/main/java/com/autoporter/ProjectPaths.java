package com.autoporter;

import java.nio.file.Path;

/** Shared path helpers for walking a mod project. */
final class ProjectPaths {
    private ProjectPaths() {}

    /**
     * True when {@code file} sits inside a generated folder of the project — a {@code build} or {@code .gradle}
     * directory <em>below</em> {@code root}.
     *
     * <p>Only path segments relative to the project root are checked, and only exact folder names. Folders above the
     * project (e.g. {@code C:/builds/mymod}) and names that merely contain the word (e.g. {@code builders/},
     * {@code buildcraft-compat/}) do not count.
     */
    static boolean isGenerated(Path root, Path file) {
        Path relative;
        try {
            relative = root.toAbsolutePath().normalize().relativize(file.toAbsolutePath().normalize());
        } catch (IllegalArgumentException differentRoot) {
            return false;
        }
        for (Path segment : relative) {
            String name = segment.toString();
            if (name.equals("build") || name.equals(".gradle")) return true;
        }
        return false;
    }
}
