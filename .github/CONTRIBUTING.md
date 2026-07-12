---
name: How to Contribute
---

# Contributing to mc-mod-porter

Thanks for your interest in contributing to mc-mod-porter.

Contributions are welcome in many forms, including code, testing, documentation, migration data, bug reports, and feature suggestions.

## Getting Started

Before contributing:

1. Read the [README](../README.md) to understand what the project does.
2. Check the existing [issues](../../issues) and [discussions](../../discussions) to avoid duplicating work.
3. Try the tool on a real mod so you understand the current workflow.
4. Look for issues labeled `good first issue` or `help wanted`.

## Reporting a Bug

Open a bug report and include:

* Minecraft source and target versions
* Mod loader and loader version
* Java version
* Operating system
* Steps to reproduce the problem
* Expected behavior
* Actual behavior
* Relevant logs, stack traces, or error messages

The more detail you provide, the easier the issue will be to investigate.

## Suggesting a Feature

Open a feature request and explain:

* The problem you are trying to solve
* Your proposed solution
* Why the change would be useful
* Any alternatives or workarounds you have considered

Feature requests should focus on the problem first. A proposed implementation is helpful, but not required.

## Development Setup

### Requirements

You will need:

* Git
* Java 21 or newer
* The included Gradle wrapper

A separate Gradle installation is not required.

### Clone and Build

```bash
git clone https://github.com/reqsery/mc-mod-porter.git
cd mc-mod-porter
```

List the available Gradle tasks:

```bash
./gradlew tasks
```

Build the CLI tool:

```bash
cd auto-porter
./gradlew build
```

The generated JAR will be located at:

```
auto-porter/build/libs/auto-porter-1.0.0.jar
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Testing the Build

List the supported migration versions:

```bash
java -jar auto-porter/build/libs/auto-porter-1.0.0.jar --list-versions
```

Run a dry run without modifying files:

```bash
java -jar auto-porter/build/libs/auto-porter-1.0.0.jar /path/to/test/mod 1.20.4 1.20.5 --dry-run
```

Whenever possible, test changes against a real mod rather than relying only on synthetic examples.

## Project Structure

```
auto-porter/
  src/main/java/      Core CLI and migration logic
  src/test/java/      Automated tests
  build.gradle        Build configuration

visual-tester/        Optional Fabric mod used for testing
deep-debugger/        Optional static-analysis tooling

knowledge-base/
  minecraft/          Minecraft version migration data
  loaders/            Fabric and NeoForge version data
```

Additional contribution rules for the knowledge base are documented in:

```
docs/CONTRIBUTING.md
```

## Making Code Changes

Create a branch from `main`:

```bash
git checkout main
git pull
git checkout -b feature/my-feature
```

For bug fixes:

```bash
git checkout -b fix/bug-name
```

Keep each branch focused on one feature, fix, or documentation change. Use clear commit messages and avoid including unrelated cleanup in the same pull request.

### Run Automated Tests

```bash
cd auto-porter
./gradlew test
```

### Build and Test Manually

```bash
./gradlew build
java -jar build/libs/auto-porter-1.0.0.jar /path/to/test/mod 1.20.4 1.20.5
```

You should also inspect the generated output and verify that the resulting project still builds whenever possible.

### Push Your Branch

```bash
git push origin feature/my-feature
```

Then open a pull request at:

```
https://github.com/reqsery/mc-mod-porter/pulls
```

## Adding Knowledge-Base Data

Knowledge-base contributions must be based on verified migration information.

To add support data for a new Minecraft migration:

1. Copy `templates/version-template.md`
2. Save it using the migration path: `knowledge-base/minecraft/X.XX_to_Y.YY.md`
3. Add only information you have verified.
4. Include a source URL for every migration entry.
5. Follow the complete format described in `docs/CONTRIBUTING.md`

Acceptable sources include:

* Official Mojang documentation or source mappings
* Fabric documentation and changelogs
* NeoForge documentation and changelogs
* Official loader or API repositories
* Verified upstream commits and release notes

Do not add guessed, assumed, or AI-generated migration information without verification. Every knowledge-base entry must be supported by a source.

## Pull Request Guidelines

A pull request should include:

* A clear and specific title
* A description of what changed
* An explanation of why the change is needed
* Testing performed
* Relevant logs or dry-run output
* Links to related issues

Use `Fixes #123` when the pull request closes an issue.

### Example

```markdown
## What changed?

Added automatic handling for the GuiGraphics API changes introduced in the 1.19.4 to 1.20 migration path.

## Why?

Calls using the previous rendering signatures were not being migrated, causing affected projects to fail during compilation.

## Testing

- Ported a Fabric test mod from 1.19.4 to 1.20
- Ran the migration in dry-run mode on five projects
- Built the generated project successfully
- Manually inspected the modified rendering calls

Fixes #42
```

Avoid vague descriptions such as "fixed issue" or "updated code." Reviewers should be able to understand the purpose and impact of the change without reading the entire diff first.

## Other Ways to Contribute

You do not need to write code to help the project.

Useful contributions include:

* Testing the tool on real mods
* Reporting migration failures
* Improving documentation
* Correcting unclear instructions or examples
* Adding verified migration data
* Creating minimal reproduction projects
* Reviewing pull requests
* Participating in discussions
* Sharing the project with mod developers

## Questions and Support

For development questions or contribution discussions:

* Discord: https://discord.gg/vV2USr9phF
* Discussions: https://github.com/reqsery/mc-mod-porter/discussions
* FAQ: docs/FAQ.md
* Troubleshooting: docs/TROUBLESHOOTING.md
* Knowledge-base rules: docs/CONTRIBUTING.md
* AI assistant guidance: AI_GUIDE.md

Thank you for helping improve mc-mod-porter.