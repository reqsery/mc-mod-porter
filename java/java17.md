# Java 17 — Minecraft Mod Requirements

> Required from: Minecraft 1.18+
> Source: https://minecraft.wiki/w/Java_Edition_1.18

---

## Gradle Toolchain

```groovy
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.withType(JavaCompile).configureEach {
    it.options.release = 17
}
```

---

## Logging Migration (from Java/Log4j in 1.16)

### Before (1.16.x with Log4j)
```java
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

private static final Logger LOGGER = LogManager.getLogger("ModName");
```

### After (1.17+ with SLF4J)
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger LOGGER = LoggerFactory.getLogger("ModName");
```

---

## Key Java 17 Features Available

- Sealed classes (`sealed`, `permits`)
- Pattern matching for `instanceof`
- Records
- Text blocks (`"""..."""`)
- Switch expressions

---

## Compatibility Notes

- Java 17 is backwards compatible with Java 16 source
- Java 17 LTS — widely available via Eclipse Adoptium, Amazon Corretto, etc.
