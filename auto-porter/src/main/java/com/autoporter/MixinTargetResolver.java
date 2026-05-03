package com.autoporter;

import java.util.*;

/**
 * Translates mixin {@code @At(target = "...")} JVM descriptor strings between MC versions.
 *
 * <p>Mixin injection points use the full JVM descriptor format inside {@code @At(target = ...)}:
 * <pre>
 *   "Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/item/ItemStack;III)V"
 * </pre>
 *
 * <p>When a class or method is renamed between MC versions both the class name inside {@code L...;}
 * and the method name must be updated. This class handles the <em>method-name</em> portion;
 * class renames inside descriptors are handled first by {@link ApiChangeRule} CLASS_RENAME rules
 * in {@link SourcePatcher} (Phase 1), so this resolver runs as Phase 2.
 *
 * <p><b>Execution order matters:</b> By the time {@link #translateFile} is called, all
 * CLASS_RENAME rules have already fired, so class names in the content already reflect the
 * target version. Each {@link MethodEntry#className()} must therefore use the
 * <em>post-class-rename</em> simple name.
 *
 * <p>For example, when upgrading 1.21.11 → 26.1:
 * <ol>
 *   <li>ApiChangeRule CLASS_RENAME: {@code GuiGraphics} → {@code GuiGraphicsExtractor}</li>
 *   <li>This resolver finds {@code GuiGraphicsExtractor;renderItem(} and renames
 *       {@code renderItem} → {@code item}</li>
 * </ol>
 *
 * <h3>Sources</h3>
 * <ul>
 *   <li>NeoForge 26.1 announcement: https://neoforged.net/news/26.1release/</li>
 *   <li>Fabric 1.21.9 announcement: https://fabricmc.net/2025/09/23/1219.html</li>
 * </ul>
 */
public class MixinTargetResolver {

    /**
     * A single known method rename at a specific version boundary.
     *
     * @param fromVersion source MC version (what the mod was written for)
     * @param toVersion   target MC version (what we're porting to)
     * @param className   simple class name AS IT APPEARS after Phase 1 class renames have been applied
     * @param fromMethod  method name found in the (post-class-rename) source content
     * @param toMethod    method name that should appear in the ported target content
     */
    private record MethodEntry(
        String fromVersion,
        String toVersion,
        String className,
        String fromMethod,
        String toMethod
    ) {}

    private static final List<MethodEntry> ENTRIES = buildEntries();

    private static List<MethodEntry> buildEntries() {
        List<MethodEntry> list = new ArrayList<>();

        // ── 1.21.11 → 26.1 ───────────────────────────────────────────────────────────────────
        // Source: https://neoforged.net/news/26.1release/ (GUI rendering pipeline refactoring)
        //
        // When upgrading: ApiChangeRule CLASS_RENAME already converted GuiGraphics → GuiGraphicsExtractor.
        // So this resolver sees the POST-rename class names in method references.

        // CONFIRMED by NeoForge 26.1 blog:
        list.add(new MethodEntry("1.21.11", "26.1", "Screen",                  "render",           "extractRenderState"));
        list.add(new MethodEntry("1.21.11", "26.1", "Screen",                  "renderBackground", "extractBackground"));
        list.add(new MethodEntry("1.21.11", "26.1", "AbstractContainerScreen", "renderBg",         "extractBackground"));
        list.add(new MethodEntry("1.21.11", "26.1", "AbstractContainerScreen", "renderLabels",     "extractLabels"));

        // INFERRED — consistent with render* → extract* pattern, not explicitly listed in official blog:
        list.add(new MethodEntry("1.21.11", "26.1", "AbstractContainerScreen", "renderSlot",       "extractSlot"));
        // GuiGraphicsExtractor (was GuiGraphics) — renderItem renamed to 'item' in 26.1:
        list.add(new MethodEntry("1.21.11", "26.1", "GuiGraphicsExtractor",    "renderItem",       "item"));

        // ── 26.1 → 1.21.11 ───────────────────────────────────────────────────────────────────
        // When downgrading: ApiChangeRule CLASS_RENAME already converted GuiGraphicsExtractor → GuiGraphics.
        // So the post-rename class name to look for is GuiGraphics.

        // CONFIRMED (reverses of above):
        list.add(new MethodEntry("26.1", "1.21.11", "Screen",                  "extractRenderState", "render"));
        list.add(new MethodEntry("26.1", "1.21.11", "Screen",                  "extractBackground",  "renderBackground"));
        list.add(new MethodEntry("26.1", "1.21.11", "AbstractContainerScreen", "extractBackground",  "renderBg"));
        list.add(new MethodEntry("26.1", "1.21.11", "AbstractContainerScreen", "extractLabels",      "renderLabels"));

        // INFERRED (reverses):
        list.add(new MethodEntry("26.1", "1.21.11", "AbstractContainerScreen", "extractSlot",        "renderSlot"));
        list.add(new MethodEntry("26.1", "1.21.11", "GuiGraphics",             "item",               "renderItem"));

        return Collections.unmodifiableList(list);
    }

    // ─────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Translate all mixin JVM-descriptor target strings in a Java source file.
     *
     * <p>Scans for {@code /ClassName;methodName(} (JVM format) and
     * {@code ClassName.methodName(} (dot format) occurrences and applies known
     * method renames for the version transition chain.
     *
     * @param content     full Java source file content, after Phase 1 ApiChangeRule rules
     * @param fromVersion MC version the mod was written for
     * @param toVersion   MC version we are porting to
     * @return modified content, or the original string unchanged if no renames applied
     */
    public String translateFile(String content, String fromVersion, String toVersion) {
        List<MethodEntry> applicable = collectApplicable(fromVersion, toVersion);
        if (applicable.isEmpty()) return content;

        for (MethodEntry entry : applicable) {
            content = applyRename(content, entry);
        }
        return content;
    }

    // ─────────────────────────────────────────────────────────────────────────────────────────

    /**
     * Collect all {@link MethodEntry} items applicable for the full fromVersion → toVersion
     * transition by walking each adjacent hop in the version chain.
     */
    private List<MethodEntry> collectApplicable(String fromVersion, String toVersion) {
        List<String> versions = VersionDatabase.allVersions();
        int fromIdx = versions.indexOf(fromVersion);
        int toIdx   = versions.indexOf(toVersion);
        if (fromIdx < 0 || toIdx < 0) return List.of();

        List<MethodEntry> result = new ArrayList<>();
        if (fromIdx <= toIdx) {
            for (int i = fromIdx; i < toIdx; i++) {
                addForHop(result, versions.get(i), versions.get(i + 1));
            }
        } else {
            for (int i = fromIdx; i > toIdx; i--) {
                addForHop(result, versions.get(i), versions.get(i - 1));
            }
        }
        // Also check for a direct cross-version entry that covers the whole jump
        addForHop(result, fromVersion, toVersion);
        return result;
    }

    private void addForHop(List<MethodEntry> result, String from, String to) {
        for (MethodEntry e : ENTRIES) {
            if (e.fromVersion().equals(from) && e.toVersion().equals(to) && !result.contains(e)) {
                result.add(e);
            }
        }
    }

    /**
     * Apply a single {@link MethodEntry} rename to the content string.
     *
     * <p>Handles two target string formats:
     * <ul>
     *   <li><b>JVM descriptor:</b> {@code Lnet/minecraft/pkg/ClassName;methodName(...)}</li>
     *   <li><b>Dot notation:</b>  {@code ClassName.methodName(...)}</li>
     * </ul>
     */
    private String applyRename(String content, MethodEntry entry) {
        // JVM format: "/ClassName;fromMethod(" → "/ClassName;toMethod("
        String jvmFrom = "/" + entry.className() + ";" + entry.fromMethod() + "(";
        String jvmTo   = "/" + entry.className() + ";" + entry.toMethod()   + "(";
        content = content.replace(jvmFrom, jvmTo);

        // Dot format: "ClassName.fromMethod(" → "ClassName.toMethod("
        String dotFrom = entry.className() + "." + entry.fromMethod() + "(";
        String dotTo   = entry.className() + "." + entry.toMethod()   + "(";
        content = content.replace(dotFrom, dotTo);

        return content;
    }
}
