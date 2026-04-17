# AI Guide — How to port your mod using AI

This guide is for using an AI tool to port your mod with mc-mod-porter.
The AI runs the porter for you, reads the output, and fixes what the tool couldn't handle automatically.

---

## What AI to use

You need an AI that runs **on your PC** and can execute terminal commands. Web-based chat (pasting into a browser) won't work here.

| Tool | Notes |
|------|-------|
| [Claude Code](https://claude.ai/claude-code) | Best for this. Runs in your terminal, reads files, executes commands. **Max plan** recommended for large mods. |
| [Cursor](https://cursor.sh) | AI-powered VS Code fork. Open the mc-mod-porter folder, use the built-in chat. **Pro plan** recommended. |
| [Windsurf](https://codeium.com/windsurf) | Similar to Cursor. Free tier works for smaller mods. |
| [GitHub Copilot](https://github.com/features/copilot) | Works inside VS Code / JetBrains. Better for fixing individual files after the tool runs. |

---

## How to use it

### Step 1 — Open the AI in the mc-mod-porter folder

Point your AI tool at the `mc-mod-porter` folder on your PC. For example in Claude Code:

```bash
cd C:/Users/yourname/Desktop/mc-mod-porter
claude
```

In Cursor or Windsurf: open the `mc-mod-porter` folder as your project.

### Step 2 — Tell the AI what you want

Give it your mod path and the versions you want to port between. No need to paste any files — just give it the path.

**Example prompt:**

```
I have a Fabric mod at C:/Users/yourname/Desktop/mymod.
I want to port it from 1.20.4 to 1.20.5.

The auto-porter JAR is at auto-porter/build/libs/auto-porter-1.0.0.jar.
If it hasn't been built yet, build it first with: cd auto-porter && ./gradlew build

Then:
1. Run a dry run to show what would change
2. Run the actual port
3. Read the build output
4. Fix any remaining errors using the knowledge-base files in knowledge-base/minecraft/
```

The AI will:
1. Build the tool if needed
2. Run the dry run and show you the planned changes
3. Port the mod (creates a copy — your original is never touched)
4. Read the build errors
5. Look up the relevant knowledge-base file and fix what the tool couldn't

### Step 3 — Review the result

The ported mod will be at `C:/Users/yourname/Desktop/mymod-ported-1_20_5`.
Open it, check the changes, and test it in-game.

---

## Porting across multiple versions

If you're jumping more than one version (e.g. 1.19.4 → 1.21.4), tell the AI to port one step at a time:

```
I have a Fabric mod at C:/Users/yourname/Desktop/mymod.
I want to port it from 1.19.4 to 1.21.4 step by step.

Port it one version at a time in this order:
1.19.4 → 1.20
1.20 → 1.20.1
1.20.1 → 1.20.2
... and so on

After each step, fix any build errors using the matching knowledge-base file before moving to the next step.
The auto-porter JAR is at auto-porter/build/libs/auto-porter-1.0.0.jar.
```

---

## If something goes wrong

Tell the AI to look at the knowledge-base file for that version hop:

```
The build failed after porting from 1.20.4 to 1.20.5.
Read knowledge-base/minecraft/1.20.4_to_1.20.5.md and fix the remaining errors in the ported mod.
Do not guess — only apply changes that are documented in that file.
```

---

## Rules for AI working on this repo

> **DO NOT GUESS.** Every version number, class name, and API change must come from the knowledge-base or a verified source. If something is not documented, say so — do not invent it.
>
> **The knowledge-base is the source of truth.** If any rule in `auto-porter` conflicts with a knowledge-base file, the rule must be fixed, not the knowledge-base.
