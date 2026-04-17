# Java 25 — Minecraft Mod Requirements

> Required from: Minecraft 26.1+
> Source: https://minecraft.wiki/w/Java_Edition_26.1

---

## Gradle Toolchain

```groovy
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.withType(JavaCompile).configureEach {
    it.options.release = 25
}
```

---

## Why Java 25?

Minecraft 26.1 (released March 24, 2026) is the first version to require Java 25.
This coincides with the switch to calendar-based versioning and fully unobfuscated code.

---

## Availability

- Java 25 is available from Eclipse Adoptium (Temurin), Oracle, Amazon Corretto
- **LTS** — Java 25 is a Long-Term Support release (LTS cadence: Java 17, 21, 25)
- Gradle 9.4.0+ required for Java 25 support
- Source: https://fabricmc.net/2026/03/14/261.html

---

## Compatibility Notes

- Mods compiled for Java 21 will NOT run on Java 25 JVM without recompilation (due to class file version changes)
- All 1.21.x mods must be recompiled for 26.x regardless
