## MC Mod Porter v1.3.0-beta

- Added Minecraft 26.3 version data, templates, and migration rules for Fabric and NeoForge, with warnings for changes that need manual work.
- Fixed Linux/macOS Gradle wrapper permissions after porting and before GUI Build/Run Client tasks.
- Added 12px GUI quick links with icons, ordered Discord, GitHub, Contribute, Report Bug, and Suggest Feature.
- Added helpful completion messages pointing to bug reports and Discord support.
- Improved shared GUI/CLI Java detection for Java 25 builds and JDK installation folders containing patch versions.
- Fixed detection of NeoForge's `neo_version` property and streamed automatic build output to the GUI logs.
- Updated NeoForge metadata handling and Gradle wrapper versions for 26.3 projects.

### Download and run

Download `auto-porter-1.3.0-beta.jar` from the assets below. The same standalone JAR supports both modes:

```bash
# Open the desktop GUI
java -jar auto-porter-1.3.0-beta.jar

# Use the CLI
java -jar auto-porter-1.3.0-beta.jar --help
```

Requires Java 21+ to run the porter; Minecraft 26.x projects require JDK 25 to build. Select an extracted mod source/project folder with its Gradle files and `src/`, rather than a compiled mod JAR.

This is still **Beta**. Review the output, warnings, and `build/port-report.json`. Semantic API changes, mixins, and third-party dependencies can require manual fixes; a completed port is not a guarantee that every mod compiles or runs.

[Report a bug](https://github.com/reqsery/mc-mod-porter/issues/new?template=bug_report.md) · [Request a feature](https://github.com/reqsery/mc-mod-porter/issues/new?template=feature_request.md)

**Full changelog:** https://github.com/reqsery/mc-mod-porter/compare/v1.2.0-beta...v1.3.0-beta
