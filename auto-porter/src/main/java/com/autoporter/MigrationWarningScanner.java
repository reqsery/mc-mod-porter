package com.autoporter;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/** Reports migration items that are semantic or not one-to-one. */
public class MigrationWarningScanner {
    private record Probe(String token, String message) {}

    private static final String PRIMER_26_2 =
        "https://github.com/neoforged/.github/blob/main/primers/26.2/index.md";
    private static final String PRIMER_26_3 =
        "https://github.com/neoforged/.github/blob/main/primers/26.3/index.md";
    private static final String FABRIC_26_3 =
        "https://fabricmc.net/2026/09/15/263.html";
    private static final String PRIMER = PRIMER_26_2;

    private static final List<Probe> JAVA_PROBES_26_2 = List.of(
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

    private static final List<Probe> RESOURCE_PROBES_26_2 = List.of(
        new Probe("\"minecraft:signs\"", "26.2 resources: minecraft:signs atlas was removed; signs now use block models. Source: https://www.minecraft.net/en-us/article/minecraft-java-edition-26-2"),
        new Probe("\"minecraft:beds\"", "26.2 resources: minecraft:beds atlas was removed; beds now use block models. Source: https://www.minecraft.net/en-us/article/minecraft-java-edition-26-2"),
        new Probe("\"minecraft:hanging_signs\"", "26.2 resources: hanging signs now use block models; verify atlas/model references. Source: https://www.minecraft.net/en-us/article/minecraft-java-edition-26-2"),
        new Probe("rendertype_text_background_see_through", "26.2 shaders: fold into text_background with IS_SEE_THROUGH define. Source: " + PRIMER),
        new Probe("rendertype_text_intensity", "26.2 shaders: fold into text with IS_GRAYSCALE define. Source: " + PRIMER),
        new Probe("rendertype_text_see_through", "26.2 shaders: fold into text with IS_SEE_THROUGH define. Source: " + PRIMER)
    );

    private static final List<Probe> JAVA_PROBES_26_3 = List.of(
        new Probe("org.lwjgl.glfw", "26.3 input/windowing: GLFW was replaced by SDL. Use InputConstants or SDL constants; numeric codes changed. Source: " + PRIMER_26_3),
        new Probe("GLFW.", "26.3 input/windowing: GLFW constants are no longer valid; use InputConstants or SDL constants. Source: " + FABRIC_26_3),
        new Probe("KeyEvent", "26.3 input: KeyEvent key/scancode meaning changed and scancode was renamed to keycode; review handlers. Source: " + PRIMER_26_3),
        new Probe("setFocused(", "26.3 text input: custom text widgets must notify Minecraft/TextInputManager when focus changes. Source: " + FABRIC_26_3),
        new Probe("RenderSystem.outputColorTextureOverride", "26.3 rendering: output texture overrides were removed; create an explicit RenderPass. Source: " + PRIMER_26_3),
        new Probe("RenderSystem.outputDepthTextureOverride", "26.3 rendering: output texture overrides were removed; create an explicit RenderPass. Source: " + PRIMER_26_3),
        new Probe("ConfiguredFeature", "26.3 worldgen: ConfiguredFeature and FeatureConfiguration merged into the Feature interface. Source: " + PRIMER_26_3),
        new Probe("FeatureConfiguration", "26.3 worldgen: feature configuration is implemented directly by Feature records/classes. Source: " + PRIMER_26_3),
        new Probe("ConfiguredWorldCarver", "26.3 worldgen: ConfiguredWorldCarver and CarverConfiguration merged into WorldCarver. Source: " + PRIMER_26_3),
        new Probe("CarverConfiguration", "26.3 worldgen: carver configuration is implemented directly by WorldCarver. Source: " + PRIMER_26_3),
        new Probe("BuiltInRegistries.FEATURE", "26.3 worldgen: feature type codecs now register to FEATURE_TYPE; verify this is a type registration before changing it. Source: " + FABRIC_26_3),
        new Probe("BuiltInRegistries.CARVER", "26.3 worldgen: carver type codecs now register to CARVER_TYPE; verify manually. Source: " + PRIMER_26_3),
        new Probe("SurfaceRules", "26.3 worldgen: surface rules became registerable material rules and inner types were split; migrate semantically. Source: " + PRIMER_26_3),
        new Probe("SurfaceSystem", "26.3 worldgen: SurfaceSystem became MaterialSystem, with wider material-rule changes. Source: " + PRIMER_26_3),
        new Probe("NumberProvider", "26.3 data: number providers split into ContextIntProvider and ContextFloatProvider; choose by use site. Source: " + FABRIC_26_3),
        new Probe("ContextAwarePredicate", "26.3 loot/data: ContextAwarePredicate was removed in favor of registered holder-wrapped predicates. Source: " + PRIMER_26_3),
        new Probe("LootTableProvider", "26.3 data generation: loot tables are a reloadable registry bootstrap, not an independent DataProvider. Source: " + PRIMER_26_3),
        new Probe("AdvancementProvider", "26.3 data generation: advancements are generated through reloadable registry bootstraps. Source: " + PRIMER_26_3),
        new Probe("PotionBrewing", "26.3 brewing: brewing mixes are data-driven recipes. Source: " + PRIMER_26_3),
        new Probe("ComposterBlock.COMPOSTABLES", "26.3 composting: compostability moved to DataComponents.COMPOSTABLE. Source: " + FABRIC_26_3),
        new Probe("FuelValues", "26.3 fuel: cooking fuel moved to DataComponents.COOKING_FUEL. Source: " + PRIMER_26_3),
        new Probe("AxeItem.STRIPPABLES", "26.3 block interaction: stripping moved to data-driven BlockTransformers. Source: " + PRIMER_26_3),
        new Probe("ShovelItem.FLATTENABLES", "26.3 block interaction: flattening moved to data-driven BlockTransformers. Source: " + PRIMER_26_3),
        new Probe("HoeItem.TILLABLES", "26.3 block interaction: tilling moved to data-driven BlockTransformers. Source: " + PRIMER_26_3),
        new Probe("CompostingChanceRegistry", "26.3 Fabric API: CompostingChanceRegistry was removed; use item components or DefaultItemComponentEvents. Source: " + FABRIC_26_3),
        new Probe("FuelRegistry", "26.3 Fabric API: FuelRegistry was removed; use item components or DefaultItemComponentEvents. Source: " + FABRIC_26_3),
        new Probe("FabricPotionBrewingBuilder", "26.3 Fabric API: brewing registration was replaced by data-driven recipes. Source: " + FABRIC_26_3),
        new Probe("StrippableBlockRegistry", "26.3 Fabric API: use BlockTransformerHelper or data-driven block transformers. Source: " + FABRIC_26_3),
        new Probe("TillableBlockRegistry", "26.3 Fabric API: simple registrations can use BlockTransformerHelper.registerTilling; complex predicates need manual migration. Source: " + FABRIC_26_3),
        new Probe("FlattenableBlockRegistry", "26.3 Fabric API: use BlockTransformerHelper or data-driven block transformers. Source: " + FABRIC_26_3),
        new Probe("enableColoredVanillaFluidNames", "26.3 Fabric API: this global toggle was removed; provide names with FluidVariantAttributes.getColoredName. Source: " + FABRIC_26_3),
        new Probe("ModConfig.Type.COMMON", "26.3 NeoForge: COMMON config was renamed to LOCAL. Source: https://github.com/neoforged/NeoForge/pull/3384"),
        new Probe("ModConfig.Type.SERVER", "26.3 NeoForge: SERVER config was renamed to SYNCED. Source: https://github.com/neoforged/NeoForge/pull/3384"),
        new Probe("ViewportEvent", "26.3 NeoForge: ViewportEvent partial ticks changed from double to float; review constructors and handlers. Source: https://github.com/neoforged/NeoForge/pull/3491"),
        new Probe("IFluidTank", "26.3 NeoForge: deprecated IFluidTank was removed; migrate to the current fluid-handler API. Source: https://github.com/neoforged/NeoForge/pull/3489"),
        new Probe("InvWrapper", "26.3 NeoForge: deprecated InvWrapper was removed; use current item-handler wrappers. Source: https://github.com/neoforged/NeoForge/pull/3489"),
        new Probe("RangedWrapper", "26.3 NeoForge: deprecated RangedWrapper was removed; use current item-handler wrappers. Source: https://github.com/neoforged/NeoForge/pull/3489"),
        new Probe("EmptyFluidHandler", "26.3 NeoForge: deprecated EmptyFluidHandler was removed. Source: https://github.com/neoforged/NeoForge/pull/3489"),
        new Probe("FluidHandlerItemStackSimple.Consumable", "26.3 NeoForge: the deprecated Consumable handler was removed. Source: https://github.com/neoforged/NeoForge/pull/3489"),
        new Probe("NeoForgeRenderTypes", "26.3 NeoForge: deprecated NeoForgeRenderTypes helpers were removed during rendering cleanup. Source: https://github.com/neoforged/NeoForge/pull/3489"),
        new Probe("processEntityInfos", "26.3 NeoForge: StructureTemplate.processEntityInfos moved to CommonHooks. Source: https://github.com/neoforged/NeoForge/pull/3619"),
        new Probe("markWorldsDirty", "26.3 NeoForge: MinecraftServer.markWorldsDirty was removed. Source: https://github.com/neoforged/NeoForge/pull/3619"),
        new Probe("forgeGetWorldMap", "26.3 NeoForge: MinecraftServer.forgeGetWorldMap was removed. Source: https://github.com/neoforged/NeoForge/pull/3619"),
        new Probe("EnderMan", "26.3 entity rename: EnderMan became Enderman but the primer marks the migration non-one-to-one. Source: " + PRIMER_26_3),
        new Probe("@At(", "26.3 mixins: verify every target against 26.3 official sources; only exact known descriptors are safe to automate. Source: " + PRIMER_26_3)
    );

    private static final List<Probe> RESOURCE_PROBES_26_3 = List.of(
        new Probe("\"spawners\"", "26.3 biome schema: spawners moved to environment attributes; migrate manually. Source: https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3"),
        new Probe("\"spawn_costs\"", "26.3 biome schema: spawn costs moved to environment attributes; migrate manually. Source: https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3"),
        new Probe("\"creature_spawn_probability\"", "26.3 biome schema: creature spawn probability moved to environment attributes. Source: https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3"),
        new Probe("worldgen/number_provider", "26.3 data: number providers split into context_int_provider and context_float_provider; the correct type depends on use. Source: " + FABRIC_26_3),
        new Probe("armor_trims.json", "26.3 resources: the armor trims atlas was removed and entity trim textures use palette metadata. Source: " + PRIMER_26_3),
        new Probe("template_farmland", "26.3 models: template_farmland became template_cube_bottom_top_indented and is not one-to-one. Source: " + PRIMER_26_3),
        new Probe("post_effect/transparency", "26.3 shaders: the transparency post effect was removed for OIT. Source: " + PRIMER_26_3),
        new Probe("text_background", "26.3 shaders: text_background was removed and merged into text; update pipeline definitions manually. Source: " + PRIMER_26_3)
    );

    public PatchResult scan(Path modRoot, String fromVersion, String toVersion) throws IOException {
        boolean includes26_2 = includesTransition(fromVersion, toVersion, "26.1.2", "26.2");
        boolean includes26_3 = includesTransition(fromVersion, toVersion, "26.2", "26.3");
        if (!includes26_2 && !includes26_3) {
            return PatchResult.success("No migration warnings for " + fromVersion + " -> " + toVersion, List.of());
        }
        List<String> warnings = new ArrayList<>();
        if (includes26_2) {
            scanFiles(modRoot, JAVA_PROBES_26_2, warnings, "26.2", ".java", ".gradle", ".kts");
            scanFiles(modRoot, RESOURCE_PROBES_26_2, warnings, "26.2", ".json", ".mcmeta");
        }
        if (includes26_3) {
            scanFiles(modRoot, JAVA_PROBES_26_3, warnings, "26.3", ".java", ".gradle", ".kts");
            scanFiles(modRoot, RESOURCE_PROBES_26_3, warnings, "26.3", ".json", ".mcmeta", ".vsh", ".fsh", ".glsl");
        }
        return PatchResult.success("Migration warnings: " + warnings.size(), warnings);
    }

    private boolean includesTransition(String fromVersion, String toVersion, String stepFrom, String stepTo) {
        List<String> versions = VersionDatabase.allVersions();
        int fromIdx = versions.indexOf(VersionDatabase.normalize(fromVersion));
        int toIdx = versions.indexOf(VersionDatabase.normalize(toVersion));
        if (fromIdx < 0 || toIdx < 0 || fromIdx >= toIdx) return false;

        for (int i = fromIdx; i < toIdx; i++) {
            if (versions.get(i).equals(stepFrom) && versions.get(i + 1).equals(stepTo)) {
                return true;
            }
        }
        return false;
    }

    private void scanFiles(Path root, List<Probe> probes, List<String> warnings,
                           String reviewVersion, String... suffixes) throws IOException {
        if (!Files.exists(root)) return;
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                .filter(path -> !path.toString().contains("build") && !path.toString().contains(".gradle"))
                .filter(path -> hasSuffix(path, suffixes))
                .forEach(path -> scanFile(root, path, probes, warnings, reviewVersion));
        }
    }

    private boolean hasSuffix(Path path, String[] suffixes) {
        String name = path.getFileName().toString();
        for (String suffix : suffixes) if (name.endsWith(suffix)) return true;
        return false;
    }

    private void scanFile(Path root, Path file, List<Probe> probes, List<String> warnings,
                          String reviewVersion) {
        try {
            String content = Files.readString(file);
            for (Probe probe : probes) {
                if (content.contains(probe.token())) {
                    warnings.add(root.relativize(file) + ": MANUAL " + reviewVersion
                        + " REVIEW for `" + probe.token() + "` - " + probe.message());
                }
            }
        } catch (IOException ignored) {
        }
    }
}
