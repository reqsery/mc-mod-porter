package com.autoporter;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/** Reports 26.2 migration items that are semantic or not one-to-one. */
public class MigrationWarningScanner {
    private record Probe(String token, String message) {}

    private static final String PRIMER =
        "https://github.com/neoforged/.github/blob/main/primers/26.2/index.md";

    private static final List<Probe> JAVA_PROBES = List.of(
        new Probe("valueLookupBuilder", "26.2 registration/data-gen: valueLookupBuilder was removed; split ids into BlockIds, BlockItemIds, and ItemIds. Source: " + PRIMER),
        new Probe("HolderTagProvider", "26.2 tags provider: HolderTagProvider was removed; migrate to TagsProvider with ResourceKey/TagKey entries. Source: " + PRIMER),
        new Probe("IntrinsicHolderTagsProvider", "26.2 tags provider: IntrinsicHolderTagsProvider was removed; migrate to TagsProvider. Source: " + PRIMER),
        new Probe("KeyTagProvider", "26.2 tags provider: KeyTagProvider was removed; tag methods moved to TagsProvider. Source: " + PRIMER),
        new Probe("BlockItemTagsProvider", "26.2 tags provider: BlockItemTagsProvider uses BlockItemTagId and CombinedAppender. Review manually. Source: " + PRIMER),
        new Probe("StructureProcessorType", "26.2 structure processors: registry now uses MapCodec directly and StructureProcessor is an interface. Source: " + PRIMER),
        new Probe("StructureProcessor", "26.2 structure processors: getType/processBlock signatures changed; review custom processors. Source: " + PRIMER),
        new Probe("Font.draw", "26.2 rendering: Font draw methods were removed; prepare text or submit text through the new pipeline. Source: " + PRIMER),
        new Probe("drawInBatch", "26.2 rendering: Font drawInBatch methods were removed; migrate text rendering manually. Source: " + PRIMER),
        new Probe("MultiBufferSource", "26.2 rendering: feature rendering no longer uses direct MultiBufferSource paths. Source: " + PRIMER),
        new Probe("RenderPipeline", "26.2 rendering: RenderPipeline bind groups, color targets, and vertex bindings changed. Source: " + PRIMER),
        new Probe("VertexFormatElement", "26.2 rendering: VertexFormatElement construction changed to GpuFormat attributes. Source: " + PRIMER),
        new Probe("TextureFormat", "26.2 rendering: TextureFormat was replaced by GpuFormat. Source: " + PRIMER),
        new Probe("DestFactor", "26.2 rendering: DestFactor/SourceFactor merged into BlendFactor/BlendOp. Source: " + PRIMER),
        new Probe("SourceFactor", "26.2 rendering: DestFactor/SourceFactor merged into BlendFactor/BlendOp. Source: " + PRIMER),
        new Probe("GraphicsWorkarounds", "26.2 rendering: GraphicsWorkarounds split into GlHeuristics/HintsAndWorkarounds; not one-to-one. Source: " + PRIMER),
        new Probe("Tesselator", "26.2 rendering: Tesselator class was removed. Source: " + PRIMER),
        new Probe("PictureInPictureRenderer", "26.2 PiP: prepare/renderToTexture signatures changed. Source: " + PRIMER),
        new Probe("ShapeRenderer", "26.2 outlines: ShapeRenderer was replaced by submitShapeOutline. Source: " + PRIMER),
        new Probe("ScreenManager", "26.2 monitor API: ScreenManager became MonitorManager and is not one-to-one. Source: " + PRIMER),
        new Probe("FrameBufferCache", "26.2 rendering: FBO access and presentation moved through FrameBufferCache/GpuSurface APIs; review manually. Source: " + PRIMER),
        new Probe("GpuDevice", "26.2 rendering backend: device info, limits, vsync, and surface APIs changed. Source: " + PRIMER),
        new Probe("LevelRenderer", "26.2 level rendering was heavily reorganized into extractor/render-state/feature renderer paths; many methods are not one-to-one. Source: " + PRIMER),
        new Probe("RenderBuffers", "26.2 rendering buffers were replaced by staged/feature renderer paths; review manually. Source: " + PRIMER),
        new Probe("SectionRenderDispatcher", "26.2 chunk rendering tasks/sections changed names and signatures; review manually. Source: " + PRIMER),
        new Probe("BedRenderer", "26.2 resources/rendering: BedRenderer was removed because beds use block models. Source: " + PRIMER),
        new Probe("BedSpecialRenderer", "26.2 resources/rendering: BedSpecialRenderer was removed because beds use block models. Source: " + PRIMER),
        new Probe("Model.renderToBuffer", "26.2 model rendering signature/removal requires semantic renderer migration. Source: " + PRIMER),
        new Probe("ChatFormatting", "26.2 text: ChatFormatting usage in Components should migrate to Style/TextColor APIs. Source: " + PRIMER),
        new Probe("WeatheringCopperItems", "26.2 data-gen: WeatheringCopperItems merged into WeatheringCopperCollection; not one-to-one. Source: " + PRIMER),
        new Probe("WeatheringCopperBlocks", "26.2 data-gen: WeatheringCopperBlocks merged into WeatheringCopperCollection; not one-to-one. Source: " + PRIMER),
        new Probe("BuiltInLootTables", "26.2 loot/data-gen: BuiltInLootTables map fields were replaced by ColorCollections; review manually. Source: " + PRIMER),
        new Probe("EntityPredicate", "26.2 advancement predicates: EntityPredicate is now a wrapper around EntitySubPredicate maps; semantic migration required. Source: " + PRIMER),
        new Probe("SurfaceRules.isBiome", "26.2 worldgen: SurfaceRules.isBiome now takes HolderGetter<Biome>. Source: " + PRIMER),
        new Probe("noiseCondition(", "26.2 worldgen: noiseCondition split into noiseCondition2d/noiseCondition3d. Source: " + PRIMER),
        new Probe("getSphaghettiRarity2D", "26.2 worldgen: rarity methods moved to QuantizedSpaghettiRarity wrappers; not one-to-one. Source: " + PRIMER),
        new Probe("getSpaghettiRarity3D", "26.2 worldgen: rarity methods moved to QuantizedSpaghettiRarity wrappers; not one-to-one. Source: " + PRIMER),
        new Probe("WeightedPlacedFeature", "26.2 worldgen: WeightedPlacedFeature deprecated; use WeightedRandomSelectorFeature. Source: " + PRIMER),
        new Probe("SimpleRandomFeatureConfiguration", "26.2 worldgen: SimpleRandomFeatureConfiguration renamed to CompositeFeatureConfiguration; constructor semantics changed. Source: " + PRIMER),
        new Probe("LakeFeature.Configuration", "26.2 worldgen: LakeFeature.Configuration now takes BlockPredicates for placement/replacement; review manually. Source: " + PRIMER),
        new Probe("LargeDripstoneFeature", "26.2 worldgen: LargeDripstoneFeature now takes a HolderSet of replaceable Blocks; review manually. Source: " + PRIMER),
        new Probe("hangingSign(", "26.2 recipe provider: hangingSign changed to hangingSignBuilder with Ingredient semantics; review manually. Source: " + PRIMER),
        new Probe("SelectorPattern", "26.2 command selector internals changed several options to option objects; review manually. Source: " + PRIMER),
        new Probe("EntitySpawnReason", "26.2 entity factory APIs now use EntitySpawnRequest in affected create methods; review manually. Source: " + PRIMER),
        new Probe("Sheets.SIGN", "26.2 resources: sign sprite/sheet APIs were removed because signs use block models. Source: " + PRIMER),
        new Probe("Sheets.HANGING_SIGN", "26.2 resources: hanging sign sprite APIs were removed because signs use block models. Source: " + PRIMER),
        new Probe("ModelLayers.BED", "26.2 resources: bed entity model layers were removed because beds use block models. Source: " + PRIMER),
        new Probe("@At(", "26.2 mixins: verify targets against the renamed/moved 26.2 methods before release; only exact known target strings are safe to automate. Source: " + PRIMER)
    );

    private static final List<Probe> RESOURCE_PROBES = List.of(
        new Probe("\"minecraft:signs\"", "26.2 resources: minecraft:signs atlas was removed; signs now use block models. Source: https://www.minecraft.net/en-us/article/minecraft-java-edition-26-2"),
        new Probe("\"minecraft:beds\"", "26.2 resources: minecraft:beds atlas was removed; beds now use block models. Source: https://www.minecraft.net/en-us/article/minecraft-java-edition-26-2"),
        new Probe("\"minecraft:hanging_signs\"", "26.2 resources: hanging signs now use block models; verify atlas/model references. Source: https://www.minecraft.net/en-us/article/minecraft-java-edition-26-2"),
        new Probe("rendertype_text_background_see_through", "26.2 shaders: fold into text_background with IS_SEE_THROUGH define. Source: " + PRIMER),
        new Probe("rendertype_text_intensity", "26.2 shaders: fold into text with IS_GRAYSCALE define. Source: " + PRIMER),
        new Probe("rendertype_text_see_through", "26.2 shaders: fold into text with IS_SEE_THROUGH define. Source: " + PRIMER)
    );

    public PatchResult scan(Path modRoot, String fromVersion, String toVersion) throws IOException {
        if (!fromVersion.equals("26.1.2") || !toVersion.equals("26.2")) {
            return PatchResult.success("No migration warnings for " + fromVersion + " -> " + toVersion, List.of());
        }
        List<String> warnings = new ArrayList<>();
        scanFiles(modRoot, JAVA_PROBES, warnings, ".java", ".gradle", ".kts");
        scanFiles(modRoot, RESOURCE_PROBES, warnings, ".json", ".mcmeta");
        return PatchResult.success("Migration warnings: " + warnings.size(), warnings);
    }

    private void scanFiles(Path root, List<Probe> probes, List<String> warnings, String... suffixes) throws IOException {
        if (!Files.exists(root)) return;
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                .filter(path -> !path.toString().contains("build") && !path.toString().contains(".gradle"))
                .filter(path -> hasSuffix(path, suffixes))
                .forEach(path -> scanFile(root, path, probes, warnings));
        }
    }

    private boolean hasSuffix(Path path, String[] suffixes) {
        String name = path.getFileName().toString();
        for (String suffix : suffixes) if (name.endsWith(suffix)) return true;
        return false;
    }

    private void scanFile(Path root, Path file, List<Probe> probes, List<String> warnings) {
        try {
            String content = Files.readString(file);
            for (Probe probe : probes) {
                if (content.contains(probe.token())) {
                    warnings.add(root.relativize(file) + ": MANUAL 26.2 REVIEW for `" + probe.token() + "` - " + probe.message());
                }
            }
        } catch (IOException ignored) {
        }
    }
}
