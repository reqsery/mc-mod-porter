## MC Mod Porter v1.2.0-beta

This beta introduces a desktop GUI while keeping the existing CLI available in the same cross-platform JAR.

### What's new

- Added a Swing/FlatLaf desktop GUI that opens when the JAR is launched without arguments.
- Added project-folder browsing and automatic source-version and Fabric/NeoForge loader detection.
- Added target-version selection using MC Mod Porter's existing supported-version database.
- Added **Port Mod**, **Build**, and **Run Client** actions.
- Added live logs, status, and progress while porter and Gradle tasks run.
- Added **Open Folder** and **Cancel** controls.
- Kept the existing CLI behavior when command-line arguments are supplied.
- Removed the unused `visual-tester` and `deep-debugger` experimental modules.
- Updated the helper script so it finds the current release JAR instead of relying on an old hardcoded filename.

### Download

- [`auto-porter-1.2.0-beta.jar`](https://github.com/reqsery/mc-mod-porter/releases/download/v1.2.0-beta/auto-porter-1.2.0-beta.jar) — standalone GUI + CLI JAR
- [`auto-porter-1.2.0-beta.zip`](https://github.com/reqsery/mc-mod-porter/releases/download/v1.2.0-beta/auto-porter-1.2.0-beta.zip) — packaged Windows distribution
- [`auto-porter-1.2.0-beta.tar`](https://github.com/reqsery/mc-mod-porter/releases/download/v1.2.0-beta/auto-porter-1.2.0-beta.tar) — packaged macOS/Linux distribution
- SHA256 files are included for verifying each download.

The standalone JAR is the recommended download for most users.

Open the GUI:

```bash
java -jar auto-porter-1.2.0-beta.jar
```

Use the CLI:

```bash
java -jar auto-porter-1.2.0-beta.jar --help
```

Use an extracted mod source/project folder containing its Gradle files and `src/` directory. Compiled mod JARs cannot be ported directly.

MC Mod Porter requires Java 21 or newer. Ported projects may require a newer Java version depending on their target Minecraft version.

### Beta notice

This release is still beta software. Always review the generated project, logs, and `port-report.json`; some API or mod-specific changes may still require manual work.

### Feedback

- [Report a bug](https://github.com/reqsery/mc-mod-porter/issues/new?template=bug_report.md)
- [Request a feature](https://github.com/reqsery/mc-mod-porter/issues/new?template=feature_request.md)

**Full changelog:** https://github.com/reqsery/mc-mod-porter/compare/v1.1.3-beta...v1.2.0-beta
