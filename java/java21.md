# Java 21 — Minecraft Mod Requirements

> Required from: Minecraft 1.20.5+
> Source: https://minecraft.wiki/w/Java_Edition_1.20.5

---

## Gradle Toolchain

```groovy
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType(JavaCompile).configureEach {
    it.options.release = 21
}
```

---

## Key Java 21 Features Available

- Virtual threads (Project Loom)
- Pattern matching for switch (finalized)
- Record patterns
- Sequenced collections
- String Templates (preview in 21)

---

## Compatibility Notes

- Java 21 is LTS — available via Eclipse Adoptium, Amazon Corretto, GraalVM
- Default JDK path (Eclipse Adoptium on Windows): `C:\Users\<user>\AppData\Local\Programs\Eclipse Adoptium\jdk-21.x.x-hotspot`
