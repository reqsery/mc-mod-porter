package com.autoporter;

import com.autoporter.JavaTokenizer.Kind;
import com.autoporter.JavaTokenizer.Token;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Stream;

/**
 * Token-aware Yarn → Mojang remapping of a mod's own {@code .java} / {@code .kt} sources (never compiled jars).
 *
 * <p>What is renamed, and how safely:
 * <ul>
 *   <li><b>Classes</b> — exact: {@code import} statements, fully-qualified names and the simple names they import,
 *       including nested classes ({@code Outer.Inner}).</li>
 *   <li><b>Methods / fields</b> — only when the Yarn name maps to exactly one Mojang name across all of Minecraft,
 *       and only where the name is clearly a Minecraft member: after {@code .} or {@code ::}, on
 *       {@code @Override} / {@code @Shadow} declarations (and Kotlin {@code override fun}), and unqualified uses of
 *       a mixin's {@code @Shadow} members. Names the mod declares itself and common JDK method names are skipped.</li>
 *   <li>Comments, string literals and char literals are never changed. Mixin target strings
 *       ({@code method = "…"}, {@code @At(target = "…")}) are reported for manual review.</li>
 * </ul>
 */
public final class SourceRemapper {

    /** Which names a project's sources use. */
    public enum Mappings {
        AUTO, YARN, MOJANG, UNKNOWN;

        public static Mappings parse(String value) {
            return switch (value == null ? "auto" : value.trim().toLowerCase(Locale.ROOT)) {
                case "yarn" -> YARN;
                case "mojang", "mojmap", "official" -> MOJANG;
                case "auto", "" -> AUTO;
                default -> throw new IllegalArgumentException("--source-mappings must be auto, yarn or mojang, not " + value);
            };
        }
    }

    public record Detection(Mappings mappings, String yarnBuild, String reason) {}

    public record FileResult(String content, Map<String, String> renames, int count, List<String> warnings) {}

    /** Method/field names that are far more likely to belong to JDK or library types than to Minecraft. */
    private static final Set<String> COMMON_NAMES = Set.of(
        "add", "addAll", "get", "getOrDefault", "put", "putAll", "remove", "removeIf", "size", "isEmpty", "contains",
        "containsKey", "containsValue", "equals", "hashCode", "toString", "length", "stream", "forEach", "of", "copyOf",
        "valueOf", "values", "keySet", "entrySet", "getKey", "getValue", "setValue", "apply", "accept", "test", "run",
        "call", "close", "iterator", "next", "hasNext", "map", "filter", "collect", "append", "format", "getName",
        "getClass", "compareTo", "clear", "set", "sort", "max", "min", "abs", "floor", "ceil", "sqrt", "getMessage",
        "printStackTrace", "start", "join", "clone", "ordinal", "name", "orElse", "orElseThrow", "isPresent",
        "ifPresent", "toList", "toArray", "subList", "indexOf", "trim", "split", "replace", "substring", "charAt",
        "startsWith", "endsWith", "toLowerCase", "toUpperCase", "intValue", "floatValue", "doubleValue", "longValue",
        "getFirst", "getLast", "peek", "poll", "offer", "push", "pop", "write", "read", "flush", "info", "warn",
        "error", "debug", "println", "print", "x", "y", "z");

    private static final Set<String> NOT_A_TYPE = Set.of("return", "new", "throw", "else", "case", "instanceof",
        "extends", "implements", "import", "package", "yield", "assert", "in", "is", "as", "this", "super");

    private static final Pattern MIXIN_STRING_TARGET = Pattern.compile(
        "@(?:Inject|Redirect|ModifyArg|ModifyArgs|ModifyVariable|ModifyConstant|ModifyExpressionValue|ModifyReturnValue|"
        + "WrapOperation|WrapWithCondition|Accessor|Invoker|At)\\s*\\([^)]*(?:method|target|value)\\s*=\\s*\"|"
        + "@(?:Accessor|Invoker)\\s*\\(\\s*\"");

    // ── Detection ──────────────────────────────────────────────────────────────────────────────

    /** Works out whether a project uses Yarn or Mojang names, and which Yarn build. */
    public static Detection detect(Path root, String sourceVersion) throws IOException {
        Properties props = new Properties();
        for (String rel : List.of("gradle.properties", "fabric/gradle.properties", "common/gradle.properties")) {
            Path p = root.resolve(rel);
            if (Files.isRegularFile(p)) try (var reader = Files.newBufferedReader(p)) { props.load(reader); }
        }
        StringBuilder build = new StringBuilder();
        for (Path file : BuildFilePatcher.findBuildFiles(root)) build.append(Files.readString(file)).append('\n');
        String scripts = build.toString();
        VersionDatabase.VersionInfo info = VersionDatabase.get(sourceVersion);
        String fallbackYarn = info == null ? null : info.yarnMappings();

        Matcher literal = Pattern.compile("net\\.fabricmc:yarn:([^:'\"$]+)").matcher(scripts);
        if (literal.find()) return new Detection(Mappings.YARN, literal.group(1), "build script uses Yarn " + literal.group(1));
        if (scripts.contains("net.fabricmc:yarn:") || props.getProperty("yarn_mappings") != null) {
            String yarn = props.getProperty("yarn_mappings", fallbackYarn);
            return new Detection(Mappings.YARN, yarn == null ? null : yarn.trim(), "build script uses Yarn mappings");
        }
        if (scripts.contains("officialMojangMappings")) return new Detection(Mappings.MOJANG, null, "build script uses Mojang mappings");
        if (scripts.contains("net.neoforged") || scripts.contains("net.minecraftforge")) {
            return new Detection(Mappings.MOJANG, null, "NeoForge/Forge projects use Mojang names");
        }
        if (VersionDatabase.isUnobfuscated(sourceVersion)) {
            return new Detection(Mappings.MOJANG, null, "26.x sources use official names");
        }
        return new Detection(Mappings.UNKNOWN, fallbackYarn, "no mappings declaration found");
    }

    // ── Project ────────────────────────────────────────────────────────────────────────────────

    public static List<Path> findSources(Path root) throws IOException {
        List<Path> files = new ArrayList<>();
        for (String module : List.of("", "common/", "fabric/", "neoforge/")) {
            for (String set : List.of("main", "client")) {
                for (String lang : List.of("java", "kotlin")) {
                    Path dir = root.resolve(module + "src/" + set + "/" + lang);
                    if (!Files.isDirectory(dir)) continue;
                    try (Stream<Path> walk = Files.walk(dir)) {
                        walk.filter(Files::isRegularFile)
                            .filter(p -> p.toString().endsWith(".java") || p.toString().endsWith(".kt"))
                            .forEach(files::add);
                    }
                }
            }
        }
        return files;
    }

    /** Remaps every source file of the project. */
    public static PatchResult remapProject(Path root, MappingTable table, boolean dryRun) throws IOException {
        List<Path> files = findSources(root);
        List<String> contents = new ArrayList<>();
        for (Path f : files) contents.add(Files.readString(f));
        Set<String> declared = declaredNames(contents);

        List<String> changes = new ArrayList<>();
        Map<String, String> classRenames = new TreeMap<>();
        int total = 0, changedFiles = 0;
        for (int i = 0; i < files.size(); i++) {
            FileResult r = remap(contents.get(i), table, declared);
            String rel = root.relativize(files.get(i)).toString().replace('\\', '/');
            for (String w : r.warnings()) changes.add(rel + ": MANUAL " + w);
            if (r.count() == 0) continue;
            changedFiles++;
            total += r.count();
            changes.add(rel + ": remapped " + r.count() + " name(s) Yarn → Mojang");
            // package-only moves (Screen → Screen) are visible in the imports; list real renames only
            r.renames().forEach((k, v) -> { if (Character.isUpperCase(k.charAt(0)) && !k.equals(v)) classRenames.put(k, v); });
            if (!dryRun) Files.writeString(files.get(i), r.content());
        }
        if (!classRenames.isEmpty()) {
            StringBuilder sb = new StringBuilder("class renames: ");
            classRenames.forEach((k, v) -> sb.append(k).append(" → ").append(v).append(", "));
            changes.add(sb.substring(0, sb.length() - 2));
        }
        String prefix = dryRun ? "[DRY RUN] Would remap " : "Remapped ";
        return PatchResult.success(prefix + total + " name(s) in " + changedFiles + " of " + files.size() + " source file(s)", changes);
    }

    // ── Declarations ───────────────────────────────────────────────────────────────────────────

    /** Method, field and variable names the mod declares itself (excluding @Override / @Shadow members). */
    static Set<String> declaredNames(List<String> sources) {
        Set<String> declared = new HashSet<>();
        for (String src : sources) {
            List<Token> code = codeTokens(src);
            for (int i = 1; i < code.size(); i++) {
                Token t = code.get(i);
                if (t.kind() != Kind.IDENT) continue;
                Token prev = code.get(i - 1);
                Token next = i + 1 < code.size() ? code.get(i + 1) : null;
                boolean typedBefore = (prev.kind() == Kind.IDENT && !NOT_A_TYPE.contains(prev.text()))
                    || prev.is(">") || prev.is("]");
                if (i >= 2 && code.get(i - 2).is(".")) typedBefore = false;
                boolean kotlinDecl = prev.is("fun") || prev.is("val") || prev.is("var");
                boolean method = next != null && next.is("(") && (typedBefore || prev.is("fun"));
                boolean field = next != null && (next.is("=") || next.is(";") || next.is(",") || next.is(":")) && (typedBefore || kotlinDecl);
                if ((method || field || kotlinDecl) && !isMinecraftMemberDeclaration(code, i)) declared.add(t.text());
            }
        }
        return declared;
    }

    /** True for a declaration annotated @Override/@Shadow, or a Kotlin override fun/val. */
    private static boolean isMinecraftMemberDeclaration(List<Token> code, int index) {
        for (int i = index - 1; i >= 0; i--) {
            Token t = code.get(i);
            if (t.is(";") || t.is("{") || t.is("}")) return false;
            if (t.is("override")) return true;
            if (t.is("@") && i + 1 < code.size() && (code.get(i + 1).is("Override") || code.get(i + 1).is("Shadow"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isShadowDeclaration(List<Token> code, int index) {
        for (int i = index - 1; i >= 0; i--) {
            Token t = code.get(i);
            if (t.is(";") || t.is("{") || t.is("}")) return false;
            if (t.is("@") && i + 1 < code.size() && code.get(i + 1).is("Shadow")) return true;
        }
        return false;
    }

    private static List<Token> codeTokens(String src) {
        return JavaTokenizer.tokenize(src).stream().filter(Token::code).toList();
    }

    // ── Remapping one file ─────────────────────────────────────────────────────────────────────

    private record Edit(int start, int end, String text) {}

    private record Resolved(String internal, int classEnd) {}

    /** Longest prefix of the dotted segments that is a Yarn class (packages joined by '/', nested by '$'). */
    private static Resolved resolveClass(List<String> segs, MappingTable table) {
        for (int classEnd = segs.size(); classEnd >= 2; classEnd--) {
            for (int pkgEnd = classEnd - 1; pkgEnd >= 1; pkgEnd--) {
                String name = String.join("/", segs.subList(0, pkgEnd)) + "/" + String.join("$", segs.subList(pkgEnd, classEnd));
                if (table.hasClass(name)) return new Resolved(name, classEnd);
            }
        }
        return null;
    }

    private static String dotted(String internal) { return internal.replace('/', '.').replace('$', '.'); }

    private static String simpleName(String internal) {
        return internal.substring(Math.max(internal.lastIndexOf('/'), internal.lastIndexOf('$')) + 1);
    }

    public static FileResult remap(String src, MappingTable table, Set<String> declared) {
        List<Token> code = codeTokens(src);
        List<Edit> edits = new ArrayList<>();
        Map<String, String> renames = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        Map<String, String> imported = new HashMap<>();   // yarn simple name → yarn internal
        Map<String, String> shadowed = new HashMap<>();   // yarn @Shadow member name → mojang name
        boolean[] done = new boolean[code.size()];
        String[] classAt = new String[code.size()];

        // 1. import statements
        for (int i = 0; i < code.size(); i++) {
            if (!code.get(i).is("import") || (i > 0 && code.get(i - 1).is("."))) continue;
            int j = i + 1;
            if (j < code.size() && code.get(j).is("static")) j++;
            List<Integer> segIdx = new ArrayList<>();
            boolean wildcard = false;
            while (j < code.size() && code.get(j).kind() == Kind.IDENT) {
                segIdx.add(j);
                if (j + 1 < code.size() && code.get(j + 1).is(".")) {
                    if (j + 2 < code.size() && code.get(j + 2).is("*")) { wildcard = true; j += 3; break; }
                    j += 2;
                } else { j++; break; }
            }
            boolean alias = j < code.size() && code.get(j).is("as");
            for (int k = i; k < j && k < code.size(); k++) done[k] = true;
            if (segIdx.isEmpty()) continue;
            List<String> segs = segIdx.stream().map(k -> code.get(k).text()).toList();
            Resolved r = resolveClass(segs, table);
            if (r == null) {
                if (wildcard && String.join(".", segs).startsWith("net.minecraft")) {
                    warnings.add("wildcard import " + String.join(".", segs) + ".* was not remapped — replace it with explicit imports");
                }
                continue;
            }
            String replacement = dotted(table.mojangClass(r.internal()));
            int last = r.classEnd() - 1;
            if (r.classEnd() == segs.size() - 1) { // import static a.b.C.member
                String member = segs.get(r.classEnd());
                String mapped = Optional.ofNullable(table.uniqueMethod(member)).orElse(table.uniqueField(member));
                replacement += "." + (mapped != null ? mapped : member);
                last = r.classEnd();
            }
            addEdit(src, code.get(segIdx.get(0)).start(), code.get(segIdx.get(last)).end(), replacement, edits, renames,
                String.join(".", segs.subList(0, last + 1)));
            if (!alias && !wildcard && r.classEnd() == segs.size()) imported.put(segs.get(segs.size() - 1), r.internal());
        }

        // 2. @Shadow member declarations (names inside mixins are Minecraft names)
        for (int i = 0; i < code.size(); i++) {
            Token t = code.get(i);
            if (done[i] || t.kind() != Kind.IDENT || !isShadowDeclaration(code, i)) continue;
            Token next = i + 1 < code.size() ? code.get(i + 1) : null;
            if (next == null || !(next.is("(") || next.is(";") || next.is("="))) continue;
            String mapped = next.is("(") ? table.uniqueMethod(t.text()) : table.uniqueField(t.text());
            if (mapped != null) shadowed.put(t.text(), mapped);
        }

        // 3. fully-qualified names in code, then simple names, nested classes and members
        for (int i = 0; i < code.size(); i++) {
            Token t = code.get(i);
            if (done[i] || t.kind() != Kind.IDENT) continue;
            Token prev = i > 0 ? code.get(i - 1) : null;
            Token next = i + 1 < code.size() ? code.get(i + 1) : null;
            boolean afterDot = prev != null && (prev.is(".") || prev.is("::"));

            if (!afterDot && (t.is("net") || t.is("com")) && next != null && next.is(".")) {
                List<Integer> segIdx = new ArrayList<>();
                int j = i;
                while (j < code.size() && code.get(j).kind() == Kind.IDENT) {
                    segIdx.add(j);
                    if (j + 2 < code.size() && code.get(j + 1).is(".") && code.get(j + 2).kind() == Kind.IDENT) j += 2;
                    else break;
                }
                Resolved r = resolveClass(segIdx.stream().map(k -> code.get(k).text()).toList(), table);
                if (r != null) {
                    int last = segIdx.get(r.classEnd() - 1);
                    addEdit(src, t.start(), code.get(last).end(), dotted(table.mojangClass(r.internal())), edits, renames,
                        src.substring(t.start(), code.get(last).end()));
                    for (int k = i; k <= last; k++) done[k] = true;
                    classAt[last] = r.internal();
                    continue;
                }
            }

            if (afterDot) {
                String owner = i >= 2 ? classAt[i - 2] : null;
                if (owner != null && table.hasClass(owner + "$" + t.text())) {
                    String nested = owner + "$" + t.text();
                    classAt[i] = nested;
                    addEdit(src, t.start(), t.end(), simpleName(table.mojangClass(nested)), edits, renames, t.text());
                    continue;
                }
                boolean call = (next != null && next.is("(")) || prev.is("::");
                if (owner != null) { // Identifier.of(…), Blocks.STONE: the owner is a known Minecraft class
                    String ownedName = call ? table.methodOf(owner, t.text()) : table.fieldOf(owner, t.text());
                    if (ownedName != null) { addEdit(src, t.start(), t.end(), ownedName, edits, renames, t.text()); continue; }
                    Set<String> candidates = call ? table.methodCandidates(owner, t.text()) : Set.of();
                    if (candidates.size() > 1) {
                        String w = simpleName(owner) + "." + t.text() + "(…) has several Mojang names " + new TreeSet<>(candidates)
                            + " depending on its arguments — pick the right one by hand";
                        if (!warnings.contains(w)) warnings.add(w);
                        continue;
                    }
                }
                if (declared.contains(t.text()) || COMMON_NAMES.contains(t.text())) continue;
                String mapped = call ? table.uniqueMethod(t.text()) : table.uniqueField(t.text());
                if (mapped != null) addEdit(src, t.start(), t.end(), mapped, edits, renames, t.text());
                continue;
            }

            if (imported.containsKey(t.text())) {
                String internal = imported.get(t.text());
                classAt[i] = internal;
                addEdit(src, t.start(), t.end(), simpleName(table.mojangClass(internal)), edits, renames, t.text());
                continue;
            }
            if (shadowed.containsKey(t.text())) {
                addEdit(src, t.start(), t.end(), shadowed.get(t.text()), edits, renames, t.text());
                continue;
            }
            if (next != null && next.is("(") && isMinecraftMemberDeclaration(code, i)) {
                String mapped = table.uniqueMethod(t.text());
                if (mapped != null) addEdit(src, t.start(), t.end(), mapped, edits, renames, t.text());
            }
        }

        // 4. mixin strings are never rewritten
        Matcher mixin = MIXIN_STRING_TARGET.matcher(src);
        int mixinTargets = 0;
        while (mixin.find()) mixinTargets++;
        if (mixinTargets > 0 && !edits.isEmpty()) {
            warnings.add(mixinTargets + " mixin target string(s) (method = \"…\", @At(target = \"…\"), @Accessor(\"…\")) "
                + "still use Yarn names — update them by hand");
        }

        edits.sort(Comparator.comparingInt(Edit::start).reversed());
        StringBuilder out = new StringBuilder(src);
        for (Edit e : edits) out.replace(e.start(), e.end(), e.text());
        return new FileResult(out.toString(), renames, edits.size(), warnings);
    }

    private static void addEdit(String src, int start, int end, String replacement, List<Edit> edits,
                                Map<String, String> renames, String original) {
        if (src.substring(start, end).equals(replacement)) return;
        edits.add(new Edit(start, end, replacement));
        renames.putIfAbsent(original.contains(".") ? original.substring(original.lastIndexOf('.') + 1) : original,
            replacement.contains(".") ? replacement.substring(replacement.lastIndexOf('.') + 1) : replacement);
    }
}
