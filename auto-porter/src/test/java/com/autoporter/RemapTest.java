package com.autoporter;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Yarn → Mojang remapping: mapping files, tokenizer safety and source rewriting. */
class RemapTest {
    @TempDir Path temp;

    /** A tiny slice of real 1.21.1 names (official = obfuscated). */
    static final String TINY = String.join("\n",
        "tiny\t2\t0\tofficial\tintermediary\tnamed",
        "c\tfhe\tnet/minecraft/class_310\tnet/minecraft/client/MinecraftClient",
        "\tf\tLfkg;\tr\tfield_1724\tplayer",
        "\tm\t()V\ta\tmethod_1490\tsetScreen",
        "c\tfkg\tnet/minecraft/class_746\tnet/minecraft/client/network/ClientPlayerEntity",
        "c\tfhz\tnet/minecraft/class_332\tnet/minecraft/client/gui/DrawContext",
        "\tm\t(IIIII)V\tc\tmethod_25294\tfill",
        "c\tamo\tnet/minecraft/class_2960\tnet/minecraft/util/Identifier",
        "\tm\t(Ljava/lang/String;)Lamo;\tb\tmethod_60654\tof",
        "c\tdkg\tnet/minecraft/class_4970\tnet/minecraft/block/AbstractBlock",
        "c\tdkg$d\tnet/minecraft/class_4970$class_2251\tnet/minecraft/block/AbstractBlock$Settings",
        "\tm\t(F)Ldkg$d;\ta\tmethod_9632\tstrength",
        "c\tfqd\tnet/minecraft/class_437\tnet/minecraft/client/gui/screen/Screen",
        "\tm\t(Lfhz;IIF)V\ta\tmethod_25394\trender",
        "\tf\tLfhe;\tl\tfield_22787\tclient",
        "") + "\n";

    static final String PROGUARD = String.join("\n",
        "# {\"fileName\":\"client.txt\"}",
        "net.minecraft.client.Minecraft -> fhe:",
        "    net.minecraft.client.player.LocalPlayer player -> r",
        "    void setScreen() -> a",
        "net.minecraft.client.player.LocalPlayer -> fkg:",
        "net.minecraft.client.gui.GuiGraphics -> fhz:",
        "    1:4:void fill(int,int,int,int,int):10:13 -> c",
        "net.minecraft.resources.ResourceLocation -> amo:",
        "    net.minecraft.resources.ResourceLocation parse(java.lang.String) -> b",
        "net.minecraft.world.level.block.state.BlockBehaviour -> dkg:",
        "net.minecraft.world.level.block.state.BlockBehaviour$Properties -> dkg$d:",
        "    net.minecraft.world.level.block.state.BlockBehaviour$Properties strength(float) -> a",
        "net.minecraft.client.gui.screens.Screen -> fqd:",
        "    void render(net.minecraft.client.gui.GuiGraphics,int,int,float) -> a",
        "    net.minecraft.client.Minecraft minecraft -> l",
        "") + "\n";

    static MappingTable table() throws Exception {
        return MappingTable.join(new StringReader(TINY), List.of(new StringReader(PROGUARD)));
    }

    @Test void joinsYarnAndMojangOnObfuscatedNames() throws Exception {
        MappingTable t = table();
        assertEquals("net/minecraft/client/Minecraft", t.mojangClass("net/minecraft/client/MinecraftClient"));
        assertEquals("net/minecraft/client/gui/GuiGraphics", t.mojangClass("net/minecraft/client/gui/DrawContext"));
        assertEquals("net/minecraft/world/level/block/state/BlockBehaviour$Properties",
            t.mojangClass("net/minecraft/block/AbstractBlock$Settings"));
        assertEquals("parse", t.uniqueMethod("of"), "method descriptors are matched through the class map");
        assertEquals("minecraft", t.uniqueField("client"));
        assertNull(t.uniqueMethod("render"), "same name in both → nothing to do");
        assertNull(t.uniqueField("player"));
    }

    @Test void overloadsWithDifferentMojangNamesAreReported() throws Exception {
        MappingTable t = table()
            .addMethod("net/minecraft/util/Identifier", "of", "fromNamespaceAndPath");
        String src = "import net.minecraft.util.Identifier;\nclass A { Object x = Identifier.of(\"a\", \"b\"); }\n";
        SourceRemapper.FileResult r = SourceRemapper.remap(src, t, Set.of());
        assertTrue(r.content().contains("ResourceLocation.of(\"a\", \"b\")"), r.content());
        assertTrue(r.warnings().stream().anyMatch(w -> w.contains("Identifier.of(…) has several Mojang names [fromNamespaceAndPath, parse]")),
            r.warnings().toString());
    }

    @Test void consoleFallsBackToAsciiOnNonUtf8CodePages() {
        assertEquals("[3/9] Remapping (Yarn -> Mojang)... OK", ConsoleText.toAscii("[3/9] Remapping (Yarn → Mojang)... ✓"));
        assertEquals("  - a.java: of(...) - pick one", ConsoleText.toAscii("  • a.java: of(…) — pick one"));
    }

    @Test void ambiguousMemberNamesAreNotRemapped() {
        MappingTable t = new MappingTable().addMethod("tick", "tick").addMethod("tick", "baseTick").addMethod("getPos", "blockPosition");
        assertNull(t.uniqueMethod("tick"));
        assertEquals("blockPosition", t.uniqueMethod("getPos"));
    }

    @Test void tokenizerSeparatesCodeFromStringsAndComments() {
        String src = "a.b(\"Screen\") // Screen\n/* Screen */ char c = 'S'; String t = \"\"\"\nScreen\n\"\"\"; Screen s;";
        List<String> idents = JavaTokenizer.tokenize(src).stream()
            .filter(t -> t.kind() == JavaTokenizer.Kind.IDENT).map(JavaTokenizer.Token::text).toList();
        assertEquals(List.of("a", "b", "char", "c", "String", "t", "Screen", "s"), idents);
    }

    static final String YARN_SCREEN = """
        package example;

        import net.minecraft.client.MinecraftClient;
        import net.minecraft.client.gui.DrawContext;
        import net.minecraft.client.gui.screen.Screen;
        import net.minecraft.util.Identifier;
        import net.minecraft.block.AbstractBlock;

        // DrawContext and MinecraftClient in a comment stay as they are
        public class MyScreen extends Screen {
            private static final Identifier ID = Identifier.of("mymod:icon");
            private final List<String> names = new ArrayList<>();

            @Override
            public void render(DrawContext context, int mouseX, int mouseY, float delta) {
                context.fill(0, 0, 10, 10, 0xFF000000);
                MinecraftClient mc = this.client;
                names.add("DrawContext");
                AbstractBlock.Settings settings = AbstractBlock.Settings.create().strength(1.0f);
                net.minecraft.client.gui.DrawContext full = context;
            }
        }
        """;

    @Test void remapsClassesNestedClassesAndUniqueMembersButNotStringsOrComments() throws Exception {
        SourceRemapper.FileResult r = SourceRemapper.remap(YARN_SCREEN, table(), SourceRemapper.declaredNames(List.of(YARN_SCREEN)));
        String out = r.content();
        assertTrue(out.contains("import net.minecraft.client.Minecraft;"), out);
        assertTrue(out.contains("import net.minecraft.client.gui.GuiGraphics;"), out);
        assertTrue(out.contains("import net.minecraft.client.gui.screens.Screen;"), out);
        assertTrue(out.contains("import net.minecraft.resources.ResourceLocation;"), out);
        assertTrue(out.contains("import net.minecraft.world.level.block.state.BlockBehaviour;"), out);
        assertTrue(out.contains("ResourceLocation ID = ResourceLocation.parse(\"mymod:icon\")"), out);
        assertTrue(out.contains("public void render(GuiGraphics context"), out);
        assertTrue(out.contains("Minecraft mc = this.minecraft;"), out);
        assertTrue(out.contains("BlockBehaviour.Properties settings = BlockBehaviour.Properties.create().strength(1.0f);"), out);
        assertTrue(out.contains("net.minecraft.client.gui.GuiGraphics full = context;"), out);
        assertTrue(out.contains("// DrawContext and MinecraftClient in a comment stay as they are"), out);
        assertTrue(out.contains("names.add(\"DrawContext\");"), "string literals are never changed");
        assertTrue(r.count() > 10);
    }

    @Test void modOwnMembersAndCommonJdkNamesAreNotRenamed() throws Exception {
        MappingTable t = table().addMethod("add", "addEntry").addField("count", "size");
        String src = """
            import net.minecraft.client.MinecraftClient;
            class Mine {
                int count;
                void of() { }
                void use(java.util.List<String> list, Mine other) { list.add("x"); other.count = 1; other.of(); }
            }
            """;
        String out = SourceRemapper.remap(src, t, SourceRemapper.declaredNames(List.of(src))).content();
        assertTrue(out.contains("list.add(\"x\")"), out);
        assertTrue(out.contains("other.count = 1"), out);
        assertTrue(out.contains("other.of()"), "the mod declares of() itself: " + out);
    }

    @Test void mixinShadowMembersAreRemappedAndTargetStringsReported() throws Exception {
        String src = """
            import net.minecraft.client.gui.screen.Screen;
            import org.spongepowered.asm.mixin.*;
            @Mixin(Screen.class)
            abstract class ScreenMixin {
                @Shadow protected MinecraftClient client;
                @Inject(method = "render", at = @At("HEAD"))
                private void onRender(CallbackInfo ci) { if (client != null) { } }
            }
            """;
        SourceRemapper.FileResult r = SourceRemapper.remap(src, table(), SourceRemapper.declaredNames(List.of(src)));
        assertTrue(r.content().contains("@Shadow protected MinecraftClient minecraft;"), r.content());
        assertTrue(r.content().contains("if (minecraft != null)"), r.content());
        assertTrue(r.content().contains("@Mixin(Screen.class)"));
        assertTrue(r.content().contains("import net.minecraft.client.gui.screens.Screen;"));
        assertTrue(r.warnings().stream().anyMatch(w -> w.contains("mixin target string")), r.warnings().toString());
    }

    @Test void kotlinSourcesAreRemappedToo() throws Exception {
        String src = """
            import net.minecraft.client.gui.DrawContext
            import net.minecraft.util.Identifier
            val ID: Identifier = Identifier.of("mymod:x")
            fun draw(context: DrawContext) { context.fill(0, 0, 1, 1, 0) }
            """;
        String out = SourceRemapper.remap(src, table(), Set.of()).content();
        assertTrue(out.contains("import net.minecraft.client.gui.GuiGraphics"), out);
        assertTrue(out.contains("val ID: ResourceLocation = ResourceLocation.parse(\"mymod:x\")"), out);
        assertTrue(out.contains("fun draw(context: GuiGraphics)"), out);
    }

    @Test void detectsYarnAndMojangProjects() throws Exception {
        Path yarn = temp.resolve("yarn");
        Files.createDirectories(yarn);
        Files.writeString(yarn.resolve("build.gradle"), "dependencies { mappings \"net.fabricmc:yarn:${project.yarn_mappings}:v2\" }");
        Files.writeString(yarn.resolve("gradle.properties"), "yarn_mappings=1.21.1+build.3\n");
        SourceRemapper.Detection d = SourceRemapper.detect(yarn, "1.21.1");
        assertEquals(SourceRemapper.Mappings.YARN, d.mappings());
        assertEquals("1.21.1+build.3", d.yarnBuild());

        Path moj = temp.resolve("moj");
        Files.createDirectories(moj);
        Files.writeString(moj.resolve("build.gradle"), "dependencies { mappings loom.officialMojangMappings() }");
        assertEquals(SourceRemapper.Mappings.MOJANG, SourceRemapper.detect(moj, "1.21.1").mappings());
    }

    @Test void remapProjectWritesFilesAndReports() throws Exception {
        Path root = temp.resolve("proj");
        Path src = root.resolve("src/main/java/example/MyScreen.java");
        Files.createDirectories(src.getParent());
        Files.writeString(src, YARN_SCREEN);
        PatchResult result = SourceRemapper.remapProject(root, table(), false);
        assertTrue(result.success());
        assertTrue(result.message().contains("in 1 of 1 source file"), result.message());
        assertTrue(Files.readString(src).contains("GuiGraphics"));
        assertTrue(result.changes().stream().anyMatch(c -> c.contains("DrawContext → GuiGraphics")), result.changes().toString());
    }

    @Test void remapPlanFollowsFlagsAndTargets() throws Exception {
        Path yarn = temp.resolve("plan");
        Files.createDirectories(yarn);
        Files.writeString(yarn.resolve("build.gradle"), "mappings \"net.fabricmc:yarn:1.21.1+build.3:v2\"");
        var auto = AutoPorterMain.Options.parse(List.of());
        assertTrue(AutoPorterMain.RemapPlan.decide(yarn, "1.21.1", "26.1.2", auto).remap(), "Yarn → 26.x remaps by default");
        assertFalse(AutoPorterMain.RemapPlan.decide(yarn, "1.21.1", "1.21.4", auto).remap(), "pre-26 targets keep Yarn");
        assertTrue(AutoPorterMain.RemapPlan.decide(yarn, "1.21.1", "1.21.4",
            AutoPorterMain.Options.parse(List.of("--remap"))).remap());
        assertFalse(AutoPorterMain.RemapPlan.decide(yarn, "1.21.1", "26.1.2",
            AutoPorterMain.Options.parse(List.of("--no-remap"))).remap());
    }
}
