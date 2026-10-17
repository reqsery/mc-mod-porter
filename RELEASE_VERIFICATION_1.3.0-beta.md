# v1.3.0-beta release verification

Verified locally on 2026-10-17. No commit, push, tag, or GitHub release was created.

## Changes made during release preparation

- `auto-porter/build.gradle`: set `1.3.0-beta`; derive JAR/ZIP/TAR filenames from `project.version`.
- `auto-porter/src/main/java/com/autoporter/BuildRunner.java`: retain package-private Java detection; recognize `options.release`, quoted compatibility values, Kotlin build files, and patch-version JDK directories; validate compiler/release metadata; stream automatic build output.
- `auto-porter/src/main/java/com/autoporter/MainWindow.java`: call the existing Unix wrapper helper in the background before GUI Gradle tasks; recognize `neo_version`; use 12px quick links with packaged 16px icons, ordered Discord, GitHub, Contribute, Report Bug, Suggest Feature; append distinct success/issue completion guidance to logs. Main controls and layout preserved.
- `auto-porter/src/main/resources/icons/`: packaged PNG icons, original SVG sources, source attribution, and Lucide license.
- `auto-porter/src/test/java/com/autoporter/MainWindowTest.java`: modern NeoForge detection regression.
- `auto-porter/src/test/java/com/autoporter/GuiReleaseTest.java`: real Swing layout/target-list checks and Port Mod/Build/Run Client button tests using a controlled Windows wrapper fixture with spaces in its path.
- `auto-porter/src/test/java/com/autoporter/ReleaseRegressionTest.java`: shared Java detection and Fabric/NeoForge port/detection checks for `1.20.4 -> 1.20.5`, `1.21.10 -> 1.21.11`, and `26.2 -> 26.3`.
- `auto-porter/src/test/java/com/autoporter/UnixWrapperSmokeTest.java`: Linux/macOS executable-bit, source preservation, Windows batch preservation, and wrapper process execution regression.
- `README.md`: current download/commands updated to `1.3.0-beta`; quick links documented. Existing v1.2.0 screenshot retained and labelled with its original version.
- `RELEASE_NOTES_1.3.0-beta.md`: release notes draft.
- `RELEASE_VERIFICATION_1.3.0-beta.md`: this report.

The previous 26.3 task's migration rules, templates, version database, metadata/resource/build patchers, migration warnings, tests, and knowledge-base documentation remain in the working tree. This release task did not redo those rules. The pre-existing deletion of `AI_GUIDE.md` remains untouched.

## Compilation and automated checks

- Clean `gradlew.bat clean test build --no-daemon` with Temurin JDK 21: passed.
- Windows: 30 tests discovered, 29 passed, 1 Unix-only test skipped, 0 failures/errors.
- Ubuntu WSL with a temporary Temurin JDK 21 on a Linux filesystem: 30 tests discovered, 28 passed, 1 Windows-only test skipped, 1 GUI test aborted by its headless-environment assumption, 0 failures.
- The Unix-only test passed on Ubuntu, so every test was exercised successfully on at least one applicable platform.
- Both Java detection methods already had package-private visibility; current clean compilation verifies `MainWindow` can call them. If Eclipse retains the old visibility errors, refresh the Gradle project and use Project > Clean.
- `git diff --check`: passed (only a line-ending normalization notice for MainWindow).

## Real example projects

Official examples were downloaded into ignored `build/release-verification/` folders. Paths contain spaces.

- Fabric `26.2 -> 26.3`: port succeeded; full Gradle `build` passed including `compileClientJava`.
- NeoForge `26.2 -> 26.3`: port succeeded; full Gradle `build` passed.
- Fabric automatic CLI `26.2 -> 26.3` with build enabled: passed with the porter running on Java 21 and detecting the installed JDK 25 automatically.
- Fabric and NeoForge 26.3 `build runClient --dry-run`: passed. This verifies task resolution, not a running Minecraft client.
- Older official Fabric `1.21.10 -> 1.21.11`: port succeeded; full Gradle `build` passed, including client compilation and JAR/source remapping (7m09s on the first build).
- Older official NeoForge `1.21.10 -> 1.21.11`: port succeeded; full Gradle `build` passed (6m05s on the first build).

Sources: https://github.com/FabricMC/fabric-example-mod and https://github.com/NeoForgeMDKs

## Standalone packaging

- No-argument launch of the final JAR opened the `MC Mod Porter` window; test window was closed afterward.
- `--help` runs in CLI mode; `--list-versions` includes 26.3 for Fabric/NeoForge.
- Manifest main class: `com.autoporter.AutoPorterMain`; implementation version: `1.3.0-beta`.
- GUI, FlatLaf, Gson, JavaParser, and SLF4J classes verified inside the JAR.
- JAR: `auto-porter/build/libs/auto-porter-1.3.0-beta.jar` (2,871,983 bytes, including quick-link icons).
- Optional ZIP/TAR: `auto-porter/build/distributions/auto-porter-1.3.0-beta.zip` and `.tar`.
- SHA256 sidecar files prepared next to all three artifacts. Rebuilds require regenerating checksums.

## Limits

- The Linux permission test uses a controlled executable shell wrapper to verify process launch for `compileJava`, `build`, and `runClient`. Real Minecraft Gradle builds were performed on Windows.
- Native macOS execution was not available; the same POSIX helper is used there.
- Minecraft client gameplay was not launched; only task resolution and GUI command dispatch were checked.
- All five links compile, have listeners, fit below the logs without overlapping other controls, and use `Desktop.browse` for the default browser. Browser clicks and destination availability were not tested interactively.
- The existing screenshot is from v1.2.0-beta; no replacement screenshot was invented.
- Semantic API changes, mixins, complex multi-loader builds, and third-party mod dependencies can still need manual work.
- Existing Gradle deprecation and TemplateManager unchecked-operation notices remain.
