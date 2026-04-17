package com.debugger;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.Modifier;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Scans a mod project's source directories and extracts all public methods
 * from non-abstract, non-interface classes.
 */
public class SourceScanner {

    public List<MethodInfo> scan(String modPath) throws IOException {
        List<MethodInfo> methods = new ArrayList<>();
        List<Path> javaSources = findJavaSources(modPath);
        for (Path src : javaSources) {
            try {
                methods.addAll(parseFile(src));
            } catch (Exception e) {
                System.err.println("Skipping " + src.getFileName() + ": " + e.getMessage());
            }
        }
        return methods;
    }

    private List<Path> findJavaSources(String modPath) throws IOException {
        List<Path> result = new ArrayList<>();
        Path root = Path.of(modPath);
        if (!Files.exists(root)) return result;

        List<Path> searchRoots = List.of(
            root.resolve("src/main/java"),
            root.resolve("common/src/main/java"),
            root.resolve("fabric/src/main/java"),
            root.resolve("neoforge/src/main/java")
        );
        for (Path sr : searchRoots) {
            if (Files.exists(sr)) {
                Files.walk(sr).filter(p -> p.toString().endsWith(".java")).forEach(result::add);
            }
        }
        // Fallback: walk entire project
        if (result.isEmpty() && Files.exists(root)) {
            Files.walk(root)
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> !p.toString().contains("build") && !p.toString().contains(".gradle"))
                .forEach(result::add);
        }
        return result;
    }

    private List<MethodInfo> parseFile(Path src) throws Exception {
        List<MethodInfo> methods = new ArrayList<>();
        CompilationUnit cu = StaticJavaParser.parse(src);
        String pkg = cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("");

        for (TypeDeclaration<?> type : cu.getTypes()) {
            // Use ClassOrInterfaceDeclaration (javaparser's actual class name)
            if (!(type instanceof ClassOrInterfaceDeclaration cls)) continue;
            if (cls.isInterface() || cls.isAbstract()) continue;
            if (!cls.isPublic()) continue;

            String className = pkg.isEmpty() ? cls.getNameAsString() : pkg + "." + cls.getNameAsString();

            for (MethodDeclaration method : cls.getMethods()) {
                if (!method.isPublic()) continue;
                if (method.getAnnotationByName("Override").isPresent()) continue;

                List<String> paramTypes = method.getParameters()
                    .stream().map(p -> p.getType().asString()).toList();

                String returnType = method.getType().asString();
                boolean isStatic = method.hasModifier(Modifier.Keyword.STATIC);
                boolean isVoid   = returnType.equals("void");

                methods.add(new MethodInfo(className, method.getNameAsString(),
                    returnType, paramTypes, isStatic, isVoid));
            }
        }
        return methods;
    }
}
