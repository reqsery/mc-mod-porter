package com.autoporter;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Yarn → Mojang name table for one Minecraft version, built by joining a Yarn Tiny v2 file (official / named
 * namespaces) with Mojang's official ProGuard mappings on the obfuscated names they share.
 *
 * <p>Class names are exact. Member names are recorded per name: a Yarn field/method name is only remapped when every
 * Minecraft member with that Yarn name has the same Mojang name (see {@link #uniqueMethod} / {@link #uniqueField}).
 */
public final class MappingTable {
    /** Yarn internal name (net/minecraft/x/Y$Z) → Mojang internal name. */
    private final Map<String, String> classes = new HashMap<>();
    private final Map<String, Set<String>> fields = new HashMap<>();
    private final Map<String, Set<String>> methods = new HashMap<>();
    /** Yarn owner class → Yarn member name → Mojang names, for calls whose owner is known (Identifier.of(…)). */
    private final Map<String, Map<String, Set<String>>> ownedFields = new HashMap<>();
    private final Map<String, Map<String, Set<String>>> ownedMethods = new HashMap<>();

    public int classCount() { return classes.size(); }

    public String mojangClass(String yarnInternal) { return classes.get(yarnInternal); }

    public boolean hasClass(String yarnInternal) { return classes.containsKey(yarnInternal); }

    /** The single Mojang name for a Yarn method name, or null if unknown, unchanged or ambiguous. */
    public String uniqueMethod(String yarnName) { return unique(methods.get(yarnName), yarnName); }

    /** The single Mojang name for a Yarn field name, or null if unknown, unchanged or ambiguous. */
    public String uniqueField(String yarnName) { return unique(fields.get(yarnName), yarnName); }

    /** The Mojang name of a method declared on a specific Yarn class, or null if unknown, unchanged or overloaded differently. */
    public String methodOf(String yarnOwner, String yarnName) {
        return unique(ownedMethods.getOrDefault(yarnOwner, Map.of()).get(yarnName), yarnName);
    }

    /** The Mojang name of a field declared on a specific Yarn class, or null if unknown or unchanged. */
    public String fieldOf(String yarnOwner, String yarnName) {
        return unique(ownedFields.getOrDefault(yarnOwner, Map.of()).get(yarnName), yarnName);
    }

    /** Every Mojang name of a method on a Yarn class (several when Yarn overloads map to different names). */
    public Set<String> methodCandidates(String yarnOwner, String yarnName) {
        return ownedMethods.getOrDefault(yarnOwner, Map.of()).getOrDefault(yarnName, Set.of());
    }

    private static String unique(Set<String> names, String yarnName) {
        if (names == null || names.size() != 1) return null;
        String only = names.iterator().next();
        return only.equals(yarnName) ? null : only;
    }

    // ── Building (also used directly by tests) ────────────────────────────────────────────────

    public MappingTable addClass(String yarnInternal, String mojangInternal) {
        classes.put(yarnInternal, mojangInternal);
        return this;
    }

    public MappingTable addField(String yarnName, String mojangName) {
        fields.computeIfAbsent(yarnName, k -> new HashSet<>()).add(mojangName);
        return this;
    }

    public MappingTable addMethod(String yarnName, String mojangName) {
        methods.computeIfAbsent(yarnName, k -> new HashSet<>()).add(mojangName);
        return this;
    }

    public MappingTable addField(String yarnOwner, String yarnName, String mojangName) {
        ownedFields.computeIfAbsent(yarnOwner, k -> new HashMap<>()).computeIfAbsent(yarnName, k -> new HashSet<>()).add(mojangName);
        return addField(yarnName, mojangName);
    }

    public MappingTable addMethod(String yarnOwner, String yarnName, String mojangName) {
        ownedMethods.computeIfAbsent(yarnOwner, k -> new HashMap<>()).computeIfAbsent(yarnName, k -> new HashSet<>()).add(mojangName);
        return addMethod(yarnName, mojangName);
    }

    // ── Loading ───────────────────────────────────────────────────────────────────────────────

    /**
     * Joins a Tiny v2 file that has {@code official} and {@code named} (Yarn) namespaces with Mojang ProGuard
     * mappings (one or more files, e.g. client.txt and server.txt).
     */
    public static MappingTable join(Reader yarnTiny, List<Reader> mojangProguard) throws IOException {
        Proguard mojang = new Proguard();
        for (Reader reader : mojangProguard) mojang.read(reader);
        mojang.index();

        MappingTable table = new MappingTable();
        BufferedReader in = new BufferedReader(yarnTiny);
        String header = in.readLine();
        if (header == null || !header.startsWith("tiny\t2\t")) throw new IOException("Not a Tiny v2 file");
        List<String> namespaces = Arrays.asList(header.split("\t")).subList(3, header.split("\t").length);
        int official = namespaces.indexOf("official");
        int named = namespaces.indexOf("named");
        if (official < 0 || named < 0) throw new IOException("Tiny file needs 'official' and 'named' namespaces, has " + namespaces);

        String obfClass = null;
        String yarnClass = null;
        String line;
        while ((line = in.readLine()) != null) {
            if (line.startsWith("c\t")) {
                String[] p = line.split("\t", -1);
                obfClass = name(p, 1, official);
                String yarn = name(p, 1, named);
                yarnClass = yarn;
                String moj = mojang.classByObf.get(obfClass);
                if (yarn != null && moj != null) table.addClass(yarn, moj);
            } else if (obfClass != null && (line.startsWith("\tf\t") || line.startsWith("\tm\t"))) {
                String[] p = line.split("\t", -1);
                boolean method = p[1].equals("m");
                String desc = p[2];
                String obfName = name(p, 3, official);
                String yarn = name(p, 3, named);
                if (yarn == null || obfName == null || yarn.startsWith("method_") || yarn.startsWith("field_")
                        || yarn.startsWith("comp_") || yarn.startsWith("<")) continue;
                String moj = method
                    ? mojang.methodByObf.get(obfClass + "." + obfName + desc)
                    : mojang.fieldByObf.get(obfClass + "." + obfName);
                if (moj == null) continue;
                if (yarnClass == null) { if (method) table.addMethod(yarn, moj); else table.addField(yarn, moj); }
                else if (method) table.addMethod(yarnClass, yarn, moj); else table.addField(yarnClass, yarn, moj);
            }
        }
        return table;
    }

    private static String name(String[] parts, int offset, int namespace) {
        int i = offset + namespace;
        if (i >= parts.length || parts[i].isEmpty()) return null;
        return parts[i];
    }

    /** Mojang ProGuard mapping file (client.txt / server.txt): Mojang name → obfuscated name. */
    static final class Proguard {
        private static final Pattern CLASS = Pattern.compile("^(\\S+) -> (\\S+):$");
        private static final Pattern MEMBER =
            Pattern.compile("^\\s+(?:\\d+:\\d+:)?(\\S+) ([^\\s(]+)(?:\\(([^)]*)\\))?(?::\\d+:\\d+)? -> (\\S+)$");

        private record Member(String owner, String type, String name, String args, String obf, boolean method) {}

        final Map<String, String> classByObf = new HashMap<>();     // obf internal → Mojang internal
        private final Map<String, String> obfByMojang = new HashMap<>(); // Mojang internal → obf internal
        private final List<Member> members = new ArrayList<>();
        final Map<String, String> fieldByObf = new HashMap<>();     // obfOwner.obfName → Mojang name
        final Map<String, String> methodByObf = new HashMap<>();    // obfOwner.obfName+obfDesc → Mojang name

        void read(Reader reader) throws IOException {
            BufferedReader in = new BufferedReader(reader);
            String owner = null;
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("#") || line.isBlank()) continue;
                Matcher c = CLASS.matcher(line);
                if (c.matches()) {
                    String moj = c.group(1).replace('.', '/');
                    owner = c.group(2).replace('.', '/');
                    classByObf.put(owner, moj);
                    obfByMojang.put(moj, owner);
                    continue;
                }
                Matcher m = MEMBER.matcher(line);
                if (m.matches() && owner != null) {
                    members.add(new Member(owner, m.group(1), m.group(2), m.group(3), m.group(4), m.group(3) != null));
                }
            }
        }

        void index() {
            for (Member m : members) {
                if (m.method()) {
                    StringBuilder desc = new StringBuilder("(");
                    if (!m.args().isBlank()) for (String arg : m.args().split(",")) desc.append(descriptor(arg.trim()));
                    desc.append(')').append(descriptor(m.type()));
                    methodByObf.put(m.owner() + "." + m.obf() + desc, m.name());
                } else {
                    fieldByObf.put(m.owner() + "." + m.obf(), m.name());
                }
            }
            members.clear();
        }

        private String descriptor(String type) {
            int dims = 0;
            while (type.endsWith("[]")) { dims++; type = type.substring(0, type.length() - 2); }
            String base = switch (type) {
                case "void" -> "V"; case "boolean" -> "Z"; case "byte" -> "B"; case "char" -> "C";
                case "short" -> "S"; case "int" -> "I"; case "long" -> "J"; case "float" -> "F"; case "double" -> "D";
                default -> {
                    String internal = type.replace('.', '/');
                    yield "L" + obfByMojang.getOrDefault(internal, internal) + ";";
                }
            };
            return "[".repeat(dims) + base;
        }
    }
}
