package com.autoporter;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Scans mod Java source files and applies API change rules for a version transition.
 * Handles renames, import changes, method signature updates, and mixin target changes.
 */
public class SourcePatcher {

    public PatchResult patch(Path modRoot, String fromVersion, String toVersion, boolean dryRun) throws IOException {
        List<ApiChangeRule> rules = buildRuleChain(fromVersion, toVersion);
        if (rules.isEmpty()) {
            System.out.println("  [WARN] No source rules found for " + fromVersion + " → " + toVersion);
            return PatchResult.success("No API rules for " + fromVersion + " → " + toVersion, List.of());
        }

        List<Path> sources = findJavaSources(modRoot);
        List<String> allChanges = new ArrayList<>();

        for (Path src : sources) {
            List<String> fileChanges = patchFile(src, rules, dryRun, fromVersion, toVersion);
            allChanges.addAll(fileChanges);
            if (fileChanges.isEmpty()) {
                System.out.println("  [SKIP] " + src.getFileName() + " — no matching rules");
            }
        }

        String prefix = dryRun ? "[DRY RUN] Would apply" : "Source patched:";
        return PatchResult.success(
            prefix + " " + allChanges.size() + " change(s) across " + sources.size() + " file(s)",
            allChanges
        );
    }

    /** Back-compat overload for callers that don't pass dryRun. */
    public PatchResult patch(Path modRoot, String fromVersion, String toVersion) throws IOException {
        return patch(modRoot, fromVersion, toVersion, false);
    }

    /**
     * Build the full chain of rules needed to get from fromVersion to toVersion.
     * Applies rules from all intermediate versions in order.
     */
    private List<ApiChangeRule> buildRuleChain(String fromVersion, String toVersion) {
        List<String> allVersions = VersionDatabase.allVersions();
        int fromIdx = allVersions.indexOf(fromVersion);
        int toIdx   = allVersions.indexOf(toVersion);
        if (fromIdx < 0 || toIdx < 0) return List.of();

        List<ApiChangeRule> rules = new ArrayList<>();
        if (fromIdx <= toIdx) {
            // Upgrading
            for (int i = fromIdx; i < toIdx; i++) {
                rules.addAll(ApiChangeRule.forTransition(allVersions.get(i), allVersions.get(i + 1)));
            }
        } else {
            // Downgrading
            for (int i = fromIdx; i > toIdx; i--) {
                rules.addAll(ApiChangeRule.forTransition(allVersions.get(i), allVersions.get(i - 1)));
            }
        }
        // Also add direct rules (for large jumps documented directly)
        rules.addAll(ApiChangeRule.forTransition(fromVersion, toVersion));
        return deduplicateRules(rules);
    }

    private List<ApiChangeRule> deduplicateRules(List<ApiChangeRule> rules) {
        Set<String> seen = new LinkedHashSet<>();
        List<ApiChangeRule> result = new ArrayList<>();
        for (ApiChangeRule r : rules) {
            String key = r.type() + "|" + r.oldPattern();
            if (seen.add(key)) result.add(r);
        }
        return result;
    }

    private List<Path> findJavaSources(Path root) throws IOException {
        List<Path> results = new ArrayList<>();
        List<String> sourceDirs = List.of(
            "src/main/java", "common/src/main/java",
            "fabric/src/main/java", "neoforge/src/main/java"
        );
        for (String dir : sourceDirs) {
            Path p = root.resolve(dir);
            if (Files.exists(p)) {
                try (Stream<Path> walk = Files.walk(p)) {
                    walk.filter(f -> f.toString().endsWith(".java")).forEach(results::add);
                }
            }
        }
        return results;
    }

    private List<String> patchFile(Path file, List<ApiChangeRule> rules, boolean dryRun,
                                    String fromVersion, String toVersion) throws IOException {
        String original = Files.readString(file);
        String content  = original;
        List<String> changes = new ArrayList<>();

        // Phase 1: apply text-replacement rules (class renames, import changes, etc.)
        for (ApiChangeRule rule : rules) {
            String before = content;
            content = applyRule(content, rule);
            if (!content.equals(before)) {
                String change = file.getFileName() + ": [" + rule.type() + "] " + rule.description();
                changes.add(change);
                System.out.println("  [INFO] " + change);
            }
        }

        // Phase 2: translate JVM-format mixin target strings (e.g. @At(target = "Lnet/.../Class;method(...)V"))
        // Must run AFTER Phase 1 so that class renames in descriptors are already applied.
        MixinTargetResolver mixinResolver = new MixinTargetResolver();
        String afterMixin = mixinResolver.translateFile(content, fromVersion, toVersion);
        if (!afterMixin.equals(content)) {
            content = afterMixin;
            String change = file.getFileName() + ": [MIXIN_TARGET] Translated JVM mixin @At target descriptors";
            changes.add(change);
            System.out.println("  [INFO] " + change);
        }

        if (!content.equals(original)) {
            if (dryRun) {
                System.out.println("  [DRY RUN] Would modify: " + file.getFileName());
            } else {
                Files.writeString(file, content);
            }
        }
        return changes;
    }

    private String applyRule(String content, ApiChangeRule rule) {
        return switch (rule.type()) {
            case TEXT_REPLACE, CLASS_RENAME, METHOD_RENAME, FIELD_RENAME,
                 IMPORT_CHANGE, METHOD_SIGNATURE, MIXIN_TARGET ->
                content.replace(rule.oldPattern(), rule.newPattern());
        };
    }
}
