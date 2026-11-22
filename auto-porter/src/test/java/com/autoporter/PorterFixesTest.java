package com.autoporter;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Regression tests for the path filter, wide-range rule chaining and the 1.19.0 typo. */
class PorterFixesTest {
    @TempDir Path temp;

    // ── Path filter: "build" anywhere in the absolute path must not hide project files ──────────────

    @Test void generatedFolderCheckOnlyLooksInsideTheProject() {
        Path root = temp.resolve("builds/buildcraft-addon");
        assertFalse(ProjectPaths.isGenerated(root, root.resolve("src/main/resources/fabric.mod.json")));
        assertFalse(ProjectPaths.isGenerated(root, root.resolve("src/main/java/builders/Thing.java")));
        assertTrue(ProjectPaths.isGenerated(root, root.resolve("build/resources/main/fabric.mod.json")));
        assertTrue(ProjectPaths.isGenerated(root, root.resolve("fabric/.gradle/cache/x.json")));
    }

    @Test void metadataIsPatchedWhenTheProjectLivesUnderABuildsFolder() throws Exception {
        Path root = temp.resolve("builds/mymod");
        Path fmj = root.resolve("src/main/resources/fabric.mod.json");
        Path generated = root.resolve("build/resources/main/fabric.mod.json");
        for (Path file : List.of(fmj, generated)) {
            Files.createDirectories(file.getParent());
            Files.writeString(file, "{\"id\":\"mymod\",\"depends\":{\"minecraft\":\"~1.20.1\"}}");
        }

        new ModMetaPatcher().patch(root, "1.21.1");

        assertTrue(Files.readString(fmj).contains("~1.21.1"), "source fabric.mod.json must be updated");
        assertTrue(Files.readString(generated).contains("~1.20.1"), "files in build/ must be left alone");
    }

    @Test void packFormatAndWarningsWorkUnderABuildsFolder() throws Exception {
        Path root = temp.resolve("my-builds/mymod");
        Path resources = root.resolve("src/main/resources");
        Files.createDirectories(resources.resolve("assets/mymod"));
        Files.writeString(resources.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":84,\"description\":\"x\"}}");
        Path java = root.resolve("src/main/java/example/Draw.java");
        Files.createDirectories(java.getParent());
        Files.writeString(java, "class Draw { Tesselator t; }");

        new ResourcePatcher().patch(root, "26.1.2", "26.2", false);
        PatchResult warnings = new MigrationWarningScanner().scan(root, "26.1.2", "26.2");

        assertTrue(Files.readString(resources.resolve("pack.mcmeta")).contains("88"));
        assertTrue(warnings.changes().stream().anyMatch(w -> w.contains("Tesselator")));
    }

    // ── Rule chaining: wide-range rule groups apply to every port that passes through them ─────────

    private static boolean hasRule(List<ApiChangeRule> rules, String from, String to) {
        return rules.stream().anyMatch(r -> r.fromVersion().equals(from) && r.toVersion().equals(to));
    }

    @Test void wideRangeRulesApplyToLongerPorts() {
        assertTrue(hasRule(SourcePatcher.buildRuleChain("1.19.4", "1.20.1"), "1.19.4", "1.20.1"), "exact pair");
        assertTrue(hasRule(SourcePatcher.buildRuleChain("1.19.2", "1.20.1"), "1.19.4", "1.20.1"), "starts earlier");
        assertTrue(hasRule(SourcePatcher.buildRuleChain("1.19.4", "1.21"), "1.19.4", "1.20.1"), "ends later");
        assertTrue(hasRule(SourcePatcher.buildRuleChain("1.20.4", "1.19.2"), "1.20.1", "1.19.4"), "downgrade");
    }

    @Test void wideRangeRulesDoNotLeakOutsideTheirRange() {
        assertFalse(hasRule(SourcePatcher.buildRuleChain("1.19.2", "1.19.4"), "1.19.4", "1.20.1"), "ends too early");
        assertFalse(hasRule(SourcePatcher.buildRuleChain("1.20", "1.20.4"), "1.19.4", "1.20.1"), "starts too late");
        assertFalse(hasRule(SourcePatcher.buildRuleChain("1.20.1", "1.19.2"), "1.19.4", "1.20.1"), "wrong direction");
        assertTrue(SourcePatcher.buildRuleChain("1.20.1", "1.20.1").isEmpty());
    }

    @Test void guiGraphicsRulesConvertCodeInA1_19_2To1_20_1Port() throws Exception {
        Path root = temp.resolve("mod");
        Path src = root.resolve("src/main/java/example/MyScreen.java");
        Files.createDirectories(src.getParent());
        Files.writeString(src, """
            package example;
            import com.mojang.blaze3d.vertex.PoseStack;
            class MyScreen {
                public void render(PoseStack poseStack, int mouseX, int mouseY, float delta) {
                    super.render(poseStack, mouseX, mouseY, delta);
                }
            }
            """);

        new SourcePatcher().patch(root, "1.19.2", "1.20.1", false);
        String out = Files.readString(src);

        assertTrue(out.contains("import net.minecraft.client.gui.GuiGraphics;"), out);
        assertTrue(out.contains("render(GuiGraphics graphics,"), out);
        assertTrue(out.contains("super.render(graphics, mouseX, mouseY, delta);"), out);
    }

    // ── 1.19.0 typo: these rules now belong to 1.19, a real version ─────────────────────────────────

    @Test void rulesUseOnlyKnownVersions() {
        List<String> known = VersionDatabase.allVersions();
        for (ApiChangeRule rule : ApiChangeRule.all()) {
            assertTrue(known.contains(rule.fromVersion()), "unknown version " + rule.fromVersion() + " in " + rule);
            assertTrue(known.contains(rule.toVersion()), "unknown version " + rule.toVersion() + " in " + rule);
        }
    }

    @Test void the1_18_2To1_19RulesNowRun() throws Exception {
        Path root = temp.resolve("mod");
        Path src = root.resolve("src/main/java/example/Chat.java");
        Files.createDirectories(src.getParent());
        Files.writeString(src, "class Chat { Object c = new net.minecraft.network.chat.TextComponent(\"hi\"); }");

        new SourcePatcher().patch(root, "1.18.2", "1.19", false);

        assertTrue(Files.readString(src).contains("Component.literal(\"hi\")"));
    }
}
